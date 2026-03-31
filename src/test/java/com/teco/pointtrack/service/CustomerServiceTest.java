package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.customer.CustomerCreateRequest;
import com.teco.pointtrack.dto.customer.CustomerCreateResult;
import com.teco.pointtrack.dto.customer.CustomerResponse;
import com.teco.pointtrack.dto.customer.GeoPoint;
import com.teco.pointtrack.entity.Customer;
import com.teco.pointtrack.entity.Shift;
import com.teco.pointtrack.entity.enums.CustomerSource;
import com.teco.pointtrack.entity.enums.CustomerStatus;
import com.teco.pointtrack.entity.enums.ShiftStatus;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock GeocodingService   geocodingService;

    @InjectMocks CustomerService customerService;

    private Customer activeCustomer;

    @BeforeEach
    void setUp() {
        activeCustomer = Customer.builder()
                .name("Nguyễn Thị A")
                .phone("0901234567")
                .address("123 Test St, Q1, TPHCM")
                .source(CustomerSource.ZALO)
                .status(CustomerStatus.ACTIVE)
                .build();
        // Set id via reflection workaround — use spy/builder with id
    }

    // ── createCustomer ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createCustomer: geocoding thành công → không có warning")
    void createCustomer_geocodeSuccess_noWarning() {
        CustomerCreateRequest req = buildCreateRequest("0901234567", "123 Nguyễn Văn A, Q1, TPHCM");

        when(customerRepository.existsByPhoneAndDeletedAtIsNull(req.getPhone())).thenReturn(false);
        when(geocodingService.geocode(anyString())).thenReturn(new GeoPoint(10.776, 106.700));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerCreateResult result  = customerService.createCustomer(req);
        CustomerResponse     response = result.getCustomer();
        String               warning  = result.getWarning();

        assertThat(warning).isNull();
        assertThat(response.getPhone()).isEqualTo("0901234567");
    }

    @Test
    @DisplayName("createCustomer: geocoding thất bại → warning không null, vẫn lưu")
    void createCustomer_geocodeFails_returnsWarning() {
        CustomerCreateRequest req = buildCreateRequest("0901234567", "Địa chỉ không tìm thấy");

        when(customerRepository.existsByPhoneAndDeletedAtIsNull(req.getPhone())).thenReturn(false);
        when(geocodingService.geocode(anyString())).thenReturn(null);
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerCreateResult result  = customerService.createCustomer(req);
        CustomerResponse     response = result.getCustomer();
        String               warning  = result.getWarning();

        assertThat(warning).isNotBlank();
        assertThat(warning).contains("GPS");
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("createCustomer: Admin cung cấp lat/lng thủ công → bỏ qua geocoding")
    void createCustomer_manualGps_skipsGeocode() {
        CustomerCreateRequest req = buildCreateRequest("0901234567", "123 Test");
        req.setLatitude(10.776);
        req.setLongitude(106.700);

        when(customerRepository.existsByPhoneAndDeletedAtIsNull(req.getPhone())).thenReturn(false);
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerCreateResult result = customerService.createCustomer(req);

        verify(geocodingService, never()).geocode(anyString());
        assertThat(result.getWarning()).isNull(); // no warning
    }

    @Test
    @DisplayName("createCustomer: phone đã tồn tại → ConflictException")
    void createCustomer_phoneDuplicate_throwsConflict() {
        CustomerCreateRequest req = buildCreateRequest("0901234567", "123 Test");

        when(customerRepository.existsByPhoneAndDeletedAtIsNull(req.getPhone())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(req))
                .isInstanceOf(ConflictException.class);
    }

    // ── deactivateCustomer ────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateCustomer: có 3 ca SCHEDULED tương lai → hủy 3 ca, trả về 3")
    void deactivateCustomer_cancelsFutureShifts() {
        Long customerId = 1L;

        Customer customer = Customer.builder()
                .name("Test KH")
                .phone("0901234567")
                .status(CustomerStatus.ACTIVE)
                .build();

        Shift s1 = Shift.builder().status(ShiftStatus.SCHEDULED).build();
        Shift s2 = Shift.builder().status(ShiftStatus.SCHEDULED).build();
        Shift s3 = Shift.builder().status(ShiftStatus.SCHEDULED).build();

        when(customerRepository.findByIdAndDeletedAtIsNull(customerId))
                .thenReturn(Optional.of(customer));
        when(customerRepository.findFutureScheduledShifts(eq(customerId), any(LocalDate.class)))
                .thenReturn(List.of(s1, s2, s3));

        int cancelled = customerService.deactivateCustomer(customerId);

        assertThat(cancelled).isEqualTo(3);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        assertThat(customer.getDeletedAt()).isNotNull();
        assertThat(s1.getStatus()).isEqualTo(ShiftStatus.CANCELLED);
        assertThat(s2.getStatus()).isEqualTo(ShiftStatus.CANCELLED);
        assertThat(s3.getStatus()).isEqualTo(ShiftStatus.CANCELLED);

        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("deactivateCustomer: không có ca tương lai → trả về 0")
    void deactivateCustomer_noFutureShifts_returnsZero() {
        Long customerId = 1L;
        Customer customer = Customer.builder()
                .name("Test KH")
                .phone("0901234567")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerRepository.findByIdAndDeletedAtIsNull(customerId))
                .thenReturn(Optional.of(customer));
        when(customerRepository.findFutureScheduledShifts(eq(customerId), any(LocalDate.class)))
                .thenReturn(List.of());

        int cancelled = customerService.deactivateCustomer(customerId);

        assertThat(cancelled).isEqualTo(0);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
    }

    @Test
    @DisplayName("deactivateCustomer: KH không tồn tại → NotFoundException")
    void deactivateCustomer_notFound_throwsNotFoundException() {
        when(customerRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.deactivateCustomer(99L))
                .isInstanceOf(NotFoundException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CustomerCreateRequest buildCreateRequest(String phone, String address) {
        CustomerCreateRequest req = new CustomerCreateRequest();
        req.setName("Nguyễn Thị A");
        req.setPhone(phone);
        req.setAddress(address);
        req.setSource(CustomerSource.ZALO);
        return req;
    }
}
