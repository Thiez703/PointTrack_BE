package com.teco.pointtrack.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teco.pointtrack.dto.salary.SalaryLevelResponse;
import com.teco.pointtrack.entity.enums.Gender;
import com.teco.pointtrack.entity.enums.UserStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetail implements Serializable {

    Long id;
    String fullName;
    String email;
    String phoneNumber;
    String contact;
    LocalDate dateOfBirth;
    String avatarUrl;
    Gender gender;
    UserStatus status;
    String role;
    String roleName;
    
    // Thông tin cấp bậc lương
    Long salaryLevelId;
    String salaryLevelName;
    SalaryLevelResponse salaryLevel;
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
