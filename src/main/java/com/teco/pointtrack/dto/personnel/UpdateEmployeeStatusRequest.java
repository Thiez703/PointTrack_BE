package com.teco.pointtrack.dto.personnel;

import com.teco.pointtrack.entity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Request body cho PATCH /api/v1/personnel/{id}/status
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEmployeeStatusRequest {

    /** ACTIVE hoặc INACTIVE */
    @NotNull
    UserStatus status;
}

