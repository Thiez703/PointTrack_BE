package com.teco.pointtrack.dto.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponse {

    Long id;
    String name;
    String phoneNumber;
    String email;
    String street;
    String ward;
    String district;
    String city;
    Double latitude;
    Double longitude;
    String note;
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

