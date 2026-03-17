package com.teco.pointtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Bảng cấu hình hệ thống dạng key-value.
 * Keys: GRACE_PERIOD_MINUTES, PENALTY_RULES (JSON), TRAVEL_BUFFER_MINUTES
 */
@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemSetting {

    @Id
    @Column(name = "setting_key", length = 100)
    String key;

    /** Giá trị lưu dạng text (số nguyên hoặc JSON array) */
    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    String value;

    @Column(length = 255)
    String description;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "updated_by_user_id")
    Long updatedByUserId;
}