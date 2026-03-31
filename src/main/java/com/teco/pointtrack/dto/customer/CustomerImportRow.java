package com.teco.pointtrack.dto.customer;

import com.teco.pointtrack.entity.enums.CustomerSource;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Dữ liệu một dòng Excel khi import khách hàng.
 *
 * <p>Cột Excel (theo thứ tự):
 * <pre>
 * 0 – Tên KH          (bắt buộc)
 * 1 – SĐT chính       (bắt buộc)
 * 2 – SĐT phụ         (tuỳ chọn)
 * 3 – Địa chỉ         (bắt buộc)
 * 4 – Ghi chú đặc biệt (tuỳ chọn)
 * 5 – Giờ ưa thích     (tuỳ chọn)
 * 6 – Nguồn KH         (tuỳ chọn – mặc định OTHER)
 * 7 – Latitude        (tuỳ chọn)
 * 8 – Longitude       (tuỳ chọn)
 * </pre>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerImportRow {

    int rowNumber;

    String name;
    String phone;
    String secondaryPhone;
    String address;
    String specialNotes;
    String preferredTimeNote;

    /** Đọc từ Excel, parse sang enum; null → OTHER */
    CustomerSource source;

    Double latitude;
    Double longitude;
}
