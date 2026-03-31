package com.teco.pointtrack.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CustomerCreateResult {
    private CustomerResponse customer;
    private String warning; // null nếu không có cảnh báo
}
