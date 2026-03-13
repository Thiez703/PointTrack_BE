package com.teco.pointtrack.dto.personnel;

import com.teco.pointtrack.entity.enums.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Request body cho PUT /api/v1/personnel/{id}
 * Tất cả field đều optional — chỉ cập nhật field khác null
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEmployeeRequest {

    @Size(min = 2, max = 100)
    String fullName;

    @Pattern(regexp = "^[0-9]{10,11}$")
    String phoneNumber;

    /** Định dạng yyyy-MM-dd, parse sang LocalDate trong service */
    String dateOfBirth;

    String avatarUrl;

    /** MALE / FEMALE / OTHER */
    Gender gender;

    /** Định dạng yyyy-MM-dd, parse sang LocalDate trong service */
    String startDate;
}

