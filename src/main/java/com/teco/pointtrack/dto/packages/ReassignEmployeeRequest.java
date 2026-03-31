package com.teco.pointtrack.dto.packages;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReassignEmployeeRequest {

    @NotNull(message = "newEmployeeId không được để trống")
    Long newEmployeeId;
}
