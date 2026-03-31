package com.teco.pointtrack.dto.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teco.pointtrack.entity.enums.CustomerSource;
import com.teco.pointtrack.entity.enums.CustomerStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Response cho danh sách và CRUD khách hàng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponse {

    Long id;
    String name;
    String phone;
    String secondaryPhone;
    String address;
    Double latitude;
    Double longitude;
    boolean hasGps;
    String specialNotes;
    String preferredTimeNote;
    CustomerSource source;
    CustomerStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
