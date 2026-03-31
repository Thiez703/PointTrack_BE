package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.shift.ConflictCheckResponse;
import com.teco.pointtrack.entity.Customer;
import com.teco.pointtrack.entity.Shift;
import com.teco.pointtrack.entity.enums.ShiftType;
import com.teco.pointtrack.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * BR-09 (Travel Buffer) + BR-13 (Conflict Detection).
 *
 * <p>Buffer mặc định 15 phút. Nếu cả 2 ca đều có toạ độ GPS của khách hàng
 * và Google Maps API key đã cấu hình, buffer sẽ dùng thời gian lái xe thực tế.
 *
 * Được tái sử dụng bởi ShiftService và ServicePackageService.
 */
@Service
@RequiredArgsConstructor
public class ConflictCheckerService {

    static final int BUFFER_MINUTES = 15;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ShiftRepository   shiftRepository;
    private final TravelTimeService travelTimeService;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API – overload KHÔNG có customer (backward-compat, buffer cố định)
    // ─────────────────────────────────────────────────────────────────────────

    public ConflictCheckResponse check(Long employeeId,
                                       LocalDate shiftDate,
                                       LocalTime startTime,
                                       LocalTime endTime,
                                       ShiftType shiftType,
                                       Long excludeShiftId) {
        return check(employeeId, shiftDate, startTime, endTime, shiftType, excludeShiftId, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API – có customer → buffer tính theo khoảng cách thực tế (BR-09)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @param proposedCustomer Khách hàng của ca đề xuất (nullable → fallback 15 phút)
     */
    public ConflictCheckResponse check(Long employeeId,
                                       LocalDate shiftDate,
                                       LocalTime startTime,
                                       LocalTime endTime,
                                       ShiftType shiftType,
                                       Long excludeShiftId,
                                       Customer proposedCustomer) {

        List<Shift> nearby = shiftRepository.findActiveShiftsNear(
                employeeId,
                shiftDate.minusDays(1),
                shiftDate.plusDays(1),
                excludeShiftId
        );

        LocalDateTime propStart = toAbsoluteStart(shiftDate, startTime);
        LocalDateTime propEnd   = toAbsoluteEnd(shiftDate, startTime, endTime, shiftType);

        for (Shift existing : nearby) {
            LocalDateTime exStart = toAbsoluteStart(existing.getShiftDate(), existing.getStartTime());
            LocalDateTime exEnd   = toAbsoluteEnd(
                    existing.getShiftDate(),
                    existing.getStartTime(),
                    existing.getEndTime(),
                    existing.getShiftType());

            // 1. Kiểm tra OVERLAP (BR-13 hard block)
            if (overlaps(propStart, propEnd, exStart, exEnd)) {
                return ConflictCheckResponse.builder()
                        .hasConflict(true)
                        .conflictType("OVERLAP")
                        .conflictingShiftId(existing.getId())
                        .detail(String.format(
                                "Ca đề xuất %s-%s trùng giờ với ca hiện tại %s-%s (id=%d)",
                                propStart.format(TIME_FMT), propEnd.format(TIME_FMT),
                                exStart.format(TIME_FMT),  exEnd.format(TIME_FMT),
                                existing.getId()))
                        .build();
            }

            // 2. Buffer sau ca hiện tại → ca đề xuất
            //    Di chuyển: existing.customer → proposedCustomer
            int bufferAfter = requiredBufferMinutes(existing.getCustomer(), proposedCustomer);
            long gapAfterExisting = ChronoUnit.MINUTES.between(exEnd, propStart);
            if (gapAfterExisting >= 0 && gapAfterExisting < bufferAfter) {
                int shortage = (int) (bufferAfter - gapAfterExisting);
                return ConflictCheckResponse.builder()
                        .hasConflict(true)
                        .conflictType("BUFFER")
                        .conflictingShiftId(existing.getId())
                        .detail(String.format(
                                "Ca hiện tại (id=%d) kết thúc %s, ca đề xuất bắt đầu %s → cần %d phút di chuyển, thiếu %d phút",
                                existing.getId(),
                                exEnd.format(TIME_FMT),
                                propStart.format(TIME_FMT),
                                bufferAfter, shortage))
                        .minutesShort(shortage)
                        .build();
            }

            // 3. Buffer trước ca hiện tại ← ca đề xuất
            //    Di chuyển: proposedCustomer → existing.customer
            int bufferBefore = requiredBufferMinutes(proposedCustomer, existing.getCustomer());
            long gapBeforeExisting = ChronoUnit.MINUTES.between(propEnd, exStart);
            if (gapBeforeExisting >= 0 && gapBeforeExisting < bufferBefore) {
                int shortage = (int) (bufferBefore - gapBeforeExisting);
                return ConflictCheckResponse.builder()
                        .hasConflict(true)
                        .conflictType("BUFFER")
                        .conflictingShiftId(existing.getId())
                        .detail(String.format(
                                "Ca đề xuất kết thúc %s, ca tiếp theo (id=%d) bắt đầu %s → cần %d phút di chuyển, thiếu %d phút",
                                propEnd.format(TIME_FMT),
                                existing.getId(),
                                exStart.format(TIME_FMT),
                                bufferBefore, shortage))
                        .minutesShort(shortage)
                        .build();
            }
        }

        return ConflictCheckResponse.builder()
                .hasConflict(false)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers – package-private để dùng trong test
    // ─────────────────────────────────────────────────────────────────────────

    LocalDateTime toAbsoluteStart(LocalDate date, LocalTime start) {
        return date.atTime(start);
    }

    LocalDateTime toAbsoluteEnd(LocalDate date, LocalTime start, LocalTime end, ShiftType type) {
        if (type == ShiftType.OT_EMERGENCY && end.isBefore(start)) {
            return date.plusDays(1).atTime(end);
        }
        return date.atTime(end);
    }

    boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd,
                     LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    /**
     * Tính buffer cần thiết (phút) để di chuyển từ {@code from} đến {@code to}.
     * Dùng Google Maps Distance Matrix nếu cả hai có toạ độ; fallback về 15 phút.
     */
    private int requiredBufferMinutes(Customer from, Customer to) {
        if (from == null || to == null) return BUFFER_MINUTES;

        Double fromLat = from.getLatitude(), fromLng = from.getLongitude();
        Double toLat   = to.getLatitude(),   toLng   = to.getLongitude();

        if (fromLat == null || fromLng == null || toLat == null || toLng == null) {
            return BUFFER_MINUTES;
        }

        return travelTimeService.getTravelMinutes(fromLat, fromLng, toLat, toLng, BUFFER_MINUTES);
    }
}
