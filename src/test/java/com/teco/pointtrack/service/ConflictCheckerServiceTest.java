package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.shift.ConflictCheckResponse;
import com.teco.pointtrack.entity.Customer;
import com.teco.pointtrack.entity.Shift;
import com.teco.pointtrack.entity.ShiftTemplate;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.ShiftStatus;
import com.teco.pointtrack.entity.enums.ShiftType;
import com.teco.pointtrack.repository.ShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests cho ConflictCheckerService – BR-09 (Travel Buffer) + BR-13 (Conflict Detection).
 */
@ExtendWith(MockitoExtension.class)
class ConflictCheckerServiceTest {

    @Mock
    ShiftRepository shiftRepository;

    @InjectMocks
    ConflictCheckerService service;

    private static final Long EMPLOYEE_ID = 1L;
    private static final LocalDate DATE     = LocalDate.of(2026, 3, 21);

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Shift buildShift(LocalDate date, LocalTime start, LocalTime end, ShiftType type) {
        User employee = User.builder().id(EMPLOYEE_ID).fullName("Test Employee").build();
        Customer customer = Customer.builder().id(1L).name("Test Customer").build();
        return Shift.builder()
                .id(99L)
                .employee(employee)
                .customer(customer)
                .shiftDate(date)
                .startTime(start)
                .endTime(end)
                .durationMinutes(120)
                .shiftType(type)
                .otMultiplier(BigDecimal.ONE)
                .status(ShiftStatus.SCHEDULED)
                .build();
    }

    private void mockNoExistingShifts() {
        when(shiftRepository.findActiveShiftsNear(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
    }

    private void mockExistingShift(Shift shift) {
        when(shiftRepository.findActiveShiftsNear(anyLong(), any(), any(), any()))
                .thenReturn(List.of(shift));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // No conflict
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Không có conflict khi lịch trống")
    void noConflict_whenNoExistingShifts() {
        mockNoExistingShifts();
        ConflictCheckResponse result = service.check(
                EMPLOYEE_ID, DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL, null);

        assertThat(result.isHasConflict()).isFalse();
        assertThat(result.getConflictType()).isNull();
    }

    @Test
    @DisplayName("Không có conflict khi ca đủ buffer (≥15 phút)")
    void noConflict_whenBufferSufficient() {
        // Existing: 08:00-10:00. Proposed: 10:15-12:00 → gap = 15 min (OK)
        Shift existing = buildShift(DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL);
        mockExistingShift(existing);

        ConflictCheckResponse result = service.check(
                EMPLOYEE_ID, DATE, LocalTime.of(10, 15), LocalTime.of(12, 0), ShiftType.NORMAL, null);

        assertThat(result.isHasConflict()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OVERLAP
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("BR-13: OVERLAP detection")
    class OverlapTests {

        @Test
        @DisplayName("Trùng hoàn toàn → OVERLAP")
        void fullOverlap() {
            Shift existing = buildShift(DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL);
            mockExistingShift(existing);

            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL, null);

            assertThat(result.isHasConflict()).isTrue();
            assertThat(result.getConflictType()).isEqualTo("OVERLAP");
        }

        @Test
        @DisplayName("Partial overlap đầu ca → OVERLAP")
        void partialOverlapAtStart() {
            // Existing: 08:00-10:00. Proposed: 09:00-11:00
            Shift existing = buildShift(DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL);
            mockExistingShift(existing);

            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(9, 0), LocalTime.of(11, 0), ShiftType.NORMAL, null);

            assertThat(result.isHasConflict()).isTrue();
            assertThat(result.getConflictType()).isEqualTo("OVERLAP");
        }

        @Test
        @DisplayName("Partial overlap cuối ca → OVERLAP")
        void partialOverlapAtEnd() {
            // Existing: 09:00-11:00. Proposed: 08:00-10:00
            Shift existing = buildShift(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0), ShiftType.NORMAL);
            mockExistingShift(existing);

            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL, null);

            assertThat(result.isHasConflict()).isTrue();
            assertThat(result.getConflictType()).isEqualTo("OVERLAP");
        }

        @Test
        @DisplayName("Ca đề xuất nằm trong ca hiện tại → OVERLAP")
        void proposedInsideExisting() {
            // Existing: 08:00-12:00. Proposed: 09:00-11:00
            Shift existing = buildShift(DATE, LocalTime.of(8, 0), LocalTime.of(12, 0), ShiftType.NORMAL);
            mockExistingShift(existing);

            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(9, 0), LocalTime.of(11, 0), ShiftType.NORMAL, null);

            assertThat(result.isHasConflict()).isTrue();
            assertThat(result.getConflictType()).isEqualTo("OVERLAP");
        }

        @Test
        @DisplayName("Ca kế tiếp bắt đầu đúng lúc ca hiện tại kết thúc → Không overlap (half-open)")
        void adjacentShifts_noOverlap() {
            // Existing: 08:00-10:00. Proposed: 10:00-12:00 → gap = 0, but not overlap
            // Tuy nhiên buffer = 0 < 15 → BUFFER
            Shift existing = buildShift(DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL);
            mockExistingShift(existing);

            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(10, 0), LocalTime.of(12, 0), ShiftType.NORMAL, null);

            assertThat(result.getConflictType()).isNotEqualTo("OVERLAP");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUFFER VIOLATION (BR-09)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("BR-09: Travel Buffer (15 phút)")
    class BufferTests {

        @Test
        @DisplayName("Gap = 0 phút → BUFFER, thiếu 15 phút")
        void bufferViolation_zeroGap() {
            Shift existing = buildShift(DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL);
            mockExistingShift(existing);

            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(10, 0), LocalTime.of(12, 0), ShiftType.NORMAL, null);

            assertThat(result.isHasConflict()).isTrue();
            assertThat(result.getConflictType()).isEqualTo("BUFFER");
            assertThat(result.getMinutesShort()).isEqualTo(15);
        }

        @Test
        @DisplayName("Gap = 10 phút → BUFFER, thiếu 5 phút")
        void bufferViolation_tenMinGap() {
            // Existing: 08:00-10:00. Proposed: 10:10-12:00 → gap=10 → thiếu 5
            Shift existing = buildShift(DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL);
            mockExistingShift(existing);

            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(10, 10), LocalTime.of(12, 0), ShiftType.NORMAL, null);

            assertThat(result.isHasConflict()).isTrue();
            assertThat(result.getConflictType()).isEqualTo("BUFFER");
            assertThat(result.getMinutesShort()).isEqualTo(5);
            assertThat(result.getDetail()).contains("thiếu 5 phút");
        }

