package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.common.MessageResponse;
import com.teco.pointtrack.dto.customer.CustomerPageRequest;
import com.teco.pointtrack.dto.customer.CustomerRequest;
import com.teco.pointtrack.dto.customer.CustomerResponse;
import com.teco.pointtrack.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Module CUSTOMER – Quản lý Khách hàng
 * Base path: /api/v1/customers  (DC-09)
 *
 * Endpoint map:
 *   GET    /api/v1/customers        — Danh sách khách hàng (phân trang + lọc)
 *   GET    /api/v1/customers/{id}   — Chi tiết khách hàng
 *   POST   /api/v1/customers        — Tạo khách hàng mới (201)
 *   PUT    /api/v1/customers/{id}   — Cập nhật khách hàng
 *   DELETE /api/v1/customers/{id}   — Xoá khách hàng (soft delete)
 */
@RestController
@RequestMapping({"/customers", "/v1/customers"})
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Quản lý Khách hàng")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class CustomerController {

    private final CustomerService customerService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/customers — Danh sách khách hàng
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách khách hàng (phân trang + lọc)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getCustomers(
            @ModelAttribute CustomerPageRequest request) {

        Page<CustomerResponse> page = customerService.getCustomers(request);
        return ResponseEntity.ok(
                ApiResponse.success(page, "Lấy danh sách khách hàng thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/customers/{id} — Chi tiết khách hàng
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy thông tin khách hàng theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id) {

        CustomerResponse detail = customerService.getCustomerById(id);
        return ResponseEntity.ok(
                ApiResponse.success(detail, "Lấy thông tin khách hàng thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/customers — Tạo khách hàng mới
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Tạo khách hàng mới")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CustomerRequest request) {

        CustomerResponse created = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Tạo khách hàng thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/customers/{id} — Cập nhật khách hàng
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật khách hàng")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {

        CustomerResponse updated = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(updated, "Cập nhật khách hàng thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/customers/{id} — Xoá khách hàng (soft delete)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Xoá khách hàng (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteCustomer(@PathVariable Long id) {

        customerService.deleteCustomer(id);
        return ResponseEntity.ok(
                ApiResponse.success(
                        new MessageResponse("Xoá khách hàng thành công"),
                        "Xoá khách hàng thành công"));
    }
}

