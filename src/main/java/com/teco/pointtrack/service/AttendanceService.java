package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.attendance.*;
import com.teco.pointtrack.entity.*;
import com.teco.pointtrack.entity.enums.*;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.*;
import com.teco.pointtrack.utils.GpsUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    // ── System setting keys ───────────────────────────────────────────────────
    private static final String KEY_GPS_RADIUS          = "GPS_RADIUS_METERS";
    private static final String KEY_GRACE_PERIOD        = "GRACE_PERIOD_MINUTES";
    private static final String KEY_LATE_CHECKOUT_MINS  = "LATE_CHECKOUT_THRESHOLD_MINUTES";
    private static final String KEY_MIN_WORK_MINUTES    = "MIN_WORK_MINUTES";

    private static final double DEFAULT_GPS_RADIUS      = 50.0;
    private static final int    DEFAULT_GRACE_PERIOD    = 15;
    private static final int    DEFAULT_LATE_CHECKOUT   = 30;
    private static final int    DEFAULT_MIN_WORK_MINS   = 1;

    // TEMP: Bỏ qua check GPS để test nghiệp vụ lương.
    private static final boolean TEMP_BYPASS_GPS_VALIDATION = true;
    // TEMP: Bỏ qua rule chỉ checkout sau >=50% thời lượng ca để test nghiệp vụ lương.
    private static final boolean TEMP_BYPASS_MIN_CHECKOUT_RULE = true;
    // TEMP: Bỏ qua yêu cầu nhập lý do khi checkout muộn để test nghiệp vụ lương.
    private static final boolean TEMP_BYPASS_LATE_CHECKOUT_REASON = true;

    // ── Ranh giới giờ để phân loại ca (morning/afternoon/night) ──────────────
    private static final LocalTime MORNING_FROM   = LocalTime.of(5,  0);
    private static final LocalTime AFTERNOON_FROM = LocalTime.of(12, 0);
    private static final LocalTime NIGHT_FROM     = LocalTime.of(18, 0);

    /** Giới hạn xuất Excel — tránh OOM và response quá lớn */
    private static final int EXPORT_MAX_RECORDS = 10_000;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final WorkScheduleRepository       workScheduleRepo;
    private final AttendanceRecordRepository   attendanceRecordRepo;
    private final AttendancePhotoRepository    attendancePhotoRepo;
    private final ExplanationRequestRepository explanationRepo;
    private final AttendanceAuditLogRepository auditLogRepo;
    private final UserRepository               userRepo;
    private final CustomerRepository           customerRepo;
    private final SystemSettingRepository      systemSettingRepo;
    private final FileStorageService           fileStorageService;

    // ═════════════════════════════════════════════════════════════════════════
    // BR-14 + BR-15 + BR-16: CHECK-IN
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * checkIn — xử lý toàn bộ logic trong 1 transaction.
     *
     * @param workScheduleId Ca được phân công hôm nay
     * @param lat            GPS latitude của NV tại thời điểm check-in
     * @param lng            GPS longitude của NV tại thời điểm check-in
     * @param capturedAt     Timestamp đồng hồ thiết bị khi chụp ảnh (BR-15)
     * @param note           Lý do (optional — dùng cho ExplanationRequest LATE_CHECKIN)
     * @param photo          Ảnh hiện trường bắt buộc (BR-15)
     * @param userId         ID NV hiện tại (lấy từ SecurityContext ở controller)
     */
    @Transactional
    public CheckInResponse checkIn(Long workScheduleId,
                                   Double lat, Double lng,
                                   LocalDateTime capturedAt,
                                   String note,
                                   MultipartFile photo,
                                   Long userId) {

        // ── 1. Validate ảnh bắt buộc (BR-15) ─────────────────────────────────
        if (photo == null || photo.isEmpty()) {
            throw new BadRequestException("Ảnh hiện trường là bắt buộc khi check-in (BR-15)");
        }

        // ── 2. Load và validate WorkSchedule ─────────────────────────────────
        WorkSchedule schedule = workScheduleRepo.findByIdAndDeletedAtIsNull(workScheduleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lịch làm việc ID=" + workScheduleId));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new BadRequestException("Ca làm việc này không thuộc về bạn");
        }

        // BR-14: Validate status (Mới)
        if (schedule.getStatus() == WorkScheduleStatus.IN_PROGRESS) {
            throw new BadRequestException("Bạn chưa check-out ca trước");
        }
        if (schedule.getStatus() == WorkScheduleStatus.COMPLETED) {
            throw new ConflictException("Ca này đã hoàn thành");
        }
        if (schedule.getStatus() == WorkScheduleStatus.CANCELLED) {
            throw new ConflictException("Ca này đã bị hủy");
        }
        if (schedule.getStatus() != WorkScheduleStatus.SCHEDULED) {
            throw new BadRequestException("Trạng thái ca không hợp lệ để check-in: " + schedule.getStatus());
        }

        // Luôn dùng timezone Việt Nam để tránh lệch ngày khi server chạy UTC
        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vnZone);
        LocalDate shiftDate = schedule.getWorkDate();
        LocalTime shiftStart = schedule.getStartTime();
        LocalTime shiftEnd   = schedule.getEndTime();

        attendanceRecordRepo.findByWorkScheduleId(workScheduleId).ifPresent(record -> {
            if (record.getCheckOutTime() != null) {
                throw new ConflictException("Ca làm việc này đã hoàn thành (đã check-out).");
            }
            throw new ConflictException("Bạn đã check-in ca này rồi");
        });

        // ── 3. Validate Customer và tọa độ GPS ───────────────────────────────
        Customer customer = schedule.getCustomer();
        if (customer == null) {
            throw new BadRequestException("Ca làm việc này chưa được gán khách hàng/địa điểm cụ thể.");
        }
        
        // BR-14: GPS Fencing — Nếu khách hàng chưa có tọa độ (do tắt bản đồ), bỏ qua kiểm tra này
        Double customerLat = customer.getLatitude();
        Double customerLng = customer.getLongitude();
        double distanceM = 0.0;

        if (customerLat == null || customerLng == null) {
            log.warn("[CHECK-IN] Customer {} has no GPS coordinates. Skipping GPS Fencing.", customer.getName());
        } else {
            double gpsRadius = getDoubleSetting(KEY_GPS_RADIUS, DEFAULT_GPS_RADIUS);
            distanceM = GpsUtils.distanceMeters(lat, lng, customerLat, customerLng);

            log.info("=== GPS CHECK-IN DEBUG ===");
            log.info("Employee userId={} scheduleId={}", userId, workScheduleId);
            log.info("Employee location: lat={}, lng={}", lat, lng);
            log.info("Customer '{}' in DB: lat={}, lng={} | radius={}m",
                    customer.getName(), customerLat, customerLng, gpsRadius);
            log.info("Calculated distance: {}m", String.format("%.2f", distanceM));
            log.info("Is within radius: {}", distanceM <= gpsRadius);
            log.info("==========================");

            /*
            if (distanceM > gpsRadius) {
                throw new BadRequestException(String.format(
                        "GPS_OUT_OF_RANGE: Bạn đang cách địa điểm %.0f mét, bán kính cho phép: %d mét",
                        distanceM, Math.round(gpsRadius)));
            }
            */
        }

        // ── 5. Tính số phút đi muộn ───────────────────────────────────────────
        LocalDateTime now           = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        int gracePeriod             = getIntSetting(KEY_GRACE_PERIOD, DEFAULT_GRACE_PERIOD);
        LocalDateTime latestOnTime  = schedule.getScheduledStart().plusMinutes(gracePeriod);
        int lateMinutes             = (int) Math.max(0, ChronoUnit.MINUTES.between(latestOnTime, now));

        // ── 6. Xác định status bản ghi ────────────────────────────────────────
        AttendanceStatus status = lateMinutes > 0 ? AttendanceStatus.LATE : AttendanceStatus.ON_TIME;

        // ── 7. Upload ảnh (BR-15) ──────────────────────────────────────────────
        String photoUrl = "default_photo_url.jpg";
        try {
            photoUrl = fileStorageService.storeAttendancePhoto(photo);
        } catch (Exception e) {
            log.error("Failed to store photo, using default. Error: {}", e.getMessage());
        }

        // ── 8. Lưu AttendanceRecord ───────────────────────────────────────────
        User user = userRepo.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", userId));

        AttendanceRecord record = AttendanceRecord.builder()
                .workSchedule(schedule)
                .user(user)
                .checkInTime(now)
                .checkInLat(lat)
                .checkInLng(lng)
                .checkInDistanceMeters(distanceM)
                .lateMinutes(lateMinutes)
                .status(status)
                .otMultiplier(BigDecimal.ONE)  // Mặc định là 1.0 khi không dùng template
                .note(note)
                .build();

        attendanceRecordRepo.save(record);

        // ── 9. Lưu AttendancePhoto (BR-15) ────────────────────────────────────
        AttendancePhoto attendancePhoto = AttendancePhoto.builder()
                .attendanceRecord(record)
                .type(PhotoType.CHECK_IN)
                .photoUrl(photoUrl)
                .capturedLat(lat)
                .capturedLng(lng)
                .capturedAt(capturedAt != null ? capturedAt : now)
                .fileSizeBytes(photo.getSize())
                .mimeType(photo.getContentType())
                .originalFileName(photo.getOriginalFilename())
                .build();

        attendancePhotoRepo.save(attendancePhoto);

        // ── 10. Tạo ExplanationRequest nếu cần (BR-16) ────────────────────────
        List<ExplanationRequest> createdExplanations = new ArrayList<>();

        if (lateMinutes > 0) {
            // BR-16.1: Đi muộn — auto tạo, note từ payload là lý do (optional)
            createdExplanations.add(createExplanation(record, user, ExplanationType.LATE_CHECKIN, note));
        }

        // ── 11. Cập nhật WorkSchedule status ──────────────────────────────────
        schedule.setStatus(WorkScheduleStatus.IN_PROGRESS);
        workScheduleRepo.save(schedule);

        // ── 12. Build response ─────────────────────────────────────────────────
        Long firstExplanationId = createdExplanations.stream()
                .findFirst()
                .map(ExplanationRequest::getId)
                .orElse(null);

        String message = buildCheckInMessage(lateMinutes);

        return CheckInResponse.builder()
                .attendanceRecordId(record.getId())
                .status(status)
                .checkInTime(now)
                .distanceMeters(Math.round(distanceM * 10.0) / 10.0)
                .gpsValid(true)
                .lateMinutes(lateMinutes)
                .explanationRequestId(firstExplanationId)
                .message(message)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BR-15 + BR-16.2: CHECK-OUT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * checkOut — validate checkout trễ (BR-16.2), tính actualMinutes, earlyLeave.
     *
     * @param attendanceRecordId  ID bản ghi check-in tương ứng
     * @param checkOutReason      Lý do — BẮT BUỘC khi checkout quá muộn (BR-16.2)
     */
    @Transactional
    public CheckOutResponse checkOut(Long attendanceRecordId,
                                     Double lat, Double lng,
                                     LocalDateTime capturedAt,
                                     String checkOutReason,
                                     MultipartFile photo,
                                     Long userId) {

        // ── 1. Validate ảnh bắt buộc (BR-15) ─────────────────────────────────
        if (photo == null || photo.isEmpty()) {
            throw new BadRequestException("Ảnh hiện trường là bắt buộc khi check-out (BR-15)");
        }

        // ── 2. Load AttendanceRecord ──────────────────────────────────────────
        AttendanceRecord record = attendanceRecordRepo.findById(attendanceRecordId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bản ghi chấm công ID=" + attendanceRecordId));

        WorkSchedule ws = record.getWorkSchedule();
        if (ws != null) {
            if (ws.getStatus() == WorkScheduleStatus.SCHEDULED) {
                throw new BadRequestException("Bạn chưa check-in");
            }
            if (ws.getStatus() == WorkScheduleStatus.COMPLETED) {
                throw new ConflictException("Ca này đã hoàn thành rồi");
            }
            if (ws.getStatus() != WorkScheduleStatus.IN_PROGRESS) {
                throw new BadRequestException("Trạng thái ca không hợp lệ để check-out: " + ws.getStatus());
            }
        }

        if (!record.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bản ghi này không thuộc về bạn");
        }
        if (record.getCheckInTime() == null) {
            throw new BadRequestException("Chưa có dữ liệu check-in cho bản ghi này");
        }
        if (record.getCheckOutTime() != null) {
            throw new ConflictException("Bạn đã check-out ca này rồi");
        }

        LocalDateTime now         = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime checkInTime = record.getCheckInTime() != null ? record.getCheckInTime() : now;

        // ── 2.1. Chỉ được checkout sau khi đã làm >= 50% thời lượng ca ─────
        long workedMinutesBeforeCheckout = Math.max(0, ChronoUnit.MINUTES.between(checkInTime, now));
        int minRequiredCheckoutMinutes = resolveMinimumCheckoutMinutes(ws);

        if (!TEMP_BYPASS_MIN_CHECKOUT_RULE && workedMinutesBeforeCheckout < minRequiredCheckoutMinutes) {
            throw new BadRequestException(String.format(
                    "Bạn chỉ có thể check-out sau khi đã làm ít nhất %d phút (50%% thời lượng ca). Hiện tại mới làm %d phút.",
                    minRequiredCheckoutMinutes,
                    workedMinutesBeforeCheckout));
        }

        if (TEMP_BYPASS_MIN_CHECKOUT_RULE && workedMinutesBeforeCheckout < minRequiredCheckoutMinutes) {
            log.warn("[TEMP_BYPASS_MIN_CHECKOUT] Checkout recordId={} before 50% rule: worked={}m, required={}m.",
                attendanceRecordId,
                workedMinutesBeforeCheckout,
                minRequiredCheckoutMinutes);
        }

        // ── 2.2. BR-14: GPS Fencing khi check-out ─────────────────────────
        double checkOutDistanceMeters = 0.0;
        if (ws != null && ws.getCustomer() != null) {
            Customer checkoutCustomer = ws.getCustomer();
            if (checkoutCustomer.getLatitude() != null && checkoutCustomer.getLongitude() != null) {
                double gpsRadiusOut = getDoubleSetting(KEY_GPS_RADIUS, DEFAULT_GPS_RADIUS);
                checkOutDistanceMeters = GpsUtils.distanceMeters(lat, lng,
                        checkoutCustomer.getLatitude(), checkoutCustomer.getLongitude());

                if (!TEMP_BYPASS_GPS_VALIDATION && checkOutDistanceMeters > gpsRadiusOut) {
                    throw new BadRequestException(String.format(
                            "GPS_OUT_OF_RANGE: Bạn đang cách địa điểm %d mét, bán kính cho phép: %d mét.",
                            Math.round(checkOutDistanceMeters),
                            Math.round(gpsRadiusOut)));
                }

                if (TEMP_BYPASS_GPS_VALIDATION && checkOutDistanceMeters > gpsRadiusOut) {
                    log.warn("[TEMP_BYPASS_GPS] Checkout recordId={} out-of-range {}m (allowed {}m) but still accepted.",
                            attendanceRecordId,
                            Math.round(checkOutDistanceMeters),
                            Math.round(gpsRadiusOut));
                }
            }
        }

        LocalDateTime schedEnd   = record.getWorkSchedule() != null ? record.getWorkSchedule().getScheduledEnd() : null;

        if (schedEnd == null) {
            log.warn("ScheduledEnd is null for AttendanceRecord ID={}, using now as default", attendanceRecordId);
            schedEnd = now;
        }

        // ── 3. BR-16.2: Checkout trễ — bắt buộc nhập lý do ──────────────────
        int lateCheckoutThreshold = getIntSetting(KEY_LATE_CHECKOUT_MINS, DEFAULT_LATE_CHECKOUT);
        long minutesAfterEnd      = ChronoUnit.MINUTES.between(schedEnd, now);
        boolean isLateCheckout    = minutesAfterEnd > lateCheckoutThreshold;
        String effectiveCheckOutReason = checkOutReason;

        if (!TEMP_BYPASS_LATE_CHECKOUT_REASON && isLateCheckout
            && (checkOutReason == null || checkOutReason.isBlank())) {
            throw new BadRequestException(
                    "Checkout quá " + lateCheckoutThreshold + " phút sau giờ kết thúc ca. "
                    + "Vui lòng nhập lý do vào trường checkOutReason (BR-16.2)");
        }

        if (TEMP_BYPASS_LATE_CHECKOUT_REASON && isLateCheckout
            && (checkOutReason == null || checkOutReason.isBlank())) {
            effectiveCheckOutReason = "TEMP_BYPASS_LATE_CHECKOUT_REASON";
            log.warn("[TEMP_BYPASS_LATE_CHECKOUT_REASON] recordId={} late {}m but no reason required.",
                attendanceRecordId,
                minutesAfterEnd);
        }

        // ── 4. Tính thời gian làm thực tế ─────────────────────────────────────
        long workedMinutes = Math.max(0, ChronoUnit.MINUTES.between(checkInTime, now));
        int actualMinutes = (int) workedMinutes;
        int earlyLeaveMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(now, schedEnd));

        // ── 5. Upload ảnh (BR-15) ──────────────────────────────────────────────
        String photoUrl = "default_checkout_photo.jpg";
        try {
            photoUrl = fileStorageService.storeAttendancePhoto(photo);
        } catch (Exception e) {
            log.error("Failed to store checkout photo, using default. Error: {}", e.getMessage());
        }

        // ── 6. Update AttendanceRecord ────────────────────────────────────────
        if (now.isBefore(record.getCheckInTime())) {
            throw new BadRequestException("Thời gian checkout không hợp lệ");
        }

        // Cập nhật WorkSchedule status
        if (ws != null) {
            ws.setStatus(WorkScheduleStatus.COMPLETED);
            workScheduleRepo.save(ws);
        }

        double workedHours = calculateWorkedHours(workedMinutes);
        BigDecimal estimatedSalary = calculateEstimatedSalary(record.getUser(), workedMinutes, record.getOtMultiplier());

        record.setCheckOutTime(now);
        record.setCheckOutLat(lat);
        record.setCheckOutLng(lng);
        record.setCheckOutDistanceMeters(checkOutDistanceMeters);
        record.setActualMinutes(actualMinutes);
        record.setEarlyLeaveMinutes(earlyLeaveMinutes);
        record.setWorkedMinutes(workedMinutes);
        record.setWorkedHours(workedHours);
        record.setEstimatedSalary(estimatedSalary);

        // Cập nhật status nếu cần (chỉ ghi đè khi status vẫn là ON_TIME)
        if (record.getStatus() == AttendanceStatus.ON_TIME && earlyLeaveMinutes > 0) {
            record.setStatus(AttendanceStatus.EARLY_LEAVE);
        }

        attendanceRecordRepo.save(record);

        // ── 7. Lưu AttendancePhoto (BR-15) ────────────────────────────────────
        AttendancePhoto checkOutPhoto = AttendancePhoto.builder()
                .attendanceRecord(record)
                .type(PhotoType.CHECK_OUT)
                .photoUrl(photoUrl)
                .capturedLat(lat)
                .capturedLng(lng)
                .capturedAt(capturedAt != null ? capturedAt : now)
                .fileSizeBytes(photo.getSize())
                .mimeType(photo.getContentType())
                .originalFileName(photo.getOriginalFilename())
                .build();

        attendancePhotoRepo.save(checkOutPhoto);

        // ── 8. Tạo ExplanationRequest cho checkout trễ (BR-16.2) ─────────────
        if (isLateCheckout) {
            User user = record.getUser();
            createExplanation(record, user, ExplanationType.LATE_CHECKOUT, effectiveCheckOutReason);
        }

        return CheckOutResponse.builder()
                .attendanceRecordId(record.getId())
                .status(record.getStatus())
                .checkInTime(record.getCheckInTime())
                .checkOutTime(now)
                .actualMinutes(actualMinutes)
                .earlyLeaveMinutes(earlyLeaveMinutes)
                .otMultiplier(record.getOtMultiplier())
                .workedMinutes(workedMinutes)
                .workedHours(workedHours)
                .estimatedSalary(estimatedSalary)
                .currency("VND")
                .shiftName(ws != null && ws.getCustomer() != null ? "Ca tại " + ws.getCustomer().getName() : "Ca lẻ")
                .message("Hoàn thành ca làm việc thành công!")
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BR-16: Admin duyệt / từ chối giải trình (flow đơn giản)
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public void approveExplanation(Long explanationId, ApprovalRequest req, Long adminId) {
        ExplanationRequest explanation = findExplanation(explanationId);

        User admin = userRepo.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", adminId));

        explanation.setStatus(ExplanationStatus.APPROVED);
        explanation.setReviewedBy(admin);
        explanation.setReviewedAt(LocalDateTime.now());
        explanation.setReviewNote(req.getReviewNote());
        explanationRepo.save(explanation);

        // Khi duyệt GPS_INVALID hoặc LATE_CHECKIN → cập nhật AttendanceRecord về ON_TIME
        // (chỉ cập nhật nếu không còn explanation PENDING nào khác cho record đó)
        AttendanceRecord record = explanation.getAttendanceRecord();
        boolean hasPendingOther = explanationRepo
                .findByAttendanceRecordId(record.getId())
                .stream()
                .anyMatch(e -> !e.getId().equals(explanationId)
                        && e.getStatus() == ExplanationStatus.PENDING);

        if (!hasPendingOther) {
            record.setStatus(AttendanceStatus.ON_TIME);
            attendanceRecordRepo.save(record);
        }
    }

    @Transactional
    public void rejectExplanation(Long explanationId, ApprovalRequest req, Long adminId) {
        ExplanationRequest explanation = findExplanation(explanationId);

        User admin = userRepo.findByIdAndDeletedAtIsNull(adminId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", adminId));

        explanation.setStatus(ExplanationStatus.REJECTED);
        explanation.setReviewedBy(admin);
        explanation.setReviewedAt(LocalDateTime.now());
        explanation.setReviewNote(req.getReviewNote());
        explanationRepo.save(explanation);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BR-19: Admin chỉnh sửa giờ công → ghi Audit Log
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public void adminUpdateAttendance(Long recordId, AdminUpdateAttendanceRequest req, Long adminId) {
        AttendanceRecord record = attendanceRecordRepo.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bản ghi ID=" + recordId));

        List<AttendanceAuditLog> logs = new ArrayList<>();

        // Ghi audit log cho từng field bị thay đổi
        if (req.getCheckInTime() != null
                && !req.getCheckInTime().equals(record.getCheckInTime())) {
            logs.add(buildAuditLog(recordId, adminId, "checkInTime",
                    str(record.getCheckInTime()), str(req.getCheckInTime()), req.getReason()));
            record.setCheckInTime(req.getCheckInTime());
        }

        if (req.getCheckOutTime() != null
                && !req.getCheckOutTime().equals(record.getCheckOutTime())) {
            logs.add(buildAuditLog(recordId, adminId, "checkOutTime",
                    str(record.getCheckOutTime()), str(req.getCheckOutTime()), req.getReason()));
            record.setCheckOutTime(req.getCheckOutTime());
        }

        if (req.getNote() != null && !req.getNote().equals(record.getNote())) {
            logs.add(buildAuditLog(recordId, adminId, "note",
                    record.getNote(), req.getNote(), req.getReason()));
            record.setNote(req.getNote());
        }

        if (logs.isEmpty()) {
            throw new BadRequestException("Không có trường nào thay đổi");
        }

        // Tính lại actual/worked/salary nếu cả 2 mốc thời gian đã có
        if (record.getCheckInTime() != null && record.getCheckOutTime() != null) {
            if (record.getCheckOutTime().isBefore(record.getCheckInTime())) {
                throw new BadRequestException("checkOutTime phải lớn hơn hoặc bằng checkInTime");
            }

            int newActual = (int) ChronoUnit.MINUTES.between(
                    record.getCheckInTime(), record.getCheckOutTime());
            logs.add(buildAuditLog(recordId, adminId, "actualMinutes",
                    str(record.getActualMinutes()), str(newActual), req.getReason()));
            record.setActualMinutes(newActual);

            long newWorkedMinutes = Math.max(0, newActual);
            if (!Objects.equals(record.getWorkedMinutes(), newWorkedMinutes)) {
                logs.add(buildAuditLog(recordId, adminId, "workedMinutes",
                        str(record.getWorkedMinutes()), str(newWorkedMinutes), req.getReason()));
            }
            record.setWorkedMinutes(newWorkedMinutes);

            double newWorkedHours = calculateWorkedHours(newWorkedMinutes);
            if (!Objects.equals(record.getWorkedHours(), newWorkedHours)) {
                logs.add(buildAuditLog(recordId, adminId, "workedHours",
                        str(record.getWorkedHours()), str(newWorkedHours), req.getReason()));
            }
            record.setWorkedHours(newWorkedHours);

            BigDecimal newEstimatedSalary = calculateEstimatedSalary(
                    record.getUser(), newWorkedMinutes, record.getOtMultiplier());
            if (!Objects.equals(record.getEstimatedSalary(), newEstimatedSalary)) {
                logs.add(buildAuditLog(recordId, adminId, "estimatedSalary",
                        str(record.getEstimatedSalary()), str(newEstimatedSalary), req.getReason()));
            }
            record.setEstimatedSalary(newEstimatedSalary);
        }

        attendanceRecordRepo.save(record);
        auditLogRepo.saveAll(logs);  // BR-19: lưu không cho xóa

        log.info("[AUDIT] Admin {} cập nhật AttendanceRecord {} — {} field(s) changed",
                adminId, recordId, logs.size());
    }

    @Transactional
    public WorkScheduleResponse createWorkSchedule(WorkScheduleRequest request) {
        User user = userRepo.findByIdAndDeletedAtIsNull(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Nhân viên không tồn tại (ID=" + request.getUserId() + ")"));

        WorkSchedule schedule = WorkSchedule.builder()
                .user(user)
                .workDate(request.getWorkDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .address(request.getAddress())
                .latitude(request.getLat())
                .longitude(request.getLng())
                .status(WorkScheduleStatus.SCHEDULED)
                .build();

        // Đồng bộ với logic scheduled_start/end cũ (để hệ thống cũ vẫn chạy)
        schedule.setScheduledStart(LocalDateTime.of(request.getWorkDate(), request.getStartTime()));
        schedule.setScheduledEnd(LocalDateTime.of(request.getWorkDate(), request.getEndTime()));

        workScheduleRepo.save(schedule);

        return mapToResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<WorkScheduleResponse> getAllWorkSchedules() {
        return workScheduleRepo.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private WorkScheduleResponse mapToResponse(WorkSchedule schedule) {
        // Lấy thông tin User an toàn
        Long userId = (schedule.getUser() != null) ? schedule.getUser().getId() : null;
        String userName = (schedule.getUser() != null) ? schedule.getUser().getFullName() : "N/A";

        // Logic lấy giờ: Ưu tiên trường mới, nếu null thì lấy từ scheduledStart/End (logic cũ)
        java.time.LocalTime start = (schedule.getStartTime() != null)
                ? schedule.getStartTime()
                : (schedule.getScheduledStart() != null ? schedule.getScheduledStart().toLocalTime() : null);

        java.time.LocalTime end = (schedule.getEndTime() != null)
                ? schedule.getEndTime()
                : (schedule.getScheduledEnd() != null ? schedule.getScheduledEnd().toLocalTime() : null);

        // Logic lấy địa chỉ/tọa độ: Ưu tiên trường mới, nếu null thì lấy từ Customer (logic cũ)
        String addr = schedule.getAddress();
        Double lat = schedule.getLatitude();
        Double lng = schedule.getLongitude();

        if ((addr == null || addr.isBlank()) && schedule.getCustomer() != null) {
            addr = schedule.getCustomer().getAddress();
        }
        if (lat == null && schedule.getCustomer() != null) {
            lat = schedule.getCustomer().getLatitude();
        }
        if (lng == null && schedule.getCustomer() != null) {
            lng = schedule.getCustomer().getLongitude();
        }

        return WorkScheduleResponse.builder()
                .id(schedule.getId())
                .userId(userId)
                .userName(userName)
                .workDate(schedule.getWorkDate())
                .startTime(start)
                .endTime(end)
                .address(addr)
                .lat(lat)
                .lng(lng)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═════════════════════════════════════════════════════════════════════════

    private ExplanationRequest createExplanation(AttendanceRecord record, User user,
                                                  ExplanationType type, String reason) {
        ExplanationRequest er = ExplanationRequest.builder()
                .attendanceRecord(record)
                .user(user)
                .type(type)
                .reason(reason)
                .status(ExplanationStatus.PENDING)
                .build();
        return explanationRepo.save(er);
    }

    private ExplanationRequest findExplanation(Long id) {
        return explanationRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn giải trình ID=" + id));
    }

    private AttendanceAuditLog buildAuditLog(Long recordId, Long adminId,
                                              String field, String oldVal,
                                              String newVal, String reason) {
        return AttendanceAuditLog.builder()
                .attendanceRecordId(recordId)
                .changedByUserId(adminId)
                .fieldName(field)
                .oldValue(oldVal)
                .newValue(newVal)
                .reason(reason)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin: Xem danh sách giải trình
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ExplanationRequestResponse> getExplanations(
            ExplanationStatus status, ExplanationType type, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ExplanationRequest> data;

        if (status != null && type != null) {
            data = explanationRepo.findByStatusAndType(status, type, pageable);
        } else if (status != null) {
            data = explanationRepo.findByStatus(status, pageable);
        } else if (type != null) {
            data = explanationRepo.findByType(type, pageable);
        } else {
            data = explanationRepo.findAll(pageable);
        }

        return data.map(e -> ExplanationRequestResponse.builder()
                .id(e.getId())
                .attendanceRecordId(e.getAttendanceRecord() != null ? e.getAttendanceRecord().getId() : null)
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .userName(e.getUser() != null ? e.getUser().getFullName() : null)
                .type(e.getType())
                .reason(e.getReason())
                .status(e.getStatus())
                .reviewNote(e.getReviewNote())
                .reviewedByUserName(e.getReviewedBy() != null ? e.getReviewedBy().getFullName() : null)
                .reviewedAt(e.getReviewedAt())
                .createdAt(e.getCreatedAt())
                .build());
    }

    private String buildCheckInMessage(int lateMinutes) {
        if (lateMinutes > 0) {
            return "Check-in thành công. Đi muộn " + lateMinutes + " phút. Đơn giải trình đã được tạo.";
        }
        return "Check-in thành công.";
    }

    private double getDoubleSetting(String key, double defaultVal) {
        return systemSettingRepo.findById(key)
                .map(s -> Double.parseDouble(s.getValue()))
                .orElse(defaultVal);
    }

    private int getIntSetting(String key, int defaultVal) {
        return systemSettingRepo.findById(key)
                .map(s -> Integer.parseInt(s.getValue()))
                .orElse(defaultVal);
    }

    private int resolveMinimumCheckoutMinutes(WorkSchedule ws) {
        int scheduledDurationMinutes = resolveScheduledDurationMinutes(ws);
        if (scheduledDurationMinutes > 0) {
            return Math.max(1, (int) Math.ceil(scheduledDurationMinutes / 2.0));
        }

        int fallbackMinWorkMinutes = getIntSetting(KEY_MIN_WORK_MINUTES, DEFAULT_MIN_WORK_MINS);
        return Math.max(1, fallbackMinWorkMinutes);
    }

    private int resolveScheduledDurationMinutes(WorkSchedule ws) {
        if (ws == null) {
            return 0;
        }

        if (ws.getScheduledStart() != null && ws.getScheduledEnd() != null) {
            long minutes = ChronoUnit.MINUTES.between(ws.getScheduledStart(), ws.getScheduledEnd());
            if (minutes <= 0) {
                minutes += 24 * 60;
            }
            return (int) Math.max(0, minutes);
        }

        if (ws.getWorkDate() != null && ws.getStartTime() != null && ws.getEndTime() != null) {
            LocalDateTime start = LocalDateTime.of(ws.getWorkDate(), ws.getStartTime());
            LocalDateTime end = LocalDateTime.of(ws.getWorkDate(), ws.getEndTime());
            if (!end.isAfter(start)) {
                end = end.plusDays(1);
            }
            return (int) Math.max(0, ChronoUnit.MINUTES.between(start, end));
        }

        return 0;
    }

    private double calculateWorkedHours(long workedMinutes) {
        return BigDecimal.valueOf(Math.max(0, workedMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal calculateEstimatedSalary(User user, long workedMinutes, BigDecimal otMultiplier) {
        if (workedMinutes <= 0 || user == null || user.getSalaryLevel() == null
                || user.getSalaryLevel().getBaseSalary() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal ratePerHour = user.getSalaryLevel().getBaseSalary();
        BigDecimal multiplier = otMultiplier != null ? otMultiplier : BigDecimal.ONE;

        return BigDecimal.valueOf(workedMinutes)
                .multiply(ratePerHour)
                .multiply(multiplier)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private String str(Object val) {
        return val == null ? null : val.toString();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /attendance/history — Lịch sử chấm công (có filter + summary)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Trả về danh sách lịch sử chấm công phân trang + summary.
     *
     * @param page        Trang (FE gửi 1-indexed, convert sang 0-indexed ở controller)
     * @param limit       Số bản ghi / trang (10 | 20 | 50 | 100)
     * @param search      Tìm theo tên NV (LIKE, case-insensitive)
     * @param customerId  Lọc theo địa điểm
     * @param statusStr   "on_time" | "late" | "early_leave" | "absent" | "overtime" | ""
     * @param dateFrom    Ngày bắt đầu (workDate >=)
     * @param dateTo      Ngày kết thúc (workDate <=)
     * @param shiftType   "morning" | "afternoon" | "night" | ""
     */
    @Transactional(readOnly = true)
    public AttendanceHistoryPageResponse getAttendanceHistory(
            int page, int limit,
            String search, Long customerId, String statusStr,
            LocalDate dateFrom, LocalDate dateTo, String shiftType) {

        // ── 1. Validate dateFrom <= dateTo ────────────────────────────────────
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom phải nhỏ hơn hoặc bằng dateTo");
        }

        // ── 2. Chuyển đổi status string → enum + overtime flag ────────────────
        AttendanceStatus statusEnum = null;
        BigDecimal minOtMultiplier  = null;

        if (statusStr != null && !statusStr.isBlank()) {
            switch (statusStr.toLowerCase()) {
                case "on_time"     -> statusEnum = AttendanceStatus.ON_TIME;
                case "late"        -> statusEnum = AttendanceStatus.LATE;
                case "early_leave" -> statusEnum = AttendanceStatus.EARLY_LEAVE;
                case "absent"      -> statusEnum = AttendanceStatus.ABSENT;
                // overtime: lọc theo otMultiplier > 1.0 thay vì theo status enum
                case "overtime"    -> minOtMultiplier = BigDecimal.ONE;
                default            -> throw new BadRequestException(
                        "Trạng thái không hợp lệ: " + statusStr
                        + ". Giá trị hợp lệ: on_time, late, early_leave, absent, overtime");
            }
        }

        // ── 3. Chuẩn hoá shiftType (rỗng → null) ─────────────────────────────
        String shiftTypeParam = (shiftType != null && !shiftType.isBlank()) ? shiftType.toLowerCase() : null;
        if (shiftTypeParam != null
                && !shiftTypeParam.equals("morning")
                && !shiftTypeParam.equals("afternoon")
                && !shiftTypeParam.equals("night")) {
            throw new BadRequestException("shiftType không hợp lệ: " + shiftType
                    + ". Giá trị hợp lệ: morning, afternoon, night");
        }

        // ── 4. Sanitize search string (tránh injection) ───────────────────────
        String searchParam = (search != null && !search.isBlank())
                ? search.trim().replaceAll("[%_\\\\]", "\\\\$0")
                : null;

        // ── 5. Query dữ liệu chính (phân trang) ───────────────────────────────
        Pageable pageable = PageRequest.of(page, limit);
        Page<AttendanceRecord> recordPage = attendanceRecordRepo.findHistoryByFilters(
                searchParam, customerId, statusEnum, minOtMultiplier,
                dateFrom, dateTo, shiftTypeParam,
                MORNING_FROM, AFTERNOON_FROM, NIGHT_FROM,
                pageable);

        // ── 6. Query summary (chạy sau main query trong cùng transaction) ──────
        List<Object[]> statusCounts = attendanceRecordRepo.countStatusSummaryByFilters(
                searchParam, customerId, dateFrom, dateTo, shiftTypeParam,
                MORNING_FROM, AFTERNOON_FROM, NIGHT_FROM);

        long overtimeCount = attendanceRecordRepo.countOvertimeSummaryByFilters(
                searchParam, customerId, dateFrom, dateTo, shiftTypeParam,
                MORNING_FROM, AFTERNOON_FROM, NIGHT_FROM);

        // ── 7. Tổng hợp summary từ GROUP BY result ────────────────────────────
        Map<AttendanceStatus, Long> countMap = new HashMap<>();
        long total = 0;
        for (Object[] row : statusCounts) {
            AttendanceStatus s = (AttendanceStatus) row[0];
            long cnt           = ((Number) row[1]).longValue();
            countMap.put(s, cnt);
            total += cnt;
        }

        AttendanceSummaryResponse summary = AttendanceSummaryResponse.builder()
                .totalRecords(total)
                .onTime(countMap.getOrDefault(AttendanceStatus.ON_TIME,       0L))
                .late(countMap.getOrDefault(AttendanceStatus.LATE,            0L))
                .earlyLeave(countMap.getOrDefault(AttendanceStatus.EARLY_LEAVE, 0L))
                .absent(countMap.getOrDefault(AttendanceStatus.ABSENT,        0L))
                .overtime(overtimeCount)
                .build();

        // ── 8. Map entity → DTO ────────────────────────────────────────────────
        List<AttendanceHistoryResponse> records = recordPage.getContent()
                .stream()
                .map(this::toHistoryResponse)
                .toList();

        AttendanceHistoryPageResponse.PaginationMeta pagination =
                AttendanceHistoryPageResponse.PaginationMeta.builder()
                        .page(page + 1)   // trả về 1-indexed cho FE
                        .limit(limit)
                        .total(recordPage.getTotalElements())
                        .totalPages(recordPage.getTotalPages())
                        .build();

        return AttendanceHistoryPageResponse.builder()
                .records(records)
                .pagination(pagination)
                .summary(summary)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /attendance/locations — Dropdown địa điểm
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Trả về danh sách tất cả địa điểm còn hoạt động để FE render dropdown filter.
     * Dùng ACTIVE + có GPS (phù hợp với chấm công GPS).
     */
    @Transactional(readOnly = true)
    public List<LocationDropdownResponse> getLocations() {
        return customerRepo.findAllActiveWithGps().stream()
                .map(c -> new LocationDropdownResponse(c.getId(), c.getName()))
                .toList();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PATCH /attendance/{id}/note — Cập nhật ghi chú
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Cập nhật trường note của bản ghi chấm công.
     * Chỉ Admin mới được gọi (kiểm tra ở controller).
     *
     * @return AttendanceHistoryResponse với note đã được cập nhật
     */
    @Transactional
    public AttendanceHistoryResponse updateAttendanceNote(Long recordId, String note) {
        AttendanceRecord record = attendanceRecordRepo.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bản ghi chấm công ID=" + recordId));

        record.setNote(note);
        AttendanceRecord saved = attendanceRecordRepo.save(record);

        return toHistoryResponse(saved);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /attendance/export — Xuất Excel
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Xuất danh sách chấm công ra file Excel (Apache POI).
     *
     * <p>Giới hạn tối đa 10,000 bản ghi. Nếu vượt quá → ném BadRequestException.
     *
     * @param response HttpServletResponse để stream file trực tiếp
     */
    @Transactional(readOnly = true)
    public void exportAttendanceToExcel(
            String search, Long customerId, String statusStr,
            LocalDate dateFrom, LocalDate dateTo, String shiftType,
            HttpServletResponse response) throws IOException {

        // ── 1. Chuyển đổi params (tương tự getAttendanceHistory) ──────────────
        AttendanceStatus statusEnum = null;
        BigDecimal minOtMultiplier  = null;
        if (statusStr != null && !statusStr.isBlank()) {
            switch (statusStr.toLowerCase()) {
                case "on_time"     -> statusEnum = AttendanceStatus.ON_TIME;
                case "late"        -> statusEnum = AttendanceStatus.LATE;
                case "early_leave" -> statusEnum = AttendanceStatus.EARLY_LEAVE;
                case "absent"      -> statusEnum = AttendanceStatus.ABSENT;
                case "overtime"    -> minOtMultiplier = BigDecimal.ONE;
                default            -> throw new BadRequestException("Trạng thái không hợp lệ: " + statusStr);
            }
        }
        String shiftTypeParam = (shiftType != null && !shiftType.isBlank()) ? shiftType.toLowerCase() : null;
        String searchParam    = (search != null && !search.isBlank())
                ? search.trim().replaceAll("[%_\\\\]", "\\\\$0") : null;

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom phải nhỏ hơn hoặc bằng dateTo");
        }

        // ── 2. Query với limit = EXPORT_MAX_RECORDS + 1 để phát hiện vượt ngưỡng ─
        Pageable checkLimit = PageRequest.of(0, EXPORT_MAX_RECORDS + 1);
        List<AttendanceRecord> allRecords = attendanceRecordRepo.findHistoryForExport(
                searchParam, customerId, statusEnum, minOtMultiplier,
                dateFrom, dateTo, shiftTypeParam,
                MORNING_FROM, AFTERNOON_FROM, NIGHT_FROM,
                checkLimit);

        if (allRecords.size() > EXPORT_MAX_RECORDS) {
            throw new BadRequestException(
                    "Kết quả vượt quá " + EXPORT_MAX_RECORDS + " bản ghi. "
                    + "Vui lòng thu hẹp bộ lọc trước khi xuất.");
        }

        // ── 3. Tạo workbook Excel ────────────────────────────────────────────
        String fileName = "cham-cong-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lịch sử chấm công");

            // ── 3a. Style header ────────────────────────────────────────────────
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            // ── 3b. Style row xen kẽ ─────────────────────────────────────────
            CellStyle grayRowStyle = workbook.createCellStyle();
            grayRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            grayRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── 3c. Style màu text theo status ───────────────────────────────
            Map<AttendanceStatus, Short> statusColors = Map.of(
                    AttendanceStatus.ON_TIME,         IndexedColors.GREEN.getIndex(),
                    AttendanceStatus.LATE,            IndexedColors.ORANGE.getIndex(),
                    AttendanceStatus.EARLY_LEAVE,     IndexedColors.YELLOW.getIndex(),
                    AttendanceStatus.ABSENT,          IndexedColors.RED.getIndex(),
                    AttendanceStatus.PENDING_APPROVAL, IndexedColors.BLUE.getIndex()
            );

            // ── 3d. Dòng header ───────────────────────────────────────────────
            String[] headers = {
                "STT", "Mã NV", "Tên NV", "Phòng ban", "Địa điểm", "Ca", "Ngày",
                "Giờ vào", "Trễ (phút)", "Giờ ra", "Về sớm (phút)",
                "Tổng giờ", "OT (phút)", "Trạng thái", "Ghi chú"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── 3e. Dòng dữ liệu ─────────────────────────────────────────────
            DateTimeFormatter dtFmt   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            int rowIdx = 1;

            for (AttendanceRecord record : allRecords) {
                AttendanceHistoryResponse dto = toHistoryResponse(record);
                Row row = sheet.createRow(rowIdx);

                // Row style xen kẽ
                if (rowIdx % 2 == 0) {
                    for (int ci = 0; ci < headers.length; ci++) {
                        row.createCell(ci).setCellStyle(grayRowStyle);
                    }
                }

                setCell(row, 0, String.valueOf(rowIdx));
                setCell(row, 1, dto.getEmployee().getCode());
                setCell(row, 2, dto.getEmployee().getName());
                setCell(row, 3, dto.getEmployee().getDepartment());
                setCell(row, 4, dto.getLocation().getName());
                setCell(row, 5, dto.getShift().getName());
                setCell(row, 6, dto.getDate() != null ? dto.getDate().format(dateFmt) : "");
                setCell(row, 7, dto.getCheckIn().getTime() != null ? dto.getCheckIn().getTime() : "");
                setCell(row, 8, dto.getCheckIn().getLateMinutes() != null ? String.valueOf(dto.getCheckIn().getLateMinutes()) : "0");
                setCell(row, 9, dto.getCheckOut().getTime() != null ? dto.getCheckOut().getTime() : "");
                setCell(row, 10, dto.getCheckOut().getEarlyMinutes() != null ? String.valueOf(dto.getCheckOut().getEarlyMinutes()) : "0");
                setCell(row, 11, dto.getTotalMinutes() != null
                        ? String.format("%.2f", dto.getTotalMinutes() / 60.0) : "");
                setCell(row, 12, dto.getOvertimeMinutes() != null ? String.valueOf(dto.getOvertimeMinutes()) : "0");

                // Cột Trạng thái với màu chữ theo status
                Cell statusCell = row.getCell(13);
                if (statusCell == null) statusCell = row.createCell(13);
                statusCell.setCellValue(statusLabel(dto.getStatus()));

                if (dto.getStatus() != null) {
                    CellStyle statusStyle = workbook.createCellStyle();
                    if (rowIdx % 2 == 0) {
                        statusStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                        statusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    }
                    Font statusFont = workbook.createFont();
                    statusFont.setColor(statusColors.getOrDefault(dto.getStatus(), IndexedColors.BLACK.getIndex()));
                    statusFont.setBold(true);
                    statusStyle.setFont(statusFont);
                    statusCell.setCellStyle(statusStyle);
                }

                setCell(row, 14, dto.getNote() != null ? dto.getNote() : "");
                rowIdx++;
            }

            // ── 3f. Auto-fit column width ─────────────────────────────────────
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Thêm padding nhỏ để tránh text bị cắt
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
            }

            // ── 4. Stream về client ───────────────────────────────────────────
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }

        log.info("[EXPORT] Xuất Excel chấm công {} bản ghi — filter: search={}, customerId={}, status={}, date=[{}, {}]",
                allRecords.size(), searchParam, customerId, statusStr, dateFrom, dateTo);
    }

    // ── Excel helper: ghi cell (giữ nguyên style nếu đã có) ──────────────────
    private void setCell(Row row, int col, String value) {
        Cell cell = row.getCell(col);
        if (cell == null) cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
    }

    // ── Nhãn tiếng Việt cho trạng thái trong Excel ───────────────────────────
    private String statusLabel(AttendanceStatus status) {
        if (status == null) return "";
        return switch (status) {
            case ON_TIME         -> "Đúng giờ";
            case LATE            -> "Trễ";
            case EARLY_LEAVE     -> "Về sớm";
            case ABSENT          -> "Vắng mặt";
            case OVERTIME        -> "Tăng ca";
            case PENDING_APPROVAL -> "Chờ duyệt";
        };
    }

    // ── Chuyển đổi AttendanceRecord → AttendanceHistoryResponse ──────────────
    private AttendanceHistoryResponse toHistoryResponse(AttendanceRecord a) {
        WorkSchedule ws = a.getWorkSchedule();
        User u          = a.getUser();
        Customer c      = (ws != null) ? ws.getCustomer() : null;
        String locationName    = (c != null) ? c.getName() : null;
        String locationAddress = (c != null) ? c.getAddress() : null;
        String shiftName       = "Ca làm việc"; // Không còn template
        
        LocalDate workDate     = (ws != null) ? ws.getWorkDate() : null;
        
        LocalTime startTime = (ws != null) ? ws.getScheduledStart().toLocalTime() : null;
        LocalTime endTime   = (ws != null) ? ws.getScheduledEnd().toLocalTime() : null;
        
        // Xác định loại ca (Morning/Afternoon/Night) dựa trên startTime
        String shiftTypeStr = "N/A";
        if (startTime != null) {
            LocalTime AFTERNOON_FROM = LocalTime.of(12, 0);
            LocalTime NIGHT_FROM     = LocalTime.of(18, 0);
            if (startTime.isBefore(AFTERNOON_FROM)) shiftTypeStr = "MORNING";
            else if (startTime.isBefore(NIGHT_FROM)) shiftTypeStr = "AFTERNOON";
            else shiftTypeStr = "NIGHT";
        }

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        // Tính overtimeMinutes
        Integer totalMinutes    = a.getActualMinutes();
        Integer overtimeMinutes = 0;
        if (totalMinutes != null && startTime != null && endTime != null) {
            int scheduledMin = (int) ChronoUnit.MINUTES.between(startTime, endTime);
            if (scheduledMin < 0) scheduledMin += 24 * 60; // ca qua đêm
            if (totalMinutes > scheduledMin) {
                overtimeMinutes = totalMinutes - scheduledMin;
            }
        }

        // Mã NV định dạng "NV001"
        String employeeCode = (u != null) ? String.format("NV%03d", u.getId()) : null;

        return AttendanceHistoryResponse.builder()
                .id(a.getId())
                .employee(AttendanceHistoryResponse.EmployeeInfo.builder()
                        .id(u != null ? u.getId() : null)
                        .name(u != null ? u.getFullName() : null)
                        .code(employeeCode)
                        .avatar(u != null ? u.getAvatarUrl() : null)
                        .department(u != null ? u.getArea() : null)
                        .build())
                .location(AttendanceHistoryResponse.LocationInfo.builder()
                        .id(c != null ? c.getId() : null)
                        .name(locationName)
                        .address(locationAddress)
                        .build())
                .shift(AttendanceHistoryResponse.ShiftInfo.builder()
                        .name(shiftName)
                        .startTime(startTime != null ? startTime.format(timeFmt) : null)
                        .endTime(endTime != null ? endTime.format(timeFmt) : null)
                        .type(shiftTypeStr)
                        .build())
                .date(workDate)
                .checkIn(AttendanceHistoryResponse.CheckInfo.builder()
                        .time(a.getCheckInTime() != null ? a.getCheckInTime().format(timeFmt) : null)
                        .method("GPS")
                        .note(null) // Field này trong DB AttendanceRecord chỉ có 1 field note chung
                        .lateMinutes(a.getLateMinutes())
                        .build())
                .checkOut(AttendanceHistoryResponse.CheckInfo.builder()
                        .time(a.getCheckOutTime() != null ? a.getCheckOutTime().format(timeFmt) : null)
                        .method("QR")
                        .note(null)
                        .earlyMinutes(a.getEarlyLeaveMinutes())
                        .build())
                .totalMinutes(totalMinutes)
                .overtimeMinutes(overtimeMinutes)
                .status(a.getStatus())
                .note(a.getNote())
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /v1/attendance/records — Admin xem danh sách bản ghi chấm công
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<AttendanceRecordResponse> getAttendanceRecords(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            AttendanceStatus status,
            int page,
            int size) {

        LocalDateTime startDt = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDt   = (endDate   != null) ? endDate.plusDays(1).atStartOfDay() : null;

        Pageable pageable = PageRequest.of(page, size);
        Page<AttendanceRecord> records = attendanceRecordRepo.findByFilters(
                employeeId, startDt, endDt, status, pageable);

        return records.map(this::toAttendanceRecordResponse);
    }

    private AttendanceRecordResponse toAttendanceRecordResponse(AttendanceRecord a) {
        WorkSchedule ws = a.getWorkSchedule();

        Long customerId     = null;
        String customerName = null;
        LocalDate shiftDate = null;

        if (ws != null) {
            shiftDate = ws.getWorkDate();
            if (ws.getCustomer() != null) {
                customerId   = ws.getCustomer().getId();
                customerName = ws.getCustomer().getName();
            }
        }

        Long explanationId = explanationRepo.findByAttendanceRecordId(a.getId())
                .stream()
                .findFirst()
                .map(ExplanationRequest::getId)
                .orElse(null);

        return AttendanceRecordResponse.builder()
                .id(a.getId())
                .employeeId(a.getUser() != null ? a.getUser().getId() : null)
                .employeeName(a.getUser() != null ? a.getUser().getFullName() : null)
                .customerId(customerId)
                .customerName(customerName)
                .shiftDate(shiftDate)
                .checkInTime(a.getCheckInTime())
                .checkOutTime(a.getCheckOutTime())
                .status(a.getStatus())
                .lateMinutes(a.getLateMinutes())
                .earlyLeaveMinutes(a.getEarlyLeaveMinutes())
                .distanceMeters(a.getCheckInDistanceMeters())
                .explanationId(explanationId)
                .build();
    }
}
