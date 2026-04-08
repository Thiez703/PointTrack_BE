package com.teco.pointtrack.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teco.pointtrack.common.AuthUtils;
import com.teco.pointtrack.dto.settings.*;
import com.teco.pointtrack.entity.SystemSetting;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulingSettingsService {

    static final String KEY_GRACE_PERIOD      = "GRACE_PERIOD_MINUTES";
    static final String KEY_TRAVEL_BUFFER     = "TRAVEL_BUFFER_MINUTES";
    static final String KEY_PENALTY_RULES     = "PENALTY_RULES";

    static final int DEFAULT_GRACE_PERIOD     = 15;
    static final int DEFAULT_TRAVEL_BUFFER    = 15;
    static final String DEFAULT_PENALTY_RULES = "[{\"minLateMinutes\":15,\"penaltyShift\":0.5},{\"minLateMinutes\":30,\"penaltyShift\":1.0}]";

    private final SystemSettingRepository settingRepository;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // GET tất cả settings
    // GET /api/v1/scheduling/settings
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SchedulingSettingsResponse getAll() {
        return SchedulingSettingsResponse.builder()
                .gracePeriodMinutes(getIntSetting(KEY_GRACE_PERIOD, DEFAULT_GRACE_PERIOD))
                .travelBufferMinutes(getIntSetting(KEY_TRAVEL_BUFFER, DEFAULT_TRAVEL_BUFFER))
                .penaltyRules(getPenaltyRules())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT grace period
    // PUT /api/v1/scheduling/settings/grace-period
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public SchedulingSettingsResponse updateGracePeriod(GracePeriodRequest request) {
        saveSetting(KEY_GRACE_PERIOD, String.valueOf(request.getMinutes()),
            "Dung sai check-in ±N phút vẫn tính đúng giờ (BR-11)");

        log.info("Updated grace period: {}p", request.getMinutes());
        return getAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT travel buffer
    // PUT /api/v1/scheduling/settings/travel-buffer
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public SchedulingSettingsResponse updateTravelBuffer(TravelBufferRequest request) {
        saveSetting(KEY_TRAVEL_BUFFER, String.valueOf(request.getMinutes()),
                "Thời gian đệm di chuyển tối thiểu giữa 2 ca (BR-09)");

        log.info("Updated travel buffer: {}p", request.getMinutes());
        return getAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT penalty rules
    // PUT /api/v1/scheduling/settings/penalty-rules
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public SchedulingSettingsResponse updatePenaltyRules(PenaltyRulesRequest request) {
        List<PenaltyRuleItem> rules = request.getRules();

        // Validate: minLateMinutes phải tăng dần (BR-12)
        for (int i = 1; i < rules.size(); i++) {
            if (rules.get(i).getMinLateMinutes() <= rules.get(i - 1).getMinLateMinutes()) {
                throw new BadRequestException("PENALTY_RULES_NOT_ASCENDING");
            }
        }

        try {
            String json = objectMapper.writeValueAsString(rules);
            saveSetting(KEY_PENALTY_RULES, json, "Bậc thang penalty check-in muộn (BR-12)");
        } catch (Exception e) {
            throw new BadRequestException("PENALTY_RULES_INVALID");
        }

        log.info("Updated penalty rules: {} rules", rules.size());
        return getAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers – dùng bởi các service khác (ShiftService, AttendanceService)
    // ─────────────────────────────────────────────────────────────────────────

    public int getGracePeriodMinutes() {
        return getIntSetting(KEY_GRACE_PERIOD, DEFAULT_GRACE_PERIOD);
    }

    public int getTravelBufferMinutes() {
        return getIntSetting(KEY_TRAVEL_BUFFER, DEFAULT_TRAVEL_BUFFER);
    }

    public List<PenaltyRuleItem> getPenaltyRules() {
        String json = settingRepository.findById(KEY_PENALTY_RULES)
                .map(SystemSetting::getValue)
                .orElse(DEFAULT_PENALTY_RULES);
        try {
            return objectMapper.readValue(json, new TypeReference<List<PenaltyRuleItem>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse penalty rules, using default");
            return parseDefaultPenaltyRules();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void saveSetting(String key, String value, String description) {
        Long currentUserId = AuthUtils.getUserDetail() != null ? AuthUtils.getUserDetail().getId() : null;
        SystemSetting setting = settingRepository.findById(key)
                .orElse(SystemSetting.builder().key(key).description(description).build());

        setting.setValue(value);
        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedByUserId(currentUserId);
        settingRepository.save(setting);
    }

    private int getIntSetting(String key, int defaultValue) {
        return settingRepository.findById(key)
                .map(s -> {
                    try { return Integer.parseInt(s.getValue()); }
                    catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }

    private List<PenaltyRuleItem> parseDefaultPenaltyRules() {
        try {
            return objectMapper.readValue(DEFAULT_PENALTY_RULES, new TypeReference<List<PenaltyRuleItem>>() {});
        } catch (Exception e) {
            return List.of(
                    new PenaltyRuleItem(15, 0.5),
                    new PenaltyRuleItem(30, 1.0)
            );
        }
    }
}
