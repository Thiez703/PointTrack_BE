package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.customer.*;
import com.teco.pointtrack.entity.Customer;
import com.teco.pointtrack.service.CustomerImportService;
import com.teco.pointtrack.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Module CUSTOMER – Quản lý Khách hàng (DC-09)
 * Base path: /api/v1/customers
 */
@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Quản lý Khách hàng")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class CustomerController {

    private final CustomerService       customerService;
    private final CustomerImportService customerImportService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /customers — Danh sách KH (phân trang + lọc)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Danh sách khách hàng (phân trang + lọc theo status/source/GPS/keyword)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getCustomers(
            @ModelAttribute CustomerPageRequest request) {

        Page<CustomerResponse> page = customerService.getCustomers(request);
        return ResponseEntity.ok(ApiResponse.success(page, "Lấy danh sách khách hàng thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /customers/active-with-gps — Dropdown tạo ca
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Danh sách KH ACTIVE có GPS (dùng cho dropdown tạo ca)")
    @GetMapping("/active-with-gps")
    public ResponseEntity<ApiResponse<List<ActiveCustomerResponse>>> getActiveWithGps() {
        List<ActiveCustomerResponse> list = customerService.getActiveWithGps();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách khách hàng thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /customers/import/template — Download template Excel
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Tải template Excel để import khách hàng")
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] templateBytes = customerImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"customer_import_template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(templateBytes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /customers/export — Xuất Excel dữ liệu thật
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Xuất danh sách khách hàng ra file Excel (có lọc theo request)")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCustomers(
            @ModelAttribute CustomerPageRequest request) throws IOException {

        List<Customer> customers = customerService.getCustomersForExport(request);
        byte[] excelBytes = customerImportService.exportToExcel(customers);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"customers_export.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /customers/{id} — Chi tiết KH + thống kê
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Chi tiết khách hàng kèm thống kê và 10 ca gần nhất")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomerDetail(
            @PathVariable Long id) {

        CustomerDetailResponse detail = customerService.getCustomerDetail(id);
        return ResponseEntity.ok(ApiResponse.success(detail, "Lấy thông tin khách hàng thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /customers — Tạo KH mới
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Tạo khách hàng mới (auto-geocode địa chỉ)")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CustomerCreateRequest request) {

        CustomerCreateResult result  = customerService.createCustomer(request);
        CustomerResponse     data    = result.getCustomer();
        String               warning = result.getWarning();

        if (warning != null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.successWithWarning(data, "Tạo khách hàng thành công", warning));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Tạo khách hàng thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /customers/{id} — Cập nhật KH
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật khách hàng (re-geocode tự động nếu địa chỉ thay đổi)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequest request) {

        CustomerCreateResult result  = customerService.updateCustomer(id, request);
        CustomerResponse     data    = result.getCustomer();
        String               warning = result.getWarning();

        if (warning != null) {
            return ResponseEntity.ok(
                    ApiResponse.successWithWarning(data, "Cập nhật khách hàng thành công", warning));
        }
        return ResponseEntity.ok(ApiResponse.success(data, "Cập nhật khách hàng thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /customers/{id} — Soft delete + huỷ ca tương lai
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Vô hiệu hóa khách hàng (soft delete) và huỷ các ca SCHEDULED tương lai")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(@PathVariable Long id) {
        int cancelledShifts = customerService.deactivateCustomer(id);
        String message = cancelledShifts > 0
                ? String.format("Đã vô hiệu hóa KH. %d ca tương lai đã bị hủy.", cancelledShifts)
                : "Đã vô hiệu hóa khách hàng thành công.";
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /customers/{id}/gps — Cập nhật GPS thủ công
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật tọa độ GPS thủ công (khi auto-geocode thất bại)")
    @PutMapping("/{id}/gps")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateGps(
            @PathVariable Long id,
            @Valid @RequestBody GpsUpdateRequest request) {

        CustomerResponse updated = customerService.updateGps(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật GPS thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /customers/{id}/geocode — Re-geocode địa chỉ hiện tại
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Re-geocode địa chỉ hiện tại (hữu ích khi Google Maps API bị gián đoạn lúc tạo)")
    @PostMapping("/{id}/geocode")
    public ResponseEntity<ApiResponse<GeocodeResponse>> reGeocode(@PathVariable Long id) {
        GeocodeResponse result = customerService.reGeocode(id);
        return ResponseEntity.ok(ApiResponse.success(result,
                result.isSuccess() ? "Re-geocode thành công" : "Re-geocode thất bại"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /customers/import — Import Excel
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Import danh sách khách hàng từ file Excel (.xlsx)")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CustomerImportResult>> importCustomers(
            @RequestParam("file") MultipartFile file) {

        CustomerImportResult result = customerImportService.importFromExcel(file);
        String message = String.format("Import hoàn tất: %d thành công, %d thất bại",
                result.getSuccess(), result.getFailed());
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }
}