        @Test
        @DisplayName("Gap = 14 phút → BUFFER, thiếu 1 phút")
        void bufferViolation_fourteenMinGap() {
            Shift existing = buildShift(DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL);
            mockExistingShift(existing);

            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(10, 14), LocalTime.of(12, 0), ShiftType.NORMAL, null);

            assertThat(result.isHasConflict()).isTrue();
            assertThat(result.getConflictType()).isEqualTo("BUFFER");
            assertThat(result.getMinutesShort()).isEqualTo(1);
        }

        @Test
        @DisplayName("Gap chính xác 15 phút → Không vi phạm buffer")
        void bufferExact15_noViolation() {
            Shift existing = buildShift(DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL);
            mockExistingShift(existing);

            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(10, 15), LocalTime.of(12, 0), ShiftType.NORMAL, null);

            assertThat(result.isHasConflict()).isFalse();
        }

        @Test
        @DisplayName("Ca đề xuất trước ca hiện tại, gap < 15 phút → BUFFER")
        void bufferViolation_proposedBeforeExisting() {
            // Proposed: 08:00-10:00. Existing: 10:05-12:00 → gap=5 → thiếu 10
            Shift existing = buildShift(DATE, LocalTime.of(10, 5), LocalTime.of(12, 0), ShiftType.NORMAL);
            mockExistingShift(existing);

            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL, null);

            assertThat(result.isHasConflict()).isTrue();
            assertThat(result.getConflictType()).isEqualTo("BUFFER");
            assertThat(result.getMinutesShort()).isEqualTo(10);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OT_EMERGENCY overnight (BR-10)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OT_EMERGENCY – ca qua đêm")
    class OvernightTests {

        @Test
        @DisplayName("Ca qua đêm 22:00-02:00 không conflict với ca sáng hôm sau 08:00-10:00")
        void overnightNoConflict_withNextDayShift() {
            // Existing: ngày sau, 08:00-10:00 (NORMAL)
            Shift nextDayShift = buildShift(DATE.plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0), ShiftType.NORMAL);
            mockExistingShift(nextDayShift);

            // Proposed: DATE 22:00 → DATE+1 02:00 (OT_EMERGENCY overnight)
            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(22, 0), LocalTime.of(2, 0), ShiftType.OT_EMERGENCY, null);

            assertThat(result.isHasConflict()).isFalse();
        }

        @Test
        @DisplayName("Ca qua đêm 22:00-02:00 overlap với ca 01:00-03:00 ngày hôm sau → OVERLAP")
        void overnightOverlap_withNextDayShift() {
            // Existing: DATE+1, 01:00-03:00
            Shift nextDayShift = buildShift(DATE.plusDays(1), LocalTime.of(1, 0), LocalTime.of(3, 0), ShiftType.NORMAL);
            mockExistingShift(nextDayShift);

            // Proposed overnight: DATE 22:00 → DATE+1 02:00
            ConflictCheckResponse result = service.check(
                    EMPLOYEE_ID, DATE, LocalTime.of(22, 0), LocalTime.of(2, 0), ShiftType.OT_EMERGENCY, null);

            assertThat(result.isHasConflict()).isTrue();
            assertThat(result.getConflictType()).isEqualTo("OVERLAP");
        }

        @Test
        @DisplayName("toAbsoluteEnd: OT_EMERGENCY qua đêm tính đúng ngày hôm sau")
        void toAbsoluteEnd_overnightCalculation() {
            LocalDate date  = LocalDate.of(2026, 3, 21);
            LocalTime start = LocalTime.of(22, 0);
            LocalTime end   = LocalTime.of(2, 0);

            var absEnd = service.toAbsoluteEnd(date, start, end, ShiftType.OT_EMERGENCY);

            assertThat(absEnd.toLocalDate()).isEqualTo(date.plusDays(1));
            assertThat(absEnd.toLocalTime()).isEqualTo(LocalTime.of(2, 0));
        }

        @Test
        @DisplayName("toAbsoluteEnd: OT_EMERGENCY không qua đêm (end > start) → cùng ngày")
        void toAbsoluteEnd_otEmergency_sameDay() {
            LocalDate date  = LocalDate.of(2026, 3, 21);
            LocalTime start = LocalTime.of(8, 0);
            LocalTime end   = LocalTime.of(12, 0);

            var absEnd = service.toAbsoluteEnd(date, start, end, ShiftType.OT_EMERGENCY);

            assertThat(absEnd.toLocalDate()).isEqualTo(date);
            assertThat(absEnd.toLocalTime()).isEqualTo(LocalTime.of(12, 0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Overlap utility
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("overlaps() utility – half-open interval")
    class OverlapsUtilityTests {

        @Test
        @DisplayName("A hoàn toàn trước B → không overlap")
        void noOverlap_aBefore() {
            var aStart = DATE.atTime(8, 0);
            var aEnd   = DATE.atTime(10, 0);
            var bStart = DATE.atTime(10, 0);
            var bEnd   = DATE.atTime(12, 0);
            assertThat(service.overlaps(aStart, aEnd, bStart, bEnd)).isFalse();
        }

        @Test
        @DisplayName("A hoàn toàn sau B → không overlap")
        void noOverlap_aAfter() {
            var aStart = DATE.atTime(12, 0);
            var aEnd   = DATE.atTime(14, 0);
            var bStart = DATE.atTime(8, 0);
            var bEnd   = DATE.atTime(10, 0);
            assertThat(service.overlaps(aStart, aEnd, bStart, bEnd)).isFalse();
        }

        @Test
        @DisplayName("A và B chồng lấp → overlap")
        void overlap_partial() {
            var aStart = DATE.atTime(8, 0);
            var aEnd   = DATE.atTime(10, 0);
            var bStart = DATE.atTime(9, 0);
            var bEnd   = DATE.atTime(11, 0);
            assertThat(service.overlaps(aStart, aEnd, bStart, bEnd)).isTrue();
        }
    }
}
