package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.shift.*;
import com.teco.pointtrack.entity.*;
import com.teco.pointtrack.entity.enums.CustomerStatus;
import com.teco.pointtrack.entity.enums.ShiftStatus;
import com.teco.pointtrack.entity.enums.ShiftType;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.exception.ShiftAssignException;
import org.springframework.http.HttpStatus;
import com.teco.pointtrack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftService {

    private final ShiftRepository         shiftRepository;
    private final UserRepository          userRepository;
    private final CustomerRepository      customerRepository;
    private final ConflictCheckerService  conflictChecker;

    @Value("${app.shift.geofence-radius-meters:100}")
    private double geofenceRadiusMeters;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/shifts — lịch theo tuần / tháng / nhân viên
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, List<ShiftResponse>> getShifts(String week, Integer month, Integer year, Long employeeId) {
        return (Map<String, List<ShiftResponse>>) getShiftsV2(week, month, year, null, null, employeeId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/shifts/my-today — ca hôm nay của nhân viên hiện tại
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ShiftResponse> getMyTodayShifts(Long employeeId) {
        LocalDate today = LocalDate.now();
        return shiftRepository.findRelevantShifts(employeeId, today.minusDays(1), today.plusDays(1))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * V2 hỗ trợ startDate, endDate và format {content: []}
     */
    @Transactional(readOnly = true)
    public Object getShiftsV2(String week, Integer month, Integer year, LocalDate startDate, LocalDate endDate, Long employeeId) {
        LocalDate from;
        LocalDate to;

        if (startDate != null && endDate != null) {
            from = startDate;
            to   = endDate;
        } else if (week != null && !week.isBlank()) {
            int[] yw = parseIsoWeek(week);
            from = isoWeekStart(yw[0], yw[1]);
            to   = from.plusDays(6);
        } else if (month != null && year != null) {
            from = LocalDate.of(year, month, 1);
            to   = from.with(TemporalAdjusters.lastDayOfMonth());
        } else {
            from = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            to   = from.plusDays(6);
        }

        List<Shift> shifts = shiftRepository.findByDateRangeAndEmployee(from, to, employeeId);
        List<ShiftResponse> responseList = shifts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        if (employeeId != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("content", responseList);
            return result;
        }

        return responseList.stream()
                .collect(Collectors.groupingBy(s -> String.valueOf(s.getEmployeeId())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/shifts — tạo một ca đơn lẻ
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ShiftResponse create(ShiftRequest req) {
        Customer customer = findCustomer(req.getCustomerId());

        validateOvernightRule(req.getShiftType(), req.getStartTime(), req.getEndTime());
        int duration = calcDuration(req.getShiftType(), req.getStartTime(), req.getEndTime());

        if (req.getEmployeeId() == null) {
            Shift openSlot = Shift.builder()
                    .customer(customer)
                    .shiftDate(req.getShiftDate())
                    .startTime(req.getStartTime())
                    .endTime(req.getEndTime())
                    .durationMinutes(duration)
                    .shiftType(req.getShiftType())
                    .otMultiplier(defaultOtMultiplier(req.getShiftType()))
                    .notes(req.getNotes())
                    .status(ShiftStatus.PUBLISHED)
                    .build();
            Shift saved = shiftRepository.save(openSlot);
            log.info("Created open slot id={} date={}", saved.getId(), req.getShiftDate());
            return toResponse(saved);
        }

        User employee = findEmployee(req.getEmployeeId());

        ConflictCheckResponse conflict = conflictChecker.check(
                req.getEmployeeId(), req.getShiftDate(),
                req.getStartTime(), req.getEndTime(),
                req.getShiftType(), null, customer);

        handleConflict(conflict, req.getShiftType());

        Shift shift = Shift.builder()
                .employee(employee)
                .customer(customer)
                .shiftDate(req.getShiftDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .durationMinutes(duration)
                .shiftType(req.getShiftType())
                .otMultiplier(defaultOtMultiplier(req.getShiftType()))
                .notes(req.getNotes())
                .status(ShiftStatus.ASSIGNED)
                .build();

        Shift saved = shiftRepository.save(shift);
        logNotification(saved);

        log.info("Created shift id={} employee={} date={} type={}", saved.getId(),
                employee.getFullName(), req.getShiftDate(), req.getShiftType());
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/shifts/assign — Gán ca trực (Drag & Drop)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AssignShiftResult assign(AssignShiftRequest req) {
        LocalTime startTime = req.getStartTime();
        LocalTime endTime   = req.getEndTime();

        if (startTime == null || endTime == null) {
            throw new ShiftAssignException("INVALID_TIME_RANGE",
                    "Thời gian bắt đầu và kết thúc không được để trống",
                    HttpStatus.BAD_REQUEST);
        }

        if (endTime.equals(startTime)) {
            throw new ShiftAssignException("INVALID_TIME_RANGE",
                    "Giờ bắt đầu và kết thúc không được giống nhau",
                    HttpStatus.BAD_REQUEST);
        }

        User employee = userRepository.findByIdAndDeletedAtIsNull(req.getEmployeeId())
                .orElseThrow(() -> new ShiftAssignException("EMPLOYEE_NOT_FOUND",
                        "Không tìm thấy nhân viên (id=" + req.getEmployeeId() + ")",
                        HttpStatus.NOT_FOUND));

        if (employee.getStatus() == UserStatus.INACTIVE || employee.getStatus() == UserStatus.ON_LEAVE) {
            throw new ShiftAssignException("INACTIVE_STATUS",
                    "Nhân viên đang bị khóa hoặc đang trong trạng thái nghỉ phép",
                    HttpStatus.FORBIDDEN);
        }

        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(req.getCustomerId())
                .orElseThrow(() -> new ShiftAssignException("CUSTOMER_NOT_FOUND",
                        "Không tìm thấy khách hàng (id=" + req.getCustomerId() + ")",
                        HttpStatus.NOT_FOUND));

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new ShiftAssignException("INACTIVE_STATUS",
                    "Khách hàng không ở trạng thái ACTIVE, không thể gán ca mới",
                    HttpStatus.FORBIDDEN);
        }

        ConflictCheckResponse conflict = conflictChecker.check(
                req.getEmployeeId(), req.getShiftDate(),
                startTime, endTime, req.getShiftType(), null, customer);

        if (conflict.isHasConflict() && "OVERLAP".equals(conflict.getConflictType())) {
            Shift conflictShift = conflict.getConflictingShiftId() != null
                    ? shiftRepository.findById(conflict.getConflictingShiftId()).orElse(null)
                    : null;

            String timeRange = conflictShift != null
                    ? conflictShift.getStartTime().toString() + "-" + conflictShift.getEndTime().toString()
                    : null;
            String existingCustomerName = (conflictShift != null && conflictShift.getCustomer() != null)
                    ? conflictShift.getCustomer().getName() : "";

            String msg = String.format("Trùng lịch: %s đã có ca trực từ %s tại %s",
                    employee.getFullName(), timeRange, existingCustomerName);

            throw new ShiftAssignException("SCHEDULE_CONFLICT", msg, HttpStatus.BAD_REQUEST,
                    AssignConflictDetail.builder()
                            .existingShiftId(conflict.getConflictingShiftId())
                            .timeRange(timeRange)
                            .build());
        }

        boolean distanceWarning = conflict.isHasConflict() && "BUFFER".equals(conflict.getConflictType());
        String  warningMessage  = distanceWarning ? conflict.getDetail() : null;

        int durationMinutes  = calcDuration(req.getShiftType(), startTime, endTime);
        int existingMinutes  = shiftRepository.sumDurationMinutesByEmployeeAndDate(
                req.getEmployeeId(), req.getShiftDate());
        final int MAX_MINUTES = 12 * 60;

        if (existingMinutes + durationMinutes > MAX_MINUTES) {
            throw new ShiftAssignException("EXCEED_WORKING_HOURS",
                    String.format("Tổng giờ làm trong ngày vượt quá 12 tiếng (hiện tại: %.1fh, ca mới: %.1fh)",
                            existingMinutes / 60.0, durationMinutes / 60.0),
                    HttpStatus.BAD_REQUEST);
        }

        Shift shift = Shift.builder()
                .employee(employee)
                .customer(customer)
                .shiftDate(req.getShiftDate())
                .startTime(startTime)
                .endTime(endTime)
                .durationMinutes(durationMinutes)
                .shiftType(req.getShiftType())
                .otMultiplier(defaultOtMultiplier(req.getShiftType()))
                .status(ShiftStatus.ASSIGNED)
                .build();

        Shift saved = shiftRepository.save(shift);
        logNotification(saved);

        AssignShiftResponse data = AssignShiftResponse.builder()
                .shiftId(saved.getId())
                .employeeName(employee.getFullName())
                .customerName(customer.getName())
                .status(saved.getStatus())
                .build();

        return new AssignShiftResult(data, warningMessage);
    }

    public record AssignShiftResult(AssignShiftResponse data, String distanceWarning) {
        public boolean hasWarning() { return distanceWarning != null; }
    }

    @Transactional
    public AssignToShiftResponse assignEmployee(Long shiftId, Long employeeId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ShiftAssignException("SHIFT_NOT_FOUND",
                        "Không tìm thấy ca làm việc (id=" + shiftId + ")",
                        HttpStatus.NOT_FOUND));

        if (shift.getStatus() == ShiftStatus.CANCELLED) {
            throw new ShiftAssignException("SHIFT_NOT_FOUND",
                    "Ca làm việc đã bị huỷ, không thể gán nhân viên",
                    HttpStatus.NOT_FOUND);
        }
        if (shift.getStatus() == ShiftStatus.IN_PROGRESS || shift.getStatus() == ShiftStatus.COMPLETED) {
            throw new ShiftAssignException("SHIFT_NOT_FOUND",
                    "Không thể gán nhân viên cho ca đang thực hiện hoặc đã hoàn thành",
                    HttpStatus.BAD_REQUEST);
        }

        User employee = userRepository.findByIdAndDeletedAtIsNull(employeeId)
                .orElseThrow(() -> new ShiftAssignException("SHIFT_NOT_FOUND",
                        "Không tìm thấy nhân viên (id=" + employeeId + ")",
                        HttpStatus.NOT_FOUND));

        if (employee.getStatus() == UserStatus.INACTIVE || employee.getStatus() == UserStatus.ON_LEAVE) {
            throw new ShiftAssignException("EMPLOYEE_INACTIVE",
                    "Nhân viên đang bị khóa hoặc đang trong trạng thái nghỉ phép",
                    HttpStatus.FORBIDDEN);
        }

        ConflictCheckResponse conflict = conflictChecker.check(
                employeeId, shift.getShiftDate(),
                shift.getStartTime(), shift.getEndTime(),
                shift.getShiftType(), shiftId, shift.getCustomer());

        if (conflict.isHasConflict() && "OVERLAP".equals(conflict.getConflictType())) {
            Shift conflictShift = conflict.getConflictingShiftId() != null
                    ? shiftRepository.findById(conflict.getConflictingShiftId()).orElse(null)
                    : null;

            String timeRange = conflictShift != null
                    ? conflictShift.getStartTime() + "-" + conflictShift.getEndTime()
                    : null;
            String existingCustomerName = (conflictShift != null && conflictShift.getCustomer() != null)
                    ? conflictShift.getCustomer().getName() : "";

            String msg = String.format("Xung đột lịch trình: %s đã có ca làm từ %s tại %s",
                    employee.getFullName(), timeRange, existingCustomerName);

            throw new ShiftAssignException("SCHEDULE_CONFLICT", msg, HttpStatus.BAD_REQUEST,
                    AssignConflictDetail.builder()
                            .existingShiftId(conflict.getConflictingShiftId())
                            .timeRange(timeRange)
                            .build());
        }

        int existingMinutes = shiftRepository.sumDurationMinutesByEmployeeAndDate(
                employeeId, shift.getShiftDate());
        final int MAX_MINUTES = 12 * 60;

        if (existingMinutes + shift.getDurationMinutes() > MAX_MINUTES) {
            throw new ShiftAssignException("MAX_HOURS_EXCEEDED",
                    String.format("Nhân viên đã làm việc quá định mức giờ trong ngày " +
                                  "(hiện tại: %.1fh, ca này thêm: %.1fh, tối đa: 12h)",
                            existingMinutes / 60.0, shift.getDurationMinutes() / 60.0),
                    HttpStatus.BAD_REQUEST);
        }

        shift.setEmployee(employee);
        shift.setStatus(ShiftStatus.ASSIGNED);
        shiftRepository.save(shift);

        return AssignToShiftResponse.builder()
                .id(shift.getId())
                .employeeId(employee.getId())
                .employeeName(employee.getFullName())
                .status(shift.getStatus())
                .updatedAt(shift.getUpdatedAt())
                .build();
    }

    @Transactional
    public ShiftResponse update(Long id, ShiftRequest req) {
        Shift shift = findActiveShift(id);

        if (shift.getStatus() == ShiftStatus.COMPLETED || shift.getStatus() == ShiftStatus.IN_PROGRESS) {
            throw new BadRequestException("Không thể sửa ca đang thực hiện hoặc đã hoàn thành");
        }

        Customer customer = findCustomer(req.getCustomerId());
        validateOvernightRule(req.getShiftType(), req.getStartTime(), req.getEndTime());

        if (req.getEmployeeId() == null) {
            shift.setEmployee(null);
            shift.setStatus(ShiftStatus.PUBLISHED);
        } else {
            User employee = findEmployee(req.getEmployeeId());
            ConflictCheckResponse conflict = conflictChecker.check(
                    req.getEmployeeId(), req.getShiftDate(),
                    req.getStartTime(), req.getEndTime(),
                    req.getShiftType(), id, customer);
            handleConflict(conflict, req.getShiftType());
            shift.setEmployee(employee);
            if (shift.getStatus() != ShiftStatus.CONFIRMED) {
                shift.setStatus(ShiftStatus.ASSIGNED);
            }
        }

        shift.setCustomer(customer);
        shift.setShiftDate(req.getShiftDate());
        shift.setStartTime(req.getStartTime());
        shift.setEndTime(req.getEndTime());
        shift.setDurationMinutes(calcDuration(req.getShiftType(), req.getStartTime(), req.getEndTime()));
        shift.setShiftType(req.getShiftType());
        shift.setOtMultiplier(defaultOtMultiplier(req.getShiftType()));
        shift.setNotes(req.getNotes());

        shiftRepository.save(shift);
        return toResponse(shift);
    }

    @Transactional
    public void cancel(Long id) {
        Shift shift = findActiveShift(id);
        if (shift.getStatus() == ShiftStatus.COMPLETED || shift.getStatus() == ShiftStatus.IN_PROGRESS) {
            throw new BadRequestException("Không thể huỷ ca đang thực hiện hoặc đã hoàn thành");
        }
        shift.setStatus(ShiftStatus.CANCELLED);
        shiftRepository.save(shift);
    }

    @Transactional
    public void unassignEmployee(Long id) {
        Shift shift = findActiveShift(id);
        if (shift.getStatus() == ShiftStatus.COMPLETED || shift.getStatus() == ShiftStatus.IN_PROGRESS) {
            throw new BadRequestException("Không thể gỡ nhân viên khỏi ca đang thực hiện hoặc đã hoàn thành");
        }
        if (shift.getEmployee() == null) {
            throw new BadRequestException("Ca này chưa được gán nhân viên");
        }
        shift.setEmployee(null);
        shift.setStatus(ShiftStatus.PUBLISHED);
        shiftRepository.save(shift);
        log.info("Unassigned employee from shift id={}", id);
    }

    @Transactional
    public void hardDelete(Long id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ca không tìm thấy (id={})", id));

        if (shift.getStatus() == ShiftStatus.COMPLETED || shift.getStatus() == ShiftStatus.IN_PROGRESS) {
            throw new BadRequestException("Không thể xóa ca đang thực hiện hoặc đã hoàn thành");
        }

        // Kiểm tra xem có yêu cầu đổi ca nào đang PENDING không
        // Nếu có, chúng ta nên hủy chúng hoặc báo lỗi. Ở đây báo lỗi cho an toàn.
        // existsPendingForRequesterShift ...
        
        shiftRepository.delete(shift);
        log.info("Hard deleted shift id={}", id);
    }

    @Transactional
    public void autoProcessShifts() {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        // 1. Xử lý ca MISSED (Quá 1 tiếng chưa check-in)
        LocalTime missedThreshold = now.minusHours(1);
        List<Shift> missedShifts = shiftRepository.findPendingMissedShifts(today, missedThreshold);
        if (!missedShifts.isEmpty()) {
            missedShifts.forEach(s -> s.setStatus(ShiftStatus.MISSED));
            shiftRepository.saveAll(missedShifts);
            log.info("Auto-marked {} shifts as MISSED", missedShifts.size());
        }

        // 2. Xử lý ca MISSING_OUT (Quá 2 tiếng chưa check-out)
        LocalTime missingOutThreshold = now.minusHours(2);
        List<Shift> missingOutShifts = shiftRepository.findPendingMissingOutShifts(today, today.minusDays(1), missingOutThreshold);
        if (!missingOutShifts.isEmpty()) {
            missingOutShifts.forEach(s -> s.setStatus(ShiftStatus.MISSING_OUT));
            shiftRepository.saveAll(missingOutShifts);
            log.info("Auto-marked {} shifts as MISSING_OUT", missingOutShifts.size());
        }
    }

    @Transactional(readOnly = true)
    public ConflictCheckResponse preCheck(Long employeeId, LocalDate shiftDate,
                                          LocalTime startTime, LocalTime endTime,
                                          ShiftType shiftType, Long excludeShiftId) {
        validateOvernightRule(shiftType, startTime, endTime);
        return conflictChecker.check(employeeId, shiftDate, startTime, endTime, shiftType, excludeShiftId);
    }

    @Transactional
    public CopyWeekResponse copyWeek(CopyWeekRequest req) {
        int[] srcYW = parseIsoWeek(req.getSourceWeek());
        int[] tgtYW = parseIsoWeek(req.getTargetWeek());

        LocalDate srcStart = isoWeekStart(srcYW[0], srcYW[1]);
        LocalDate tgtStart = isoWeekStart(tgtYW[0], tgtYW[1]);

        List<Shift> sourceShifts = shiftRepository.findScheduledInRange(
                srcStart, srcStart.plusDays(6));

        int copied = 0;
        int skipped = 0;
        List<ConflictCheckResponse> conflicts = new ArrayList<>();

        for (Shift src : sourceShifts) {
            long offset = src.getShiftDate().toEpochDay() - srcStart.toEpochDay();
            LocalDate newDate = tgtStart.plusDays(offset);

            ConflictCheckResponse conflict = conflictChecker.check(
                    src.getEmployee().getId(), newDate,
                    src.getStartTime(), src.getEndTime(),
                    src.getShiftType(), null, src.getCustomer());

            if (conflict.isHasConflict()) {
                conflicts.add(ConflictCheckResponse.builder()
                        .hasConflict(true)
                        .conflictType(conflict.getConflictType())
                        .detail("[" + newDate + " " + src.getEmployee().getFullName() + "] " + conflict.getDetail())
                        .minutesShort(conflict.getMinutesShort())
                        .build());
                skipped++;
                continue;
            }

            Shift copy = Shift.builder()
                    .employee(src.getEmployee())
                    .customer(src.getCustomer())
                    .shiftDate(newDate)
                    .startTime(src.getStartTime())
                    .endTime(src.getEndTime())
                    .durationMinutes(src.getDurationMinutes())
                    .shiftType(src.getShiftType())
                    .otMultiplier(src.getOtMultiplier())
                    .notes(src.getNotes())
                    .status(ShiftStatus.ASSIGNED)
                    .build();
            shiftRepository.save(copy);
            copied++;
        }

        return CopyWeekResponse.builder()
                .copied(copied)
                .skipped(skipped)
                .conflicts(conflicts)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AvailableEmployeeResponse> findAvailableEmployees(
            LocalDate shiftDate, LocalTime startTime, LocalTime endTime, ShiftType shiftType) {

        List<User> allEmployees = userRepository.findAllByStatusAndDeletedAtIsNull(UserStatus.ACTIVE);
        List<AvailableEmployeeResponse> result = new ArrayList<>();

        for (User emp : allEmployees) {
            ConflictCheckResponse conflict = conflictChecker.check(
                    emp.getId(), shiftDate, startTime, endTime, shiftType, null);

            if (!conflict.isHasConflict() || "BUFFER".equals(conflict.getConflictType())) {
                List<Shift> dayShifts = shiftRepository.findActiveShiftsNear(
                        emp.getId(), shiftDate, shiftDate, null);

                LocalTime nextEnd = dayShifts.stream()
                        .filter(s -> !s.getStartTime().isBefore(startTime))
                        .map(Shift::getEndTime)
                        .findFirst()
                        .orElse(null);

                result.add(AvailableEmployeeResponse.builder()
                        .employeeId(emp.getId())
                        .employeeName(emp.getFullName())
                        .phoneNumber(emp.getPhoneNumber())
                        .nextShiftEndTime(nextEnd)
                        .build());
            }
        }
        return result;
    }

    @Transactional
    public RecurringShiftResponse createRecurring(RecurringShiftRequest req) {
        if (req.getStartDate().plusDays(180).isBefore(req.getEndDate())) {
            throw new BadRequestException("Khoảng lặp tối đa 180 ngày");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("endDate phải sau startDate");
        }

        validateOvernightRule(req.getShiftType(), req.getStartTime(), req.getEndTime());
        int duration = calcDuration(req.getShiftType(), req.getStartTime(), req.getEndTime());
        Customer customer = findCustomer(req.getCustomerId());
        User employee = req.getEmployeeId() != null ? findEmployee(req.getEmployeeId()) : null;

        List<Long> createdIds = new ArrayList<>();
        List<ConflictCheckResponse> conflicts = new ArrayList<>();
        int skipped = 0;

        LocalDate current = req.getStartDate();
        while (!current.isAfter(req.getEndDate())) {
            if (req.getDaysOfWeek().contains(current.getDayOfWeek())) {
                LocalDate shiftDate = current;

                if (employee != null) {
                    ConflictCheckResponse conflict = conflictChecker.check(
                            employee.getId(), shiftDate,
                            req.getStartTime(), req.getEndTime(),
                            req.getShiftType(), null, customer);

                    if (conflict.isHasConflict() && "OVERLAP".equals(conflict.getConflictType())) {
                        conflicts.add(ConflictCheckResponse.builder()
                                .hasConflict(true)
                                .conflictType(conflict.getConflictType())
                                .detail("[" + shiftDate + "] " + conflict.getDetail())
                                .minutesShort(conflict.getMinutesShort())
                                .build());
                        skipped++;
                        current = current.plusDays(1);
                        continue;
                    }
                }

                Shift shift = Shift.builder()
                        .employee(employee)
                        .customer(customer)
                        .shiftDate(shiftDate)
                        .startTime(req.getStartTime())
                        .endTime(req.getEndTime())
                        .durationMinutes(duration)
                        .shiftType(req.getShiftType())
                        .otMultiplier(defaultOtMultiplier(req.getShiftType()))
                        .notes(req.getNotes())
                        .status(employee != null ? ShiftStatus.ASSIGNED : ShiftStatus.PUBLISHED)
                        .build();

                Shift saved = shiftRepository.save(shift);
                createdIds.add(saved.getId());
            }
            current = current.plusDays(1);
        }

        return RecurringShiftResponse.builder()
                .created(createdIds.size())
                .skipped(skipped)
                .createdShiftIds(createdIds)
                .conflicts(conflicts)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> getOpenShifts() {
        return shiftRepository.findOpenShifts(LocalDate.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShiftResponse claimShift(Long shiftId, Long employeeId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("Ca không tìm thấy (id={})", shiftId));

        if (shift.getStatus() != ShiftStatus.PUBLISHED) {
            throw new BadRequestException("Ca này không ở trạng thái PUBLISHED, không thể đăng ký");
        }
        User employee = findEmployee(employeeId);
        ConflictCheckResponse conflict = conflictChecker.check(
                employeeId, shift.getShiftDate(),
                shift.getStartTime(), shift.getEndTime(),
                shift.getShiftType(), shiftId, shift.getCustomer());

        if (conflict.isHasConflict()) {
            throw new ConflictException("Không thể nhận ca: " + conflict.getDetail());
        }

        shift.setEmployee(employee);
        shift.setStatus(ShiftStatus.ASSIGNED);
        shiftRepository.save(shift);
        return toResponse(shift);
    }

    @Transactional
    public ShiftResponse confirmShift(Long shiftId, Long employeeId) {
        Shift shift = findActiveShift(shiftId);
        if (shift.getEmployee() == null || !shift.getEmployee().getId().equals(employeeId)) {
            throw new BadRequestException("Bạn không phải nhân viên được gán cho ca này");
        }
        if (shift.getStatus() != ShiftStatus.ASSIGNED && shift.getStatus() != ShiftStatus.SCHEDULED) {
            throw new BadRequestException("Chỉ có thể xác nhận ca ở trạng thái ASSIGNED.");
        }
        shift.setStatus(ShiftStatus.CONFIRMED);
        shiftRepository.save(shift);
        return toResponse(shift);
    }

    private void handleConflict(ConflictCheckResponse conflict, ShiftType shiftType) {
        if (!conflict.isHasConflict()) return;
        if ("OVERLAP".equals(conflict.getConflictType())) {
            throw new ConflictException("SHIFT_CONFLICT: " + conflict.getDetail());
        }
        if (ShiftType.OT_EMERGENCY != shiftType) {
            throw new ConflictException("SHIFT_BUFFER: " + conflict.getDetail());
        }
    }

    private boolean isCheckInAllowed(ShiftStatus status) {
        return status == ShiftStatus.ASSIGNED || status == ShiftStatus.SCHEDULED || status == ShiftStatus.CONFIRMED;
    }

    private Shift findActiveShift(Long id) {
        return shiftRepository.findByIdAndStatusNot(id, ShiftStatus.CANCELLED)
                .orElseThrow(() -> new NotFoundException("Ca không tìm thấy (id={})", id));
    }

    private User findEmployee(Long id) {
        User employee = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Nhân viên không tồn tại (id={})", id));
        if (employee.getStatus() == UserStatus.ON_LEAVE || employee.getStatus() == UserStatus.INACTIVE) {
            throw new BadRequestException("Nhân viên không ở trạng thái sẵn sàng để gán ca.");
        }
        return employee;
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Khách hàng không tồn tại (id={})", id));
    }

    private void validateOvernightRule(ShiftType type, LocalTime start, LocalTime end) {
        if (end.equals(start)) {
            throw new BadRequestException("Giờ bắt đầu và kết thúc không được giống nhau");
        }
    }

    private int calcDuration(ShiftType type, LocalTime start, LocalTime end) {
        if (end.isBefore(start)) {
            int untilMidnight = (24 * 60) - (start.getHour() * 60 + start.getMinute());
            int afterMidnight = end.getHour() * 60 + end.getMinute();
            return untilMidnight + afterMidnight;
        }
        return (end.getHour() * 60 + end.getMinute()) - (start.getHour() * 60 + start.getMinute());
    }

    private java.math.BigDecimal defaultOtMultiplier(ShiftType type) {
        return switch (type) {
            case OT_EMERGENCY -> new java.math.BigDecimal("1.5");
            case HOLIDAY      -> new java.math.BigDecimal("2.0");
            default           -> java.math.BigDecimal.ONE;
        };
    }

    private void logNotification(Shift shift) {
        log.info("[NOTIFICATION] Ca mới id={} nhân viên={} ngày={}",
                shift.getId(), shift.getEmployee().getFullName(), shift.getShiftDate());
    }

    private int[] parseIsoWeek(String isoWeek) {
        String[] parts = isoWeek.split("-W");
        if (parts.length != 2) throw new BadRequestException("Định dạng tuần không hợp lệ.");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private LocalDate isoWeekStart(int year, int week) {
        return LocalDate.now()
                .withYear(year)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    ShiftResponse toResponse(Shift s) {
        Customer c = s.getCustomer();
        return ShiftResponse.builder()
                .id(s.getId())
                .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                .employeeName(s.getEmployee() != null ? s.getEmployee().getFullName() : null)
                .customerId(c.getId())
                .customerName(c.getName())
                .customerLatitude(c.getLatitude())
                .customerLongitude(c.getLongitude())
                .customerAddress(c.getAddress())
                .packageId(s.getServicePackage() != null ? s.getServicePackage().getId() : null)
                .shiftDate(s.getShiftDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .durationMinutes(s.getDurationMinutes())
                .shiftType(s.getShiftType())
                .otMultiplier(s.getOtMultiplier())
                .status(s.getStatus())
                .notes(s.getNotes())
                .checkInTime(s.getCheckInTime())
                .checkInLat(s.getCheckInLat())
                .checkInLng(s.getCheckInLng())
                .checkInDistanceMeters(s.getCheckInDistanceMeters())
                .checkInPhoto(s.getCheckInPhoto())
                .checkOutTime(s.getCheckOutTime())
                .checkOutLat(s.getCheckOutLat())
                .checkOutLng(s.getCheckOutLng())
                .checkOutDistanceMeters(s.getCheckOutDistanceMeters())
                .actualMinutes(s.getActualMinutes())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    @Transactional
    public CheckInResponse checkIn(Long shiftId, CheckInRequest request) {
        Shift shift = findActiveShift(shiftId);
        if (!isCheckInAllowed(shift.getStatus())) throw new BadRequestException("Không thể check-in.");
        double distance = computeDistance(shift.getCustomer(), request.getLatitude(), request.getLongitude());
        boolean withinGeofence = shift.getCustomer().getLatitude() == null || distance <= geofenceRadiusMeters;
        if (!withinGeofence && (request.getPhotoUrl() == null || request.getPhotoUrl().isBlank())) {
            throw new BadRequestException("Ngoài geofence: bắt buộc gửi kèm ảnh.");
        }
        LocalDateTime now = LocalDateTime.now();
        shift.setCheckInTime(now);
        shift.setCheckInLat(request.getLatitude());
        shift.setCheckInLng(request.getLongitude());
        shift.setCheckInDistanceMeters(shift.getCustomer().getLatitude() != null ? distance : null);
        shift.setCheckInPhoto(request.getPhotoUrl());
        shift.setStatus(ShiftStatus.IN_PROGRESS);
        shiftRepository.save(shift);
        return CheckInResponse.builder()
                .withinGeofence(withinGeofence)
                .distanceMeters(distance)
                .geofenceRadiusMeters(geofenceRadiusMeters)
                .actionTime(now)
                .shiftStatus(shift.getStatus())
                .message(withinGeofence ? "Check-in thành công" : "Check-in ghi nhận ngoài geofence.")
                .build();
    }

    @Transactional
    public CheckInResponse checkOut(Long shiftId, CheckInRequest request) {
        Shift shift = findActiveShift(shiftId);
        if (shift.getStatus() != ShiftStatus.IN_PROGRESS) throw new BadRequestException("Ca chưa check-in.");
        double distance = computeDistance(shift.getCustomer(), request.getLatitude(), request.getLongitude());
        LocalDateTime now = LocalDateTime.now();
        /*
        if (now.isBefore(shift.getCheckInTime().plusMinutes(1))) throw new BadRequestException("Check-out quá sớm.");
        */
        
        LocalTime schedEndTime = shift.getEndTime();
        LocalDateTime schedEndDt = LocalDateTime.of(shift.getShiftDate(), schedEndTime);
        if (schedEndTime.isBefore(shift.getStartTime())) schedEndDt = schedEndDt.plusDays(1);

        int actualMinutes = now.isBefore(schedEndDt) ? shift.getDurationMinutes() 
                : (int) java.time.temporal.ChronoUnit.MINUTES.between(shift.getCheckInTime(), now);

        shift.setCheckOutTime(now);
        shift.setCheckOutLat(request.getLatitude());
        shift.setCheckOutLng(request.getLongitude());
        shift.setCheckOutDistanceMeters(shift.getCustomer().getLatitude() != null ? distance : null);
        shift.setActualMinutes(actualMinutes);
        shift.setStatus(ShiftStatus.COMPLETED);
        shiftRepository.save(shift);
        return CheckInResponse.builder()
                .withinGeofence(shift.getCustomer().getLatitude() == null || distance <= geofenceRadiusMeters)
                .distanceMeters(distance)
                .geofenceRadiusMeters(geofenceRadiusMeters)
                .actionTime(now)
                .shiftStatus(shift.getStatus())
                .message("Check-out thành công.")
                .build();
    }

    private double computeDistance(Customer customer, double lat, double lng) {
        if (customer.getLatitude() == null || customer.getLongitude() == null) return 0;
        return haversine(customer.getLatitude(), customer.getLongitude(), lat, lng);
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
