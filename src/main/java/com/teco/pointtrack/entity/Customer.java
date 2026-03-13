package com.teco.pointtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Tên khách hàng */
    @Column(nullable = false, length = 150)
    String name;

    @Column(name = "phone_number", length = 15)
    String phoneNumber;

    @Column(length = 150)
    String email;

    // ── Địa chỉ ──────────────────────────────────────────────────────────────

    /** Số nhà, tên đường */
    @Column(length = 255)
    String street;

    /** Phường / xã */
    @Column(length = 100)
    String ward;

    /** Quận / huyện */
    @Column(length = 100)
    String district;

    /** Tỉnh / thành phố */
    @Column(length = 100)
    String city;

    // ── GPS Coordinates ───────────────────────────────────────────────────────

    /** Vĩ độ (nullable — tự động geocode) */
    @Column(name = "latitude")
    Double latitude;

    /** Kinh độ (nullable — tự động geocode) */
    @Column(name = "longitude")
    Double longitude;

    // ── Khác ──────────────────────────────────────────────────────────────────

    /** Ghi chú */
    @Column(length = 500)
    String note;

    /** Trạng thái: true = đang hoạt động */
    @Builder.Default
    Boolean isActive = true;

    /** BR-22: soft delete */
    @Column(name = "deleted_at")
    LocalDateTime deletedAt;
}

