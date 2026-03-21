package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.attendance.*;
import com.teco.pointtrack.entity.*;
import com.teco.pointtrack.entity.enums.*;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.*;
import com.teco.pointtrack.utils.GpsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    // ── System setting keys ───────────────────────────────────────────────────
    private static final String KEY_GPS_RADIUS          = "GPS_RADIUS_METERS";
    private static final String KEY_GRACE_PERIOD        = "GRACE_PERIOD_MINUTES";
    private static final String KEY_LATE_CHECKOUT_MINS  = "LATE_CHECKOUT_THRESHOLD_MINUTES";

    private static final double DEFAULT_GPS_RADIUS      = 50.0;
    private static final int    DEFAULT_GRACE_PERIOD    = 5;
    private static final int    DEFAULT_LATE_CHECKOUT   = 30;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final WorkScheduleRepository       workScheduleRepo;
    private final AttendanceRecordRepository   attendanceRecordRepo;
    private final AttendancePhotoRepository    attendancePhotoRepo;
    private final ExplanationRequestRepository explanationRepo;
    private final AttendanceAuditLogRepository auditLogRepo;
    private final UserRepository               userRepo;
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
        if (!schedule.getWorkDate().equals(LocalDate.now())) {
            throw new BadRequestException("Chỉ được check-in vào đúng ngày làm việc ("
                    + schedule.getWorkDate() + ")");
        }
        if (schedule.getStatus() != WorkScheduleStatus.SCHEDULED) {
            throw new ConflictException("Ca này đã được check-in hoặc đã hủy");
        }
        if (attendanceRecordRepo.existsByWorkScheduleId(workScheduleId)) {
            throw new ConflictException("Bạn đã check-in ca này rồi");
        }

        // ── 3. Validate Customer và tọa độ GPS ───────────────────────────────
        Customer customer = schedule.getCustomer();
        if (customer == null) {
            throw new BadRequestException("Ca làm việc này chưa được gán khách hàng/địa điểm cụ thể.");
        }
        if (customer.getLatitude() == null || customer.getLongitude() == null) {
            throw new BadRequestException(
                    "Địa điểm khách hàng (" + customer.getName() + ") chưa có tọa độ GPS. Vui lòng liên hệ Admin.");
        }

        // ── 4. BR-14: GPS Fencing ─────────────────────────────────────────────
        double gpsRadius    = getDoubleSetting(KEY_GPS_RADIUS, DEFAULT_GPS_RADIUS);
        double distanceM    = GpsUtils.distanceMeters(lat, lng, customer.getLatitude(), customer.getLongitude());
        boolean gpsValid    = distanceM <= gpsRadius;

        log.info("[CHECK-IN] userId={} scheduleId={} distance={}m radius={}m gpsValid={}",
                userId, workScheduleId, String.format("%.1f", distanceM), gpsRadius, gpsValid);

        // ── 5. Tính số phút đi muộn ───────────────────────────────────────────
        LocalDateTime now           = LocalDateTime.now();
        int gracePeriod             = getIntSetting(KEY_GRACE_PERIOD, DEFAULT_GRACE_PERIOD);
        LocalDateTime latestOnTime  = schedule.getScheduledStart().plusMinutes(gracePeriod);
        int lateMinutes             = (int) Math.max(0, ChronoUnit.MINUTES.between(latestOnTime, now));

        // ── 6. Xác định status bản ghi ────────────────────────────────────────
        // GPS sai ưu tiên hơn đi muộn (cần Admin xem xét vị trí trước)
        AttendanceStatus status = AttendanceStatus.ON_TIME;
        if (!gpsValid)          status = AttendanceStatus.PENDING_APPROVAL;  // BR-16.3
        else if (lateMinutes > 0) status = AttendanceStatus.LATE;            // BR-16.1

        // ── 7. Upload ảnh (BR-15) ──────────────────────────────────────────────
        String photoUrl = fileStorageService.storeAttendancePhoto(photo);

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
                .otMultiplier(schedule.getShiftTemplate().getOtMultiplier())  // BR-17: snapshot
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

        if (!gpsValid) {
            // BR-16.3: GPS sai — auto tạo, NV chưa cần nhập lý do ngay
            createdExplanations.add(createExplanation(record, user, ExplanationType.GPS_INVALID, null));
        }
        if (lateMinutes > 0) {
            // BR-16.1: Đi muộn — auto tạo, note từ payload là lý do (optional)
            createdExplanations.add(createExplanation(record, user, ExplanationType.LATE_CHECKIN, note));
        }

        // ── 11. Cập nhật WorkSchedule status ──────────────────────────────────
        schedule.setStatus(WorkScheduleStatus.CONFIRMED);
        workScheduleRepo.save(schedule);

        // ── 12. Build response ─────────────────────────────────────────────────
        Long firstExplanationId = createdExplanations.stream()
                .findFirst()
                .map(ExplanationRequest::getId)
                .orElse(null);

        String message = buildCheckInMessage(gpsValid, lateMinutes);

        return CheckInResponse.builder()
                .attendanceRecordId(record.getId())
                .status(status)
                .checkInTime(now)
                .distanceMeters(Math.round(distanceM * 10.0) / 10.0)
                .gpsValid(gpsValid)
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

        if (!record.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bản ghi này không thuộc về bạn");
        }
        if (record.getCheckInTime() == null) {
            throw new BadRequestException("Chưa có dữ liệu check-in cho bản ghi này");
        }
        if (record.getCheckOutTime() != null) {
            throw new ConflictException("Bạn đã check-out ca này rồi");
        }

        LocalDateTime now        = LocalDateTime.now();
        LocalDateTime schedEnd   = record.getWorkSchedule().getScheduledEnd();

        // ── 3. BR-16.2: Checkout trễ — bắt buộc nhập lý do ──────────────────
        int lateCheckoutThreshold = getIntSetting(KEY_LATE_CHECKOUT_MINS, DEFAULT_LATE_CHECKOUT);
        long minutesAfterEnd      = ChronoUnit.MINUTES.between(schedEnd, now);
        boolean isLateCheckout    = minutesAfterEnd > lateCheckoutThreshold;

        if (isLateCheckout && (checkOutReason == null || checkOutReason.isBlank())) {
            throw new BadRequestException(
                    "Checkout quá " + lateCheckoutThreshold + " phút sau giờ kết thúc ca. "
                    + "Vui lòng nhập lý do vào trường checkOutReason (BR-16.2)");
        }

        // ── 4. Tính thời gian ─────────────────────────────────────────────────
        int actualMinutes    = (int) ChronoUnit.MINUTES.between(record.getCheckInTime(), now);
        int earlyLeaveMinutes = (int) Math.max(0, ChronoUnit.MINUTES.between(now, schedEnd));

        // ── 5. Upload ảnh (BR-15) ──────────────────────────────────────────────
        String photoUrl = fileStorageService.storeAttendancePhoto(photo);

        // ── 6. Update AttendanceRecord ────────────────────────────────────────
        record.setCheckOutTime(now);
        record.setCheckOutLat(lat);
        record.setCheckOutLng(lng);
        record.setActualMinutes(actualMinutes);
        record.setEarlyLeaveMinutes(earlyLeaveMinutes);

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
            createExplanation(record, user, ExplanationType.LATE_CHECKOUT, checkOutReason);
        }

        return CheckOutResponse.builder()
                .attendanceRecordId(record.getId())
                .status(record.getStatus())
                .checkOutTime(now)
                .actualMinutes(actualMinutes)
                .earlyLeaveMinutes(earlyLeaveMinutes)
                .otMultiplier(record.getOtMultiplier())
                .message("Check-out thành công. Thời gian làm việc: " + actualMinutes + " phút.")
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

        // Tính lại actualMinutes nếu cả 2 mốc thời gian đã có
        if (record.getCheckInTime() != null && record.getCheckOutTime() != null) {
            int newActual = (int) ChronoUnit.MINUTES.between(
                    record.getCheckInTime(), record.getCheckOutTime());
            logs.add(buildAuditLog(recordId, adminId, "actualMinutes",
                    str(record.getActualMinutes()), str(newActual), req.getReason()));
            record.setActualMinutes(newActual);
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
            Customer c = schedule.getCustomer();
            StringBuilder sb = new StringBuilder();
            if (c.getStreet() != null && !c.getStreet().isBlank()) sb.append(c.getStreet());
            if (c.getWard() != null && !c.getWard().isBlank()) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(c.getWard());
            }
            if (c.getDistrict() != null && !c.getDistrict().isBlank()) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(c.getDistrict());
            }
            if (c.getCity() != null && !c.getCity().isBlank()) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(c.getCity());
            }
            addr = !sb.isEmpty() ? sb.toString() : null;
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

    private String buildCheckInMessage(boolean gpsValid, int lateMinutes) {
        if (!gpsValid && lateMinutes > 0) {
            return "Check-in thành công nhưng GPS nằm ngoài bán kính cho phép và đi muộn "
                    + lateMinutes + " phút. Đang chờ Admin duyệt.";
        }
        if (!gpsValid) {
            return "Check-in thành công nhưng GPS nằm ngoài bán kính cho phép. Đang chờ Admin duyệt.";
        }
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

    private String str(Object val) {
        return val == null ? null : val.toString();
    }
}
