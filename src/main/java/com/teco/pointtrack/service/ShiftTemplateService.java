package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.shift.ShiftTemplateRequest;
import com.teco.pointtrack.dto.shift.ShiftTemplateResponse;
import com.teco.pointtrack.entity.ShiftTemplate;
import com.teco.pointtrack.entity.enums.ShiftType;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.ShiftTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftTemplateService {

    private final ShiftTemplateRepository shiftTemplateRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy danh sách ca mẫu
    // GET /api/v1/scheduling/shift-templates
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ShiftTemplateResponse> getAll() {
        return shiftTemplateRepository.findAllByDeletedAtIsNullOrderByDefaultStartAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy chi tiết ca mẫu theo ID
    // GET /api/v1/scheduling/shift-templates/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ShiftTemplateResponse getById(Long id) {
        return toResponse(findActive(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tạo ca mẫu mới
    // POST /api/v1/scheduling/shift-templates
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ShiftTemplateResponse create(ShiftTemplateRequest request) {
        if (shiftTemplateRepository.existsByNameAndDeletedAtIsNull(request.getName())) {
            throw new ConflictException("SHIFT_TEMPLATE_NAME_CONFLICT");
        }

        validateShiftTime(request.getShiftType(), request.getDefaultStart(), request.getDefaultEnd());

        int duration = calculateDuration(request.getShiftType(), request.getDefaultStart(), request.getDefaultEnd());

        ShiftTemplate template = ShiftTemplate.builder()
                .name(request.getName().trim())
                .defaultStart(request.getDefaultStart())
                .defaultEnd(request.getDefaultEnd())
                .durationMinutes(duration)
                .shiftType(request.getShiftType())
                .color(request.getColor() != null ? request.getColor() : "#4CAF50")
                .otMultiplier(request.getOtMultiplier())
                .build();

        ShiftTemplate saved = shiftTemplateRepository.save(template);
        log.info("Created shift template: id={}, name={}, type={}, duration={}m",
                saved.getId(), saved.getName(), saved.getShiftType(), saved.getDurationMinutes());

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cập nhật ca mẫu
    // PUT /api/v1/scheduling/shift-templates/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ShiftTemplateResponse update(Long id, ShiftTemplateRequest request) {
        ShiftTemplate template = findActive(id);

        if (shiftTemplateRepository.existsByNameAndIdNotAndDeletedAtIsNull(request.getName(), id)) {
            throw new ConflictException("SHIFT_TEMPLATE_NAME_CONFLICT");
        }

        validateShiftTime(request.getShiftType(), request.getDefaultStart(), request.getDefaultEnd());

        int duration = calculateDuration(request.getShiftType(), request.getDefaultStart(), request.getDefaultEnd());

        template.setName(request.getName().trim());
        template.setDefaultStart(request.getDefaultStart());
        template.setDefaultEnd(request.getDefaultEnd());
        template.setDurationMinutes(duration);
        template.setShiftType(request.getShiftType());
        template.setColor(request.getColor() != null ? request.getColor() : template.getColor());
        template.setOtMultiplier(request.getOtMultiplier());

        shiftTemplateRepository.save(template);
        log.info("Updated shift template: id={}, name={}", template.getId(), template.getName());

        return toResponse(template);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Xoá ca mẫu (soft delete)
    // DELETE /api/v1/scheduling/shift-templates/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        ShiftTemplate template = findActive(id);

        template.setDeletedAt(LocalDateTime.now());
        template.setIsActive(false);
        shiftTemplateRepository.save(template);

        log.info("Soft-deleted shift template id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ShiftTemplate findActive(Long id) {
        return shiftTemplateRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("SHIFT_TEMPLATE_NOT_FOUND", id));
    }

    /**
     * Validate rule BR-10:
     * - NORMAL & HOLIDAY: end phải > start (chỉ trong ngày).
     * - OT_EMERGENCY: cho phép end < start (qua đêm).
     */
    private void validateShiftTime(ShiftType type, LocalTime start, LocalTime end) {
        if (type == ShiftType.NORMAL || type == ShiftType.HOLIDAY) {
            if (!end.isAfter(start)) {
                throw new BadRequestException("SHIFT_TEMPLATE_OVERNIGHT_NOT_ALLOWED");
            }
        }
        // OT_EMERGENCY: không giới hạn, end < start = qua đêm
    }

    /**
     * Tính duration (phút):
     * - NORMAL/HOLIDAY: end - start (cùng ngày).
     * - OT_EMERGENCY + end < start (qua đêm): (24h - start) + end.
     * - OT_EMERGENCY + end >= start: end - start (không qua đêm).
     */
    private int calculateDuration(ShiftType type, LocalTime start, LocalTime end) {
        if (type == ShiftType.OT_EMERGENCY && end.isBefore(start)) {
            // Ca qua đêm: phút còn lại trong ngày + phút đầu ngày hôm sau
            int minutesUntilMidnight = (24 * 60) - (start.getHour() * 60 + start.getMinute());
            int minutesAfterMidnight = end.getHour() * 60 + end.getMinute();
            return minutesUntilMidnight + minutesAfterMidnight;
        }
        // Ca trong ngày
        return (end.getHour() * 60 + end.getMinute()) - (start.getHour() * 60 + start.getMinute());
    }

    private ShiftTemplateResponse toResponse(ShiftTemplate t) {
        return ShiftTemplateResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .durationMinutes(t.getDurationMinutes())
                .defaultStart(t.getDefaultStart())
                .defaultEnd(t.getDefaultEnd())
                .shiftType(t.getShiftType())
                .color(t.getColor())
                .otMultiplier(t.getOtMultiplier())
                .isActive(t.getIsActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}