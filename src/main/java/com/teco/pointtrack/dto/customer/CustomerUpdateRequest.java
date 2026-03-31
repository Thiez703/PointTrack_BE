package com.teco.pointtrack.dto.customer;

import com.teco.pointtrack.entity.enums.CustomerSource;
import com.teco.pointtrack.entity.enums.CustomerStatus;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Body cho PUT /api/v1/customers/{id} — Cập nhật khách hàng.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerUpdateRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 255)
    String name;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "SĐT phải 10 chữ số và bắt đầu bằng 0")
    String phone;

    @Pattern(regexp = "^(0\\d{9})?$", message = "SĐT phụ phải 10 chữ số và bắt đầu bằng 0 (hoặc để trống)")
    String secondaryPhone;

    @NotBlank(message = "Địa chỉ không được để trống")
    String address;

    String specialNotes;

    @Size(max = 255)
    String preferredTimeNote;

    CustomerSource source;

    CustomerStatus status;

    /**
     * Nếu cả latitude và longitude được cung cấp → dùng trực tiếp (bỏ qua geocoding).
     * Nếu null và địa chỉ thay đổi → tự động geocode lại.
     */
    @DecimalMin(value = "-90.0",  message = "Latitude phải từ -90 đến 90")
    @DecimalMax(value = "90.0",   message = "Latitude phải từ -90 đến 90")
    Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude phải từ -180 đến 180")
    @DecimalMax(value = "180.0",  message = "Longitude phải từ -180 đến 180")
    Double longitude;
}
