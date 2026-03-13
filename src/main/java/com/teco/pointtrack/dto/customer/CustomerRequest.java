package com.teco.pointtrack.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Request body cho POST (tạo) và PUT (cập nhật) khách hàng
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerRequest {

    @NotBlank
    @Size(max = 150)
    String name;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Phone number must be 10-11 digits")
    String phoneNumber;

    @Email
    String email;

    String street;
    String ward;
    String district;
    String city;
    String note;
    Boolean isActive;
}

