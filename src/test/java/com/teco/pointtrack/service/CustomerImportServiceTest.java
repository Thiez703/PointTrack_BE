package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.customer.CustomerImportRow;
import com.teco.pointtrack.entity.enums.CustomerSource;
import com.teco.pointtrack.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerImportServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock GeocodingService   geocodingService;

    @InjectMocks CustomerImportService service;

    private Set<String> seenPhones;

    @BeforeEach
    void setUp() {
        seenPhones = new HashSet<>();
    }

    // ── validateRow ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateRow: dòng hợp lệ → trả về null")
    void validateRow_valid_returnsNull() {
        when(customerRepository.existsByPhoneAndDeletedAtIsNull("0901234567")).thenReturn(false);

        String result = service.validateRow(buildRow("0901234567", "123 Test, Q1, TPHCM"), seenPhones);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("validateRow: tên trống → lỗi")
    void validateRow_emptyName_returnsError() {
        CustomerImportRow row = buildRow("0901234567", "123 Test");
        row.setName("  ");

        String result = service.validateRow(row, seenPhones);

        assertThat(result).contains("Thiếu tên");
    }

    @Test
    @DisplayName("validateRow: SĐT 9 chữ số → lỗi")
    void validateRow_phoneTooShort_returnsError() {
        CustomerImportRow row = buildRow("090123456", "123 Test");

        String result = service.validateRow(row, seenPhones);

        assertThat(result).contains("SĐT không hợp lệ");
    }

    @Test
    @DisplayName("validateRow: SĐT không bắt đầu 0 → lỗi")
    void validateRow_phoneNoLeadingZero_returnsError() {
        CustomerImportRow row = buildRow("1901234567", "123 Test");

        String result = service.validateRow(row, seenPhones);

        assertThat(result).contains("SĐT không hợp lệ");
    }

    @Test
    @DisplayName("validateRow: địa chỉ trống → lỗi")
    void validateRow_emptyAddress_returnsError() {
        CustomerImportRow row = buildRow("0901234567", "");

        String result = service.validateRow(row, seenPhones);

        assertThat(result).contains("Thiếu địa chỉ");
    }

    @Test
    @DisplayName("validateRow: SĐT trùng trong file (seenPhones) → lỗi")
    void validateRow_phoneDuplicateInFile_returnsError() {
        seenPhones.add("0901234567");
        CustomerImportRow row = buildRow("0901234567", "123 Test");

        String result = service.validateRow(row, seenPhones);

        assertThat(result).containsIgnoringCase("trùng");
    }

    @Test
    @DisplayName("validateRow: SĐT đã tồn tại trong DB → lỗi")
    void validateRow_phoneDuplicateInDb_returnsError() {
        when(customerRepository.existsByPhoneAndDeletedAtIsNull("0901234567")).thenReturn(true);
        CustomerImportRow row = buildRow("0901234567", "123 Test");

        String result = service.validateRow(row, seenPhones);

        assertThat(result).containsIgnoringCase("tồn tại");
    }

    @Test
    @DisplayName("validateRow: SĐT null → lỗi")
    void validateRow_nullPhone_returnsError() {
        CustomerImportRow row = buildRow(null, "123 Test");

        String result = service.validateRow(row, seenPhones);

        assertThat(result).contains("SĐT không hợp lệ");
    }

    @Test
    @DisplayName("validateRow: địa chỉ null → lỗi")
    void validateRow_nullAddress_returnsError() {
        CustomerImportRow row = buildRow("0901234567", null);

        String result = service.validateRow(row, seenPhones);

        assertThat(result).contains("Thiếu địa chỉ");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CustomerImportRow buildRow(String phone, String address) {
        CustomerImportRow row = new CustomerImportRow();
        row.setRowNumber(2);
        row.setName("Nguyễn Thị A");
        row.setPhone(phone);
        row.setAddress(address);
        row.setSource(CustomerSource.OTHER);
        return row;
    }
}
