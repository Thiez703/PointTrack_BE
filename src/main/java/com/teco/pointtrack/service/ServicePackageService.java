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
    private final UserRepository           userRepository;
    private final CustomerRepository       customerRepository;
    private final ConflictCheckerService   conflictChecker;
    private final ObjectMapper             objectMapper;

    @Transactional
    public PackageResponse create(PackageRequest req) {
        User employee   = findEmployee(req.getEmployeeId());
        Customer customer = findCustomer(req.getCustomerId());

        PackageRequest.RecurrencePatternDto pattern = req.getRecurrencePattern();
        LocalTime startTime = LocalTime.parse(pattern.getTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endTime   = LocalTime.parse(req.getEndTime(), DateTimeFormatter.ofPattern("HH:mm"));
        ShiftType shiftType = req.getShiftType();

        int durationMinutes = calcDuration(startTime, endTime);

        List<LocalDate> dates = generateDates(req.getTotalSessions(), pattern.getDays(), LocalDate.now().plusDays(1));
        String patternJson = serializePattern(pattern);

        ServicePackage pkg = ServicePackage.builder()
                .customer(customer)
                .employee(employee)
                .totalSessions(req.getTotalSessions())
                .completedSessions(0)
                .recurrencePattern(patternJson)
                .notes(req.getNotes())
                .status(PackageStatus.ACTIVE)
                .build();
        ServicePackage savedPkg = packageRepository.save(pkg);

        List<PackagePreviewResponse> previewShifts = new ArrayList<>();
        List<String> conflictDates = new ArrayList<>();

        for (LocalDate date : dates) {
            ConflictCheckResponse conflict = conflictChecker.check(
                    employee.getId(), date, startTime, endTime, shiftType, null, customer);

            if (conflict.isHasConflict() && "OVERLAP".equals(conflict.getConflictType())) {
                throw new ConflictException("Nhân viên bị trùng ca vào ngày " + date + ". " + conflict.getDetail());
            }

            ShiftStatus shiftStatus = ShiftStatus.ASSIGNED;
            if (conflict.isHasConflict()) {
                conflictDates.add(date.toString());
            }

            Shift shift = Shift.builder()
                    .employee(employee)
                    .customer(customer)
                    .servicePackage(savedPkg)
                    .shiftDate(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMinutes(durationMinutes)
                    .shiftType(shiftType)
                    .otMultiplier(req.getOtMultiplier())
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

        return toResponse(savedPkg, previewShifts, conflictDates);
    }

    @Transactional(readOnly = true)
    public List<PackageResponse> getAll() {
        return packageRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(p -> toResponse(p, null, null))
                .toList();
    }

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

    @Transactional
    public PackageResponse reassignEmployee(Long id, Long newEmployeeId) {
        ServicePackage pkg = findActivePackage(id);
        if (pkg.getStatus() != PackageStatus.ACTIVE) throw new BadRequestException("Gói không ACTIVE");
        User newEmployee = findEmployee(newEmployeeId);

        List<Shift> scheduledShifts = shiftRepository.findScheduledByPackageId(id);
        for (Shift shift : scheduledShifts) {
            ConflictCheckResponse conflict = conflictChecker.check(
                    newEmployeeId, shift.getShiftDate(),
                    shift.getStartTime(), shift.getEndTime(),
                    shift.getShiftType(), null, pkg.getCustomer());
            if (conflict.isHasConflict() && "OVERLAP".equals(conflict.getConflictType())) {
                throw new ConflictException("Nhân viên bận vào ngày " + shift.getShiftDate());
            }
        }

        for (Shift shift : scheduledShifts) {
            shift.setEmployee(newEmployee);
            shiftRepository.save(shift);
        }
        pkg.setEmployee(newEmployee);
        packageRepository.save(pkg);
        return toResponse(pkg, null, null);
    }

    @Transactional
    public void cancel(Long id) {
        ServicePackage pkg = findActivePackage(id);
        List<Shift> scheduledShifts = shiftRepository.findScheduledByPackageId(id);
        for (Shift shift : scheduledShifts) {
            shift.setStatus(ShiftStatus.CANCELLED);
            shiftRepository.save(shift);
        }
        pkg.setStatus(PackageStatus.CANCELLED);
        packageRepository.save(pkg);
    }

    private ServicePackage findActivePackage(Long id) {
        return packageRepository.findByIdAndStatusNot(id, PackageStatus.CANCELLED)
                .orElseThrow(() -> new NotFoundException("Gói không tồn tại"));
    }

    private User findEmployee(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("NV không tồn tại"));
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("KH không tồn tại"));
    }

    private List<LocalDate> generateDates(int totalSessions, List<Integer> days, LocalDate startFrom) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate current = startFrom;
        while (result.size() < totalSessions) {
            if (days.contains(current.getDayOfWeek().getValue())) result.add(current);
            current = current.plusDays(1);
        }
        return result;
    }

    private int calcDuration(LocalTime start, LocalTime end) {
        if (end.isBefore(start)) {
            return (24 * 60) - (start.getHour() * 60 + start.getMinute()) + (end.getHour() * 60 + end.getMinute());
        }
        return (end.getHour() * 60 + end.getMinute()) - (start.getHour() * 60 + start.getMinute());
    }

    private String serializePattern(PackageRequest.RecurrencePatternDto pattern) {
        try {
            return objectMapper.writeValueAsString(Map.of("days", pattern.getDays(), "time", pattern.getTime()));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Pattern lỗi");
        }
    }

    private PackageResponse toResponse(ServicePackage p, List<PackagePreviewResponse> preview, List<String> conflictDates) {
        return PackageResponse.builder()
                .id(p.getId())
                .customerId(p.getCustomer().getId())
                .customerName(p.getCustomer().getName())
                .employeeId(p.getEmployee().getId())
                .employeeName(p.getEmployee().getFullName())
                .totalSessions(p.getTotalSessions())
                .completedSessions(p.getCompletedSessions())
                .remainingSessions(p.getTotalSessions() - p.getCompletedSessions())
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
