package com.teco.pointtrack.entity;

import com.teco.pointtrack.entity.enums.PhotoType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * BR-15: Ảnh bắt buộc kèm metadata GPS + timestamp khi check-in/out.
 */
@Entity
@Table(name = "attendance_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendancePhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_record_id", nullable = false)
    AttendanceRecord attendanceRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    PhotoType type;

    /** Relative path hoặc full URL (S3/Cloudinary) */
    @Column(name = "photo_url", nullable = false, length = 1000)
    String photoUrl;

    // ── GPS metadata từ thiết bị (BR-15) ─────────────────────────────────────

    /** Tọa độ GPS của thiết bị tại lúc chụp ảnh */
    @Column(name = "captured_lat")
    Double capturedLat;

    @Column(name = "captured_lng")
    Double capturedLng;

    /** Timestamp đồng hồ thiết bị tại lúc chụp (BR-15) */
    @Column(name = "captured_at")
    LocalDateTime capturedAt;

    // ── File metadata ─────────────────────────────────────────────────────────

    @Column(name = "file_size_bytes")
    Long fileSizeBytes;

    @Column(name = "mime_type", length = 50)
    String mimeType;

    @Column(name = "original_file_name", length = 255)
    String originalFileName;
}
