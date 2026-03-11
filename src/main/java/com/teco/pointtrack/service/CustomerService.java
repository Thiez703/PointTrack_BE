package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.customer.CustomerPageRequest;
import com.teco.pointtrack.dto.customer.CustomerRequest;
import com.teco.pointtrack.dto.customer.CustomerResponse;
import com.teco.pointtrack.entity.Customer;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final GeocodingService geocodingService;

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy danh sách khách hàng (phân trang + lọc)
    // GET /api/v1/customers
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(CustomerPageRequest req) {

        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // BR-22: luôn loại bỏ customer đã soft delete
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Tìm kiếm theo name hoặc phoneNumber (case-insensitive)
            if (req.getSearch() != null && !req.getSearch().isBlank()) {
                String search = "%" + req.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), search),
                        cb.like(cb.lower(root.get("phoneNumber")), search)
                ));
            }

            // Lọc theo trạng thái
            if (req.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), req.getIsActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return customerRepository.findAll(spec, pageable).map(this::toResponse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy thông tin khách hàng theo ID
    // GET /api/v1/customers/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = findActiveCustomer(id);
        return toResponse(customer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tạo khách hàng mới
    // POST /api/v1/customers
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {

        // Kiểm tra phone trùng
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (customerRepository.existsByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())) {
                throw new ConflictException("CUSTOMER_PHONE_CONFLICT");
            }
        }

        // Kiểm tra email trùng
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (customerRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
                throw new ConflictException("CUSTOMER_EMAIL_CONFLICT");
            }
        }

        Customer customer = Customer.builder()
                .name(request.getName().trim())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .street(request.getStreet())
                .ward(request.getWard())
                .district(request.getDistrict())
                .city(request.getCity())
                .note(request.getNote())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        // Geocode địa chỉ
        double[] coords = geocodingService.geocode(
                request.getStreet(), request.getWard(), request.getDistrict(), request.getCity());
        if (coords != null) {
            customer.setLatitude(coords[0]);
            customer.setLongitude(coords[1]);
        }

        Customer saved = customerRepository.save(customer);
        log.info("Created customer: name={}", saved.getName());

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cập nhật khách hàng
    // PUT /api/v1/customers/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = findActiveCustomer(id);

        // Kiểm tra phone trùng với customer khác
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (customerRepository.existsByPhoneNumberAndIdNotAndDeletedAtIsNull(request.getPhoneNumber(), id)) {
                throw new ConflictException("CUSTOMER_PHONE_CONFLICT");
            }
        }

        // Kiểm tra email trùng với customer khác
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (customerRepository.existsByEmailAndIdNotAndDeletedAtIsNull(request.getEmail(), id)) {
                throw new ConflictException("CUSTOMER_EMAIL_CONFLICT");
            }
        }

        // Kiểm tra xem address có thay đổi không
        boolean addressChanged = !Objects.equals(customer.getStreet(), request.getStreet())
                || !Objects.equals(customer.getWard(), request.getWard())
                || !Objects.equals(customer.getDistrict(), request.getDistrict())
                || !Objects.equals(customer.getCity(), request.getCity());

        // Cập nhật tất cả field
        customer.setName(request.getName().trim());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setEmail(request.getEmail());
        customer.setStreet(request.getStreet());
        customer.setWard(request.getWard());
        customer.setDistrict(request.getDistrict());
        customer.setCity(request.getCity());
        customer.setNote(request.getNote());
        if (request.getIsActive() != null) {
            customer.setIsActive(request.getIsActive());
        }

        // Geocode lại nếu address thay đổi
        if (addressChanged) {
            double[] coords = geocodingService.geocode(
                    request.getStreet(), request.getWard(), request.getDistrict(), request.getCity());
            if (coords != null) {
                customer.setLatitude(coords[0]);
                customer.setLongitude(coords[1]);
            } else {
                customer.setLatitude(null);
                customer.setLongitude(null);
            }
        }

        customerRepository.save(customer);
        return toResponse(customer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Xoá khách hàng (soft delete — BR-22)
    // DELETE /api/v1/customers/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = findActiveCustomer(id);

        // BR-22: soft delete
        customer.setDeletedAt(LocalDateTime.now());
        customer.setIsActive(false);
        customerRepository.save(customer);

        log.info("Soft-deleted customer id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Customer findActiveCustomer(Long id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", id));
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .phoneNumber(c.getPhoneNumber())
                .email(c.getEmail())
                .street(c.getStreet())
                .ward(c.getWard())
                .district(c.getDistrict())
                .city(c.getCity())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .note(c.getNote())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}

