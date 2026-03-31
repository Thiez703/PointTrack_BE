package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.customer.*;
import com.teco.pointtrack.entity.Customer;
import com.teco.pointtrack.entity.Shift;
import com.teco.pointtrack.entity.enums.CustomerSource;
import com.teco.pointtrack.entity.enums.CustomerStatus;
import com.teco.pointtrack.entity.enums.ShiftStatus;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.CustomerRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    static final String GPS_WARNING =
            "Không thể xác định tọa độ GPS. Vui lòng nhập thủ công.";

    private final CustomerRepository customerRepository;
    private final GeocodingService   geocodingService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/customers — Danh sách KH (phân trang + lọc)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(CustomerPageRequest req) {
        Specification<Customer> spec = buildSpecification(req);
        
        // Mặc định sort theo createdAt DESC nếu không có sort từ client
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        
        Pageable pageable = PageRequest.of(
                req.getPage(), req.getSize(), sort);

        return customerRepository.findAll(spec, pageable).map(this::toResponse);
    }

    /**
     * Lấy toàn bộ danh sách khách hàng khớp với bộ lọc (không phân trang) để xuất Excel.
     */
    @Transactional(readOnly = true)
    public List<Customer> getCustomersForExport(CustomerPageRequest req) {
        Specification<Customer> spec = buildSpecification(req);
        return customerRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Specification<Customer> buildSpecification(CustomerPageRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Luôn loại bỏ KH đã soft-delete
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Tìm kiếm theo name, phone hoặc address (case-insensitive)
            String kw = req.resolvedKeyword();
            if (kw != null && !kw.isBlank()) {
                String pattern = "%" + kw.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")),    pattern),
                        cb.like(cb.lower(root.get("phone")),   pattern),
                        cb.like(cb.lower(root.get("address")), pattern)
                ));
            }

            // Lọc theo status
            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            } else if (req.getIsActive() != null) {
                CustomerStatus mappedStatus = Boolean.TRUE.equals(req.getIsActive())
                        ? CustomerStatus.ACTIVE : CustomerStatus.INACTIVE;
                predicates.add(cb.equal(root.get("status"), mappedStatus));
            }

            // Lọc theo nguồn KH
            if (req.getSource() != null) {
                predicates.add(cb.equal(root.get("source"), req.getSource()));
            }

            // Lọc theo có/không có GPS
            if (Boolean.TRUE.equals(req.getHasGps())) {
                predicates.add(cb.isNotNull(root.get("latitude")));
                predicates.add(cb.isNotNull(root.get("longitude")));
            } else if (Boolean.FALSE.equals(req.getHasGps())) {
                predicates.add(cb.or(
                        cb.isNull(root.get("latitude")),
                        cb.isNull(root.get("longitude"))));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/customers/{id} — Chi tiết KH + thống kê
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerDetail(Long id) {
        Customer customer = findActiveCustomer(id);

        // Thống kê
        long totalShifts      = customerRepository.countTotalShifts(id);
        long completedShifts  = customerRepository.countCompletedShifts(id);
        long activePackages   = customerRepository.countActivePackages(id);

        // 10 ca gần nhất
        List<Shift> recentShifts = customerRepository.findRecentShifts(
                id, PageRequest.of(0, 10));

        List<CustomerDetailResponse.RecentShift> recentShiftDtos = recentShifts.stream()
                .map(s -> CustomerDetailResponse.RecentShift.builder()
                        .shiftId(s.getId())
                        .employeeName(s.getEmployee().getFullName())
                        .shiftDate(s.getShiftDate())
                        .status(s.getStatus().name())
                        .build())
                .toList();

        return CustomerDetailResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .secondaryPhone(customer.getSecondaryPhone())
                .address(customer.getAddress())
                .latitude(customer.getLatitude())
                .longitude(customer.getLongitude())
                .hasGps(customer.getLatitude() != null && customer.getLongitude() != null)
                .specialNotes(customer.getSpecialNotes())
                .preferredTimeNote(customer.getPreferredTimeNote())
                .source(customer.getSource())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .stats(CustomerDetailResponse.CustomerStats.builder()
                        .totalShifts(totalShifts)
                        .completedShifts(completedShifts)
                        .activePackages(activePackages)
                        .totalLateCheckouts(0L)   // TODO: kết nối bảng attendance_logs khi có
                        .build())
                .recentShifts(recentShiftDtos)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/customers — Tạo KH mới
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo KH mới và tự động geocode địa chỉ.
     */
    @Transactional
    public CustomerCreateResult createCustomer(CustomerCreateRequest request) {

        // Kiểm tra phone trùng
        if (customerRepository.existsByPhoneAndDeletedAtIsNull(request.getPhone())) {
            throw new ConflictException("CUSTOMER_PHONE_CONFLICT");
        }

        Customer customer = Customer.builder()
                .name(request.getName().trim())
                .phone(request.getPhone())
                .secondaryPhone(blankToNull(request.getSecondaryPhone()))
                .address(request.getAddress().trim())
                .specialNotes(blankToNull(request.getSpecialNotes()))
                .preferredTimeNote(blankToNull(request.getPreferredTimeNote()))
                .source(request.getSource() != null ? request.getSource() : CustomerSource.OTHER)
                .status(CustomerStatus.ACTIVE)
                .build();

        String warning = null;

        if (request.getLatitude() != null && request.getLongitude() != null) {
            // Admin cung cấp tọa độ thủ công
            customer.setLatitude(request.getLatitude());
            customer.setLongitude(request.getLongitude());
        } else {
            // Tự động geocode
            GeoPoint geoPoint = geocodingService.geocode(request.getAddress());
            if (geoPoint != null) {
                customer.setLatitude(geoPoint.latitude());
                customer.setLongitude(geoPoint.longitude());
            } else {
                warning = GPS_WARNING;
                log.warn("Geocoding thất bại khi tạo KH: name={}", request.getName());
            }
        }

        Customer saved = customerRepository.save(customer);
        log.info("Tạo KH mới: id={} name={}", saved.getId(), saved.getName());

        return CustomerCreateResult.builder()
                .customer(toResponse(saved))
                .warning(warning)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/customers/{id} — Cập nhật KH
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public CustomerCreateResult updateCustomer(Long id, CustomerUpdateRequest request) {
        Customer customer = findActiveCustomer(id);

        // Kiểm tra phone trùng với KH khác
        if (customerRepository.existsByPhoneAndIdNotAndDeletedAtIsNull(request.getPhone(), id)) {
            throw new ConflictException("CUSTOMER_PHONE_CONFLICT");
        }

        boolean addressChanged = !Objects.equals(customer.getAddress(), request.getAddress());

        // Cập nhật fields
        customer.setName(request.getName().trim());
        customer.setPhone(request.getPhone());
        customer.setSecondaryPhone(blankToNull(request.getSecondaryPhone()));
        customer.setAddress(request.getAddress().trim());
        customer.setSpecialNotes(blankToNull(request.getSpecialNotes()));
        customer.setPreferredTimeNote(blankToNull(request.getPreferredTimeNote()));

        if (request.getSource() != null) customer.setSource(request.getSource());
        if (request.getStatus() != null) customer.setStatus(request.getStatus());

        String warning = null;

        if (request.getLatitude() != null && request.getLongitude() != null) {
            // Admin override tọa độ thủ công → bỏ qua geocoding
            customer.setLatitude(request.getLatitude());
            customer.setLongitude(request.getLongitude());
        } else if (addressChanged) {
            // Địa chỉ thay đổi → tự geocode lại
            GeoPoint geoPoint = geocodingService.geocode(request.getAddress());
            if (geoPoint != null) {
                customer.setLatitude(geoPoint.latitude());
                customer.setLongitude(geoPoint.longitude());
            } else {
                customer.setLatitude(null);
                customer.setLongitude(null);
                warning = GPS_WARNING;
                log.warn("Re-geocoding thất bại khi cập nhật KH id={}", id);
            }
        }

        customerRepository.save(customer);
        log.info("Cập nhật KH id={}", id);

        return CustomerCreateResult.builder()
                .customer(toResponse(customer))
                .warning(warning)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/customers/{id} — Soft delete + hủy ca tương lai
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Vô hiệu hóa KH (soft delete) và hủy tất cả ca SCHEDULED tương lai.
     *
     * @return số ca đã bị hủy
     */
    @Transactional
    public int deactivateCustomer(Long id) {
        Customer customer = findActiveCustomer(id);

        // Soft delete
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);

        // Hủy các ca SCHEDULED tương lai
        List<Shift> futureShifts = customerRepository.findFutureScheduledShifts(id, LocalDate.now());
        for (Shift shift : futureShifts) {
            shift.setStatus(ShiftStatus.CANCELLED);
        }
        // futureShifts được managed bởi Hibernate → dirty-checking sẽ tự UPDATE

        log.info("Soft-delete KH id={}, huỷ {} ca tương lai", id, futureShifts.size());
        return futureShifts.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/customers/{id}/gps — Cập nhật GPS thủ công
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponse updateGps(Long id, GpsUpdateRequest request) {
        Customer customer = findActiveCustomer(id);

        customer.setLatitude(request.getLatitude());
        customer.setLongitude(request.getLongitude());
        customerRepository.save(customer);

        log.info("Cập nhật GPS thủ công: KH id={} lat={} lng={}",
                id, request.getLatitude(), request.getLongitude());

        return toResponse(customer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/customers/{id}/geocode — Re-geocode từ địa chỉ hiện tại
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public GeocodeResponse reGeocode(Long id) {
        Customer customer = findActiveCustomer(id);

        if (customer.getAddress() == null || customer.getAddress().isBlank()) {
            throw new BadRequestException("Khách hàng chưa có địa chỉ để geocode");
        }

        GeocodingService.GeocodeFullResult result = geocodingService.geocodeFull(customer.getAddress());

        if (result == null) {
            return GeocodeResponse.builder()
                    .success(false)
                    .message(GPS_WARNING)
                    .build();
        }

        customer.setLatitude(result.latitude());
        customer.setLongitude(result.longitude());
        customerRepository.save(customer);

        log.info("Re-geocode KH id={}: lat={} lng={}", id, result.latitude(), result.longitude());

        return GeocodeResponse.builder()
                .success(true)
                .latitude(result.latitude())
                .longitude(result.longitude())
                .formattedAddress(result.formattedAddress())
                .message("Cập nhật tọa độ GPS thành công")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/customers/active-with-gps — Dùng cho dropdown tạo ca
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ActiveCustomerResponse> getActiveWithGps() {
        return customerRepository.findAllActiveWithGps().stream()
                .map(c -> ActiveCustomerResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .phone(c.getPhone())
                        .address(c.getAddress())
                        .latitude(c.getLatitude())
                        .longitude(c.getLongitude())
                        .build())
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    Customer findActiveCustomer(Long id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", id));
    }

    CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(c.getPhone())
                .secondaryPhone(c.getSecondaryPhone())
                .address(c.getAddress())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .hasGps(c.getLatitude() != null && c.getLongitude() != null)
                .specialNotes(c.getSpecialNotes())
                .preferredTimeNote(c.getPreferredTimeNote())
                .source(c.getSource())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
