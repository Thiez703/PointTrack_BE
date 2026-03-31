package com.teco.pointtrack.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teco.pointtrack.dto.packages.*;
import com.teco.pointtrack.dto.shift.ConflictCheckResponse;
import com.teco.pointtrack.entity.*;
import com.teco.pointtrack.entity.enums.PackageStatus;
import com.teco.pointtrack.entity.enums.ShiftStatus;
import com.teco.pointtrack.entity.enums.ShiftType;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServicePackageService {

    private final ServicePackageRepository packageRepository;
    private final ShiftRepository          shiftRepository;
    private final ShiftTemplateRepository  templateRepository;
    private final UserRepository           userRepository;
    private final CustomerRepository       customerRepository;
    private final ConflictCheckerService   conflictChecker;
    private final ObjectMapper             objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/packages — tạo gói định kỳ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * BR: @Transactional — nếu bất kỳ ca nào lỗi nghiêm trọng, rollback toàn bộ gói.
     * Soft approach: OVERLAP → block (hard). BUFFER → ghi conflictDates, tiếp tục.
     */
    @Transactional
    public PackageResponse create(PackageRequest req) {
        User employee   = findEmployee(req.getEmployeeId());
        Customer customer = findCustomer(req.getCustomerId());
        ShiftTemplate template = findTemplate(req.getTemplateId());

        PackageRequest.RecurrencePatternDto pattern = req.getRecurrencePattern();
        LocalTime startTime = LocalTime.parse(pattern.getTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endTime   = template.getDefaultEnd();
        ShiftType shiftType = template.getShiftType();
        int durationMinutes = template.getDurationMinutes();

        // Sinh danh sách ngày theo recurrencePattern
        List<LocalDate> dates = generateDates(req.getTotalSessions(), pattern.getDays(), LocalDate.now().plusDays(1));

        // Serialize recurrencePattern sang JSON
        String patternJson = serializePattern(pattern);

        // Tạo gói
        ServicePackage pkg = ServicePackage.builder()
                .customer(customer)
                .employee(employee)
                .template(template)
                .totalSessions(req.getTotalSessions())
                .completedSessions(0)
                .recurrencePattern(patternJson)
                .notes(req.getNotes())
                .status(PackageStatus.ACTIVE)
                .build();
        ServicePackage savedPkg = packageRepository.save(pkg);

        // Tạo các ca theo lịch, xử lý conflict
        List<PackagePreviewResponse> previewShifts = new ArrayList<>();
        List<String> conflictDates = new ArrayList<>();

        for (LocalDate date : dates) {
            ConflictCheckResponse conflict = conflictChecker.check(
                    employee.getId(), date, startTime, endTime, shiftType, null, customer);

            if (conflict.isHasConflict() && "OVERLAP".equals(conflict.getConflictType())) {
                // Hard block: OVERLAP → toàn bộ gói rollback
                throw new ConflictException(
                        "PACKAGE_EMPLOYEE_BUSY: Nhân viên bị trùng ca vào ngày " + date
                        + ". " + conflict.getDetail());
            }

            ShiftStatus shiftStatus = ShiftStatus.ASSIGNED;
            if (conflict.isHasConflict()) {
                // BUFFER: ghi nhận ngày conflict, vẫn tạo ca (soft)
                conflictDates.add(date.toString());
                log.warn("[PACKAGE id={}] Buffer violation ngày {}: {}", savedPkg.getId(), date, conflict.getDetail());
            }

            Shift shift = Shift.builder()
                    .employee(employee)
                    .customer(customer)
                    .template(template)
                    .servicePackage(savedPkg)
                    .shiftDate(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMinutes(durationMinutes)
                    .shiftType(shiftType)
                    .otMultiplier(template.getOtMultiplier())
                    .notes(req.getNotes())
                    .status(shiftStatus)
                    .build();
            shiftRepository.save(shift);

            previewShifts.add(PackagePreviewResponse.builder()
                    .date(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .status(shiftStatus)
                    .build());
        }

        log.info("Created package id={} employee={} sessions={} conflictDates={}",
                savedPkg.getId(), employee.getFullName(), dates.size(), conflictDates.size());

        return toResponse(savedPkg, previewShifts, conflictDates);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/packages
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PackageResponse> getAll() {
        return packageRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(p -> toResponse(p, null, null))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/packages/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PackageResponse getById(Long id) {
        ServicePackage pkg = findActivePackage(id);

        List<Shift> shifts = shiftRepository.findAllByServicePackageIdOrderByShiftDateAsc(id);
        List<PackagePreviewResponse> preview = shifts.stream()
                .map(s -> PackagePreviewResponse.builder()
                        .date(s.getShiftDate())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .status(s.getStatus())
                        .build())
                .toList();

        return toResponse(pkg, preview, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/packages/{id}/employee — reassign nhân viên
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public PackageResponse reassignEmployee(Long id, Long newEmployeeId) {
        ServicePackage pkg = findActivePackage(id);

        if (pkg.getStatus() != PackageStatus.ACTIVE) {
            throw new BadRequestException("Chỉ có thể reassign gói đang ACTIVE");
        }

        User newEmployee = findEmployee(newEmployeeId);

        // Kiểm tra conflict cho tất cả ca SCHEDULED của gói
        List<Shift> scheduledShifts = shiftRepository.findScheduledByPackageId(id);
        for (Shift shift : scheduledShifts) {
            ConflictCheckResponse conflict = conflictChecker.check(
                    newEmployeeId, shift.getShiftDate(),
                    shift.getStartTime(), shift.getEndTime(),
                    shift.getShiftType(), null, pkg.getCustomer());

            if (conflict.isHasConflict() && "OVERLAP".equals(conflict.getConflictType())) {
                throw new ConflictException(
                        "PACKAGE_EMPLOYEE_BUSY: Nhân viên mới bận vào ngày "
                        + shift.getShiftDate() + ". " + conflict.getDetail());
            }
        }

        // Cập nhật nhân viên cho các ca SCHEDULED
        for (Shift shift : scheduledShifts) {
            shift.setEmployee(newEmployee);
            shiftRepository.save(shift);
        }

        pkg.setEmployee(newEmployee);
        packageRepository.save(pkg);

        log.info("Reassigned package id={} to employee={}", id, newEmployee.getFullName());
        return toResponse(pkg, null, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/packages/{id}/cancel — huỷ các ca SCHEDULED
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void cancel(Long id) {
        ServicePackage pkg = findActivePackage(id);

        if (pkg.getStatus() == PackageStatus.CANCELLED) {
            throw new BadRequestException("Gói này đã được huỷ trước đó");
        }

        List<Shift> scheduledShifts = shiftRepository.findScheduledByPackageId(id);
        for (Shift shift : scheduledShifts) {
            shift.setStatus(ShiftStatus.CANCELLED);
            shiftRepository.save(shift);
        }

        pkg.setStatus(PackageStatus.CANCELLED);
        packageRepository.save(pkg);

        log.info("Cancelled package id={}, huỷ {} ca SCHEDULED", id, scheduledShifts.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ServicePackage findActivePackage(Long id) {
        return packageRepository.findByIdAndStatusNot(id, PackageStatus.CANCELLED)
                .orElseThrow(() -> new NotFoundException("Gói dịch vụ không tồn tại (id={})", id));
    }

    private User findEmployee(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Nhân viên không tồn tại (id={})", id));
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Khách hàng không tồn tại (id={})", id));
    }

    private ShiftTemplate findTemplate(Long id) {
        return templateRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Ca mẫu không tồn tại (id={})", id));
    }

    /**
     * Sinh danh sách ngày theo pattern.
     * days: 1=Thứ Hai … 7=Chủ Nhật (ISO DayOfWeek).
     * Bắt đầu từ startFrom, sinh đúng totalSessions ngày.
     */
    private List<LocalDate> generateDates(int totalSessions, List<Integer> days, LocalDate startFrom) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate current = startFrom;
        while (result.size() < totalSessions) {
            int dow = current.getDayOfWeek().getValue(); // 1=Mon … 7=Sun
            if (days.contains(dow)) {
                result.add(current);
            }
            current = current.plusDays(1);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private String serializePattern(PackageRequest.RecurrencePatternDto pattern) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of("days", pattern.getDays(), "time", pattern.getTime()));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("recurrencePattern không hợp lệ");
        }
    }

    private PackageResponse toResponse(ServicePackage p,
                                       List<PackagePreviewResponse> preview,
                                       List<String> conflictDates) {
        int remaining = p.getTotalSessions() - p.getCompletedSessions();
        return PackageResponse.builder()
                .id(p.getId())
                .customerId(p.getCustomer().getId())
                .customerName(p.getCustomer().getName())
                .employeeId(p.getEmployee().getId())
                .employeeName(p.getEmployee().getFullName())
                .templateId(p.getTemplate() != null ? p.getTemplate().getId() : null)
                .templateName(p.getTemplate() != null ? p.getTemplate().getName() : null)
                .totalSessions(p.getTotalSessions())
                .completedSessions(p.getCompletedSessions())
                .remainingSessions(remaining)
                .recurrencePattern(p.getRecurrencePattern())
                .status(p.getStatus())
                .notes(p.getNotes())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .previewShifts(preview)
                .conflictDates(conflictDates)
                .build();
    }
}
