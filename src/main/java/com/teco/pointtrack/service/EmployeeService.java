package com.teco.pointtrack.service;

import com.teco.pointtrack.common.AuthUtils;
import com.teco.pointtrack.dto.employee.*;
import com.teco.pointtrack.entity.*;
import com.teco.pointtrack.entity.enums.ShiftStatus;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final UserRepository             userRepository;
    private final RoleRepository             roleRepository;
    private final SalaryLevelRepository      salaryLevelRepository;
    private final SalaryLevelHistoryRepository historyRepository;
    private final ShiftRepository            shiftRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final PasswordEncoder            passwordEncoder;
    private final PasswordService            passwordService;

    // ─────────────────────────────────────────────────────────────────────────
    // LIST  GET /v1/employees
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> listEmployees(
            UserStatus status, String area, Long salaryLevelId,
            String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (area != null && !area.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("area")),
                        "%" + area.trim().toLowerCase() + "%"));
            }
            if (salaryLevelId != null) {
                predicates.add(cb.equal(root.get("salaryLevel").get("id"), salaryLevelId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), kw),
                        cb.like(cb.lower(root.get("phoneNumber")), kw)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(this::toResponse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE  POST /v1/employees
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest req) {
        String phone = req.getPhone().trim();
        String email = req.getEmail().trim().toLowerCase();

        // 1. Kiểm tra phone
        var existingByPhone = userRepository.findByPhoneNumber(phone);
        if (existingByPhone.isPresent()) {
            User user = existingByPhone.get();
            if (user.getDeletedAt() == null) {
                throw new ConflictException("PHONE_ALREADY_EXISTS");
            }
            // Nếu đã xóa -> Reactivate
            return reactivateEmployee(user, req);
        }

        // 2. Kiểm tra email
        var existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();
            if (user.getDeletedAt() == null) {
                throw new ConflictException("EMAIL_ALREADY_EXISTS");
            }
            // Nếu đã xóa -> Reactivate
            return reactivateEmployee(user, req);
        }

        var userRole = roleRepository.findBySlug("USER")
                .orElseThrow(() -> new BadRequestException("Cấu hình lỗi: Role USER chưa được tạo"));

        SalaryLevel salaryLevel = resolveSalaryLevel(req.getSalaryLevelId());

        String tempPassword = passwordService.generateTempPassword();

        User user = User.builder()
                .fullName(req.getFullName().trim())
                .phoneNumber(phone)
                .email(email)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .avatarUrl(req.getAvatarUrl())
                .area(req.getArea())
                .skills(req.getSkills())
                .status(UserStatus.ACTIVE)
                .isFirstLogin(true)
                .startDate(parseDate(req.getHiredDate()))
                .role(userRole)
                .salaryLevel(salaryLevel)
                .build();

        userRepository.save(user);

        // Gửi email mật khẩu tạm
        passwordService.sendTempPasswordEmail(
                user.getEmail(), user.getFullName(), tempPassword, user.getPhoneNumber());

        log.info("Tạo nhân viên mới: id={} phone={}", user.getId(), user.getPhoneNumber());
        return toResponse(user);
    }

    private EmployeeResponse reactivateEmployee(User user, EmployeeCreateRequest req) {
        String tempPassword = passwordService.generateTempPassword();

        var userRole = roleRepository.findBySlug("USER")
                .orElseThrow(() -> new BadRequestException("Cấu hình lỗi: Role USER chưa được tạo"));

        SalaryLevel salaryLevel = resolveSalaryLevel(req.getSalaryLevelId());

        // Reset data cho user cũ
        user.setFullName(req.getFullName().trim());
        user.setPhoneNumber(req.getPhone().trim());
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setAvatarUrl(req.getAvatarUrl());
        user.setArea(req.getArea());
        user.setSkills(req.getSkills());
        user.setStatus(UserStatus.ACTIVE);
        user.setFirstLogin(true);
        user.setDeletedAt(null);
        user.setStartDate(parseDate(req.getHiredDate()));
        user.setRole(userRole);
        user.setSalaryLevel(salaryLevel);

        userRepository.save(user);

        passwordService.sendTempPasswordEmail(
                user.getEmail(), user.getFullName(), tempPassword, user.getPhoneNumber());

        log.info("Khôi phục tài khoản nhân viên cũ: id={} phone={}", user.getId(), user.getPhoneNumber());
        return toResponse(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET DETAIL  GET /v1/employees/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeDetail(Long id) {
        User user = findActiveUser(id);

        // Stats cho tháng hiện tại
        LocalDate now     = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd   = now.withDayOfMonth(now.lengthOfMonth());
        LocalDateTime dtFrom = monthStart.atStartOfDay();
        LocalDateTime dtTo   = monthEnd.plusDays(1).atStartOfDay();

        long total     = shiftRepository.countActiveByEmployeeAndDateRange(id, monthStart, monthEnd);
        long completed = shiftRepository.countCompletedByEmployeeAndDateRange(id, monthStart, monthEnd);
        long lateIn    = attendanceRepository.countLateCheckinsByUserAndPeriod(id, dtFrom, dtTo);
        long lateOut   = attendanceRepository.countEarlyLeavesByUserAndPeriod(id, dtFrom, dtTo);
        double rate    = total > 0 ? Math.round((completed * 100.0 / total) * 10.0) / 10.0 : 0.0;

        EmployeeStatsDto stats = EmployeeStatsDto.builder()
                .totalShiftsThisMonth(total)
                .lateCheckinsThisMonth(lateIn)
                .lateCheckoutsThisMonth(lateOut)
                .completionRate(rate)
                .build();

        List<SalaryHistoryResponse> history = historyRepository
                .findByEmployeeIdOrderByEffectiveDateDesc(id)
                .stream()
                .map(this::toHistoryResponse)
                .toList();

        return EmployeeDetailResponse.builder()
                .profile(toResponse(user))
                .stats(stats)
                .salaryLevelHistory(history)
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeProfileResponse getEmployeeProfile() {
        User user = resolveCurrentUser();
        Long userId = user.getId();

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        LocalDateTime dtFrom = monthStart.atStartOfDay();
        LocalDateTime dtTo = now.plusDays(1).atStartOfDay();

        // 1. Summary stats for this month
        long totalWorkDays = attendanceRepository.countWorkDaysByUserAndPeriod(userId, dtFrom, dtTo);
        Double otHours = attendanceRepository.sumOtHoursByUserAndPeriod(userId, dtFrom, dtTo);
        long lateDays = attendanceRepository.countLateCheckinsByUserAndPeriod(userId, dtFrom, dtTo);

        // BR-16.2: Tính tổng giờ làm và lương dựa trên work_date và thực tế
        Long totalMinutes = attendanceRepository.sumActualMinutesByUserIdAndWorkDateBetween(userId, monthStart, monthEnd);
        Double weightedMinutes = attendanceRepository.sumWeightedMinutesByUserIdAndWorkDateBetween(userId, monthStart, monthEnd);

        double baseSalary = (user.getSalaryLevel() != null) ? user.getSalaryLevel().getBaseSalary().doubleValue() : 0.0;
        double totalHoursVal = (totalMinutes != null) ? totalMinutes / 60.0 : 0.0;
        long estimatedSalary = (weightedMinutes != null) ? Math.round(weightedMinutes / 60.0 * baseSalary) : 0L;

        EmployeeProfileResponse.Summary summary = EmployeeProfileResponse.Summary.builder()
                .totalWorkDaysThisMonth(totalWorkDays)
                .totalHoursThisMonth(Math.round(totalHoursVal * 10.0) / 10.0)
                .otHoursThisMonth(otHours != null ? Math.round(otHours * 10.0) / 10.0 : 0.0)
                .lateDaysThisMonth(lateDays)
                .estimatedSalaryThisMonth(estimatedSalary)
                .build();

        // 2. History for last 6 months
        LocalDate sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1);
        List<Object[]> historyResults = attendanceRepository.countWorkDaysHistoryByUser(userId, sixMonthsAgo.atStartOfDay());

        List<EmployeeProfileResponse.History> history = new ArrayList<>();
        // Pre-populate last 6 months to ensure they appear even if 0 days
        for (int i = 5; i >= 0; i--) {
            LocalDate m = now.minusMonths(i);
            int monthValue = m.getMonthValue();
            long days = historyResults.stream()
                    .filter(r -> ((Number) r[0]).intValue() == monthValue)
                    .map(r -> ((Number) r[1]).longValue())
                    .findFirst()
                    .orElse(0L);

            history.add(EmployeeProfileResponse.History.builder()
                    .month("T" + monthValue)
                    .days(days)
                    .build());
        }

        return EmployeeProfileResponse.builder()
                .id(user.getId())
                .employeeCode("NV" + String.format("%03d", user.getId())) // Placeholder logic for employeeCode
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getSlug() : null)
                .avatarUrl(user.getAvatarUrl())
                .position(user.getSalaryLevel() != null ? user.getSalaryLevel().getName() : "Nhân viên")
                .department(user.getArea() != null ? "Phòng vận hành (" + user.getArea() + ")" : "Phòng vận hành")
                .hiredDate(user.getStartDate())
                .status(user.getStatus().name())
                .workStatistics(EmployeeProfileResponse.WorkStatistics.builder()
                        .summary(summary)
                        .history(history)
                        .build())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE  PUT /v1/employees/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest req) {
        User user = findActiveUser(id);

        if (req.getFullName() != null)  user.setFullName(req.getFullName().trim());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        if (req.getArea() != null)      user.setArea(req.getArea());
        if (req.getSkills() != null)    user.setSkills(req.getSkills());

        if (req.getEmail() != null) {
            String newEmail = req.getEmail().trim().toLowerCase();
            boolean conflict = userRepository.existsByEmailAndDeletedAtIsNull(newEmail)
                    && !newEmail.equals(user.getEmail());
            if (conflict) throw new ConflictException("EMAIL_ALREADY_EXISTS");
            user.setEmail(newEmail);
        }

        // ── Salary level change detection ─────────────────────────────────────
        if (req.getSalaryLevelId() != null) {
            Long currentLevelId = user.getSalaryLevel() != null ? user.getSalaryLevel().getId() : null;
            if (!req.getSalaryLevelId().equals(currentLevelId)) {
                if (req.getSalaryChangeReason() == null || req.getSalaryChangeReason().isBlank()) {
                    throw new BadRequestException("SALARY_CHANGE_REASON_REQUIRED");
                }
                SalaryLevel newLevel = salaryLevelRepository.findByIdAndDeletedAtIsNull(req.getSalaryLevelId())
                        .orElseThrow(() -> new NotFoundException("SALARY_LEVEL_NOT_FOUND"));
                SalaryLevel oldLevel = user.getSalaryLevel();

                User changedBy = resolveCurrentUser();

                LocalDate effectiveDate = req.getSalaryEffectiveDate() != null
                        ? req.getSalaryEffectiveDate()
                        : LocalDate.now();

                SalaryLevelHistory histEntry = SalaryLevelHistory.builder()
                        .employee(user)
                        .oldLevel(oldLevel)
                        .newLevel(newLevel)
                        .effectiveDate(effectiveDate)
                        .changedBy(changedBy)
                        .reason(req.getSalaryChangeReason().trim())
                        .build();
                historyRepository.save(histEntry);

                user.setSalaryLevel(newLevel);
                log.info("Thay đổi cấp bậc lương: employee={} {} → {}",
                        id,
                        oldLevel != null ? oldLevel.getName() : "null",
                        newLevel.getName());
            }
        }

        // ── Status change ─────────────────────────────────────────────────────
        if (req.getStatus() != null && req.getStatus() != user.getStatus()) {
            user.setStatus(req.getStatus());
            if (req.getStatus() == UserStatus.INACTIVE) {
                cancelFutureShifts(id);
            }
        }

        userRepository.save(user);
        return toResponse(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE  DELETE /v1/employees/{id}   (BR-22 soft delete)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public String deleteEmployee(Long id) {
        User user = findActiveUser(id);
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        int cancelled = cancelFutureShifts(id);
        log.info("Soft-delete nhân viên id={}, hủy {} ca tương lai", id, cancelled);

        if (cancelled > 0) {
            return String.format("Đã vô hiệu hóa NV. %d ca tương lai đã bị hủy.", cancelled);
        }
        return "Đã vô hiệu hóa nhân viên thành công.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUMMARY STATS  GET /v1/employees/stats
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EmployeeSummaryStats getSummaryStats() {
        LocalDate today      = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd   = today.withDayOfMonth(today.lengthOfMonth());

        List<Object[]> results = userRepository.fetchSummaryStats(monthStart, monthEnd);
        Object[] row = results.isEmpty() ? new Object[]{0L, 0L, 0L, 0L} : results.get(0);

        return EmployeeSummaryStats.builder()
                .totalEmployees  (toLong(row[0]))
                .activeEmployees (toLong(row[1]))
                .onLeaveEmployees(toLong(row[2]))
                .newThisMonth    (toLong(row[3]))
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeStatisticsResponse getStatistics() {
        LocalDate today = LocalDate.now();

        // Tháng này
        LocalDateTime thisStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime thisEnd   = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59);

        // Tháng trước
        LocalDate lastMonth = today.minusMonths(1);
        LocalDateTime lastStart = lastMonth.withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastEnd   = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).atTime(23, 59, 59);

        List<Object[]> results = userRepository.fetchStatistics(thisStart, thisEnd, lastStart, lastEnd);
        Object[] row = results.isEmpty() ? new Object[]{0L, 0L, 0L, 0L, 0L} : results.get(0);

        long total         = toLong(row[0]);
        long active        = toLong(row[1]);
        long onLeave       = toLong(row[2]);
        long newThisMonth  = toLong(row[3]);
        long newLastMonth  = toLong(row[4]);

        // totalTrend: so sánh NV mới tháng này vs tháng trước
        String totalTrend = formatTrend(newThisMonth, newLastMonth);

        // activeRate: % NV đang làm trên tổng
        String activeRate = total > 0
                ? Math.round((double) active / total * 100) + "%"
                : "0%";

        return EmployeeStatisticsResponse.builder()
                .totalEmployees      (total)
                .activeEmployees     (active)
                .onLeaveEmployees    (onLeave)
                .newEmployeesThisMonth(newThisMonth)
                .totalTrend          (totalTrend)
                .activeRate          (activeRate)
                .build();
    }

    /**
     * Tính xu hướng tăng trưởng: ((thisMonth - lastMonth) / max(lastMonth, 1)) * 100
     * Kết quả: "+25%", "-10%", "0%"
     */
    private String formatTrend(long thisMonth, long lastMonth) {
        double base   = Math.max(lastMonth, 1);
        long   pct    = Math.round((thisMonth - lastMonth) / base * 100);
        return (pct >= 0 ? "+" : "") + pct + "%";
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Long   l) return l;
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIST ALL  GET /v1/employees/list-all  (no pagination, for dropdown)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listAllActive() {
        return userRepository.findAllByStatusAndDeletedAtIsNull(UserStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ASSIGN SALARY LEVEL  PATCH /v1/employees/{id}/salary-level
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EmployeeResponse assignSalaryLevel(Long id, Long salaryLevelId) {
        User user = findActiveUser(id);
        SalaryLevel newLevel = salaryLevelRepository.findByIdAndDeletedAtIsNull(salaryLevelId)
                .orElseThrow(() -> new NotFoundException("SALARY_LEVEL_NOT_FOUND"));
        user.setSalaryLevel(newLevel);
        userRepository.save(user);
        log.info("Gán cấp bậc lương: employee={} → {}", id, newLevel.getName());
        return toResponse(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SALARY HISTORY  GET /v1/employees/{id}/salary-history
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SalaryHistoryResponse> getSalaryHistory(Long employeeId) {
        if (!userRepository.existsById(employeeId)) {
            throw new NotFoundException("EMPLOYEE_NOT_FOUND");
        }
        return historyRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private User findActiveUser(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("EMPLOYEE_NOT_FOUND"));
    }

    private SalaryLevel resolveSalaryLevel(Long salaryLevelId) {
        if (salaryLevelId != null) {
            return salaryLevelRepository.findByIdAndDeletedAtIsNull(salaryLevelId)
                    .orElseThrow(() -> new NotFoundException("SALARY_LEVEL_NOT_FOUND"));
        }
        // Mặc định gán Cấp 1
        return salaryLevelRepository.findByNameAndDeletedAtIsNull("Cấp 1")
                .orElse(null);
    }

    private User resolveCurrentUser() {
        var detail = AuthUtils.getUserDetail();
        if (detail == null || detail.getId() == null) {
            throw new BadRequestException("Không xác định được người dùng hiện tại");
        }
        return userRepository.findByIdAndDeletedAtIsNull(detail.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user hiện tại"));
    }

    /**
     * Hủy tất cả ca SCHEDULED trong tương lai của nhân viên.
     * @return số ca đã hủy
     */
    private int cancelFutureShifts(Long employeeId) {
        List<Shift> futureShifts = shiftRepository
                .findFutureScheduledByEmployee(employeeId, LocalDate.now());
        futureShifts.forEach(s -> s.setStatus(ShiftStatus.CANCELLED));
        if (!futureShifts.isEmpty()) {
            shiftRepository.saveAll(futureShifts);
        }
        return futureShifts.size();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        String[] formats = {"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy"};
        for (String fmt : formats) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(fmt));
            } catch (DateTimeParseException ignored) {}
        }
        throw new BadRequestException("Định dạng ngày không hợp lệ: " + dateStr);
    }

    public EmployeeResponse toResponse(User user) {
        return EmployeeResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhoneNumber())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .area(user.getArea())
                .skills(user.getSkills())
                .status(user.getStatus())
                .role(user.getRole() != null ? user.getRole().getSlug() : null)
                .isFirstLogin(user.isFirstLogin())
                .hiredDate(user.getStartDate())
                .salaryLevelId(user.getSalaryLevel() != null ? user.getSalaryLevel().getId() : null)
                .salaryLevelName(user.getSalaryLevel() != null ? user.getSalaryLevel().getName() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private SalaryHistoryResponse toHistoryResponse(SalaryLevelHistory h) {
        return SalaryHistoryResponse.builder()
                .id(h.getId())
                .employeeId(h.getEmployee().getId())
                .employeeName(h.getEmployee().getFullName())
                .oldLevelId(h.getOldLevel() != null ? h.getOldLevel().getId() : null)
                .oldLevelName(h.getOldLevel() != null ? h.getOldLevel().getName() : null)
                .newLevelId(h.getNewLevel().getId())
                .newLevelName(h.getNewLevel().getName())
                .effectiveDate(h.getEffectiveDate())
                .reason(h.getReason())
                .changedById(h.getChangedBy().getId())
                .changedByName(h.getChangedBy().getFullName())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
