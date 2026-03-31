# API Đặc Tả – Module Quản Lý Khách Hàng

**Base URL:** `http://localhost:8080/api`
**Auth:** Bearer Token (JWT) — gửi qua header `Authorization: Bearer <token>`
**Quyền truy cập:** Tất cả endpoint trong module này yêu cầu role `ADMIN`

---

## Mục Lục

1. [Danh sách khách hàng (phân trang + lọc)](#1-get-apiv1customers)
2. [Chi tiết khách hàng](#2-get-apiv1customersid)
3. [Tạo khách hàng mới](#3-post-apiv1customers)
4. [Cập nhật khách hàng](#4-put-apiv1customersid)
5. [Vô hiệu hóa khách hàng (soft delete)](#5-delete-apiv1customersid)
6. [Cập nhật GPS thủ công](#6-put-apiv1customersidgps)
7. [Re-geocode địa chỉ](#7-post-apiv1customersidgeocode)
8. [Danh sách KH ACTIVE có GPS (dropdown)](#8-get-apiv1customersactive-with-gps)
9. [Import Excel](#9-post-apiv1customersimport)
10. [Tải template Excel](#10-get-apiv1customersimporttemplate)
11. [Xuất Excel dữ liệu thật](#11-get-apiv1customersexport)
12. [Enum Reference](#12-enum-reference)
13. [Error Codes](#13-error-codes)

---

## Cấu Trúc Response Chung

### Success (không có warning)
```json
{
  "success": true,
  "data": { ... },
  "message": "Thông báo thành công"
}
```

### Success (có warning — VD: geocoding thất bại)
```json
{
  "success": true,
  "data": { ... },
  "message": "Tạo khách hàng thành công",
  "warning": "Không thể xác định tọa độ GPS. Vui lòng nhập thủ công."
}
```

### Error
```json
{
  "statusCode": "404",
  "title": "Not Found",
  "detail": "Không tìm thấy khách hàng (id=99)",
  "fieldErrors": [],
  "errorCode": null
}
```

### Validation Error (400)
```json
{
  "statusCode": "400",
  "title": "Bad Request",
  "detail": "Dữ liệu validation không hợp lệ",
  "fieldErrors": [
    "phone phải 10 chữ số và bắt đầu bằng 0",
    "address Địa chỉ không được để trống"
  ]
}
```

---

## 1. GET /api/v1/customers

Lấy danh sách khách hàng có phân trang và bộ lọc.

### Query Parameters

| Tham số   | Kiểu     | Bắt buộc | Mô tả |
|-----------|----------|----------|-------|
| `keyword` | `string` | Không | Tìm kiếm theo **tên** hoặc **SĐT** (không phân biệt hoa/thường) |
| `status`  | `string` | Không | Lọc theo trạng thái: `ACTIVE` \| `INACTIVE` \| `SUSPENDED` |
| `source`  | `string` | Không | Lọc theo nguồn: `ZALO` \| `FACEBOOK` \| `REFERRAL` \| `HOTLINE` \| `OTHER` |
| `hasGps`  | `boolean` | Không | `true` = chỉ KH có GPS · `false` = chỉ KH chưa có GPS |
| `page`    | `integer` | Không | Trang hiện tại, bắt đầu từ `0` (mặc định: `0`) |
| `size`    | `integer` | Không | Số bản ghi mỗi trang (mặc định: `20`) |

### Request Example
```
GET /api/v1/customers?status=ACTIVE&keyword=nguyễn&hasGps=false&page=0&size=20
Authorization: Bearer eyJhbGci...
```

### Response `200 OK`
```json
{
  "success": true,
  "message": "Lấy danh sách khách hàng thành công",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Nguyễn Thị A",
        "phone": "0901234567",
        "secondaryPhone": "0987654321",
        "address": "123 Nguyễn Văn B, Phường 1, Quận 1, TP.HCM",
        "latitude": 10.7769,
        "longitude": 106.7009,
        "hasGps": true,
        "specialNotes": "Có chó to, bấm chuông 2 lần",
        "preferredTimeNote": "Buổi sáng 8-10h",
        "source": "ZALO",
        "status": "ACTIVE",
        "createdAt": "2026-03-22T08:00:00",
        "updatedAt": "2026-03-22T10:30:00"
      }
    ],
    "totalElements": 150,
    "totalPages": 8,
    "size": 20,
    "number": 0,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

> **Lưu ý:** Các field có giá trị `null` sẽ bị bỏ qua trong response (không serialize).

---

## 2. GET /api/v1/customers/{id}

Lấy thông tin chi tiết của một khách hàng, kèm thống kê và 10 ca làm việc gần nhất.

### Path Parameters

| Tham số | Kiểu      | Mô tả |
|---------|-----------|-------|
| `id`    | `integer` | ID của khách hàng |

### Request Example
```
GET /api/v1/customers/1
Authorization: Bearer eyJhbGci...
```

### Response `200 OK`
```json
{
  "success": true,
  "message": "Lấy thông tin khách hàng thành công",
  "data": {
    "id": 1,
    "name": "Nguyễn Thị A",
    "phone": "0901234567",
    "secondaryPhone": "0987654321",
    "address": "123 Nguyễn Văn B, Phường 1, Quận 1, TP.HCM",
    "latitude": 10.7769,
    "longitude": 106.7009,
    "hasGps": true,
    "specialNotes": "Có chó to, bấm chuông 2 lần",
    "preferredTimeNote": "Buổi sáng 8-10h",
    "source": "ZALO",
    "status": "ACTIVE",
    "createdAt": "2026-03-22T08:00:00",
    "updatedAt": "2026-03-22T10:30:00",
    "stats": {
      "totalShifts": 25,
      "completedShifts": 20,
      "activePackages": 2,
      "totalLateCheckouts": 3
    },
    "recentShifts": [
      {
        "shiftId": 101,
        "employeeName": "Trần Văn B",
        "shiftDate": "2026-03-20",
        "status": "COMPLETED"
      },
      {
        "shiftId": 99,
        "employeeName": "Trần Văn B",
        "shiftDate": "2026-03-18",
        "status": "COMPLETED"
      }
    ]
  }
}
```

### Response `404 Not Found`
```json
{
  "statusCode": "404",
  "title": "Not Found",
  "detail": "Không tìm thấy khách hàng (id=99)"
}
```

---

## 3. POST /api/v1/customers

Tạo khách hàng mới. Hệ thống sẽ tự động geocode địa chỉ để lấy tọa độ GPS.

### Request Body

```json
{
  "name": "Nguyễn Thị A",
  "phone": "0901234567",
  "secondaryPhone": "0987654321",
  "address": "123 Nguyễn Văn B, Phường 1, Quận 1, TP.HCM",
  "specialNotes": "Có chó to, bấm chuông 2 lần",
  "preferredTimeNote": "Buổi sáng 8-10h",
  "source": "ZALO",
  "latitude": null,
  "longitude": null
}
```

### Field Rules

| Field | Kiểu | Bắt buộc | Validation |
|-------|------|----------|------------|
| `name` | `string` | ✅ | Tối đa 255 ký tự |
| `phone` | `string` | ✅ | Đúng 10 chữ số, bắt đầu bằng `0` — VD: `0901234567` |
| `secondaryPhone` | `string` | Không | Đúng 10 chữ số, bắt đầu bằng `0`, hoặc `null`/`""` |
| `address` | `string` | ✅ | Địa chỉ đầy đủ dạng chuỗi |
| `specialNotes` | `string` | Không | Ghi chú đặc biệt cho nhân viên |
| `preferredTimeNote` | `string` | Không | Tối đa 255 ký tự |
| `source` | `string` | Không | Xem [Enum Reference](#11-enum-reference). Mặc định: `OTHER` |
| `latitude` | `number` | Không | `-90.0` đến `90.0`. Nếu cung cấp cả `latitude` + `longitude` → **bỏ qua geocoding** |
| `longitude` | `number` | Không | `-180.0` đến `180.0` |

> **Geocoding logic:**
> - Nếu `latitude` + `longitude` = `null` → hệ thống tự geocode từ `address`
> - Nếu geocoding thành công → lưu tọa độ, không có `warning`
> - Nếu geocoding thất bại → lưu KH với `latitude`/`longitude` = `null`, trả về `warning`
> - Nếu Admin cung cấp `latitude` + `longitude` → dùng trực tiếp, bỏ qua Google Maps

### Response `201 Created` — Geocoding thành công
```json
{
  "success": true,
  "message": "Tạo khách hàng thành công",
  "data": {
    "id": 42,
    "name": "Nguyễn Thị A",
    "phone": "0901234567",
    "secondaryPhone": "0987654321",
    "address": "123 Nguyễn Văn B, Phường 1, Quận 1, TP.HCM",
    "latitude": 10.7769,
    "longitude": 106.7009,
    "hasGps": true,
    "source": "ZALO",
    "status": "ACTIVE",
    "createdAt": "2026-03-22T09:00:00",
    "updatedAt": "2026-03-22T09:00:00"
  }
}
```

### Response `201 Created` — Geocoding thất bại (vẫn tạo được KH)
```json
{
  "success": true,
  "message": "Tạo khách hàng thành công",
  "warning": "Không thể xác định tọa độ GPS. Vui lòng nhập thủ công.",
  "data": {
    "id": 43,
    "name": "Nguyễn Thị A",
    "phone": "0901234567",
    "address": "123 Nguyễn Văn B, Phường 1, Quận 1, TP.HCM",
    "hasGps": false,
    "source": "ZALO",
    "status": "ACTIVE",
    "createdAt": "2026-03-22T09:00:00",
    "updatedAt": "2026-03-22T09:00:00"
  }
}
```

> **FE nên:** Khi `warning` có giá trị → hiển thị banner cảnh báo màu vàng và gợi ý Admin cập nhật GPS thủ công qua endpoint [PUT /gps](#6-put-apiv1customersidgps).

### Response `400 Bad Request` — Validation lỗi
```json
{
  "statusCode": "400",
  "title": "Bad Request",
  "detail": "Dữ liệu validation không hợp lệ",
  "fieldErrors": [
    "phone phải 10 chữ số và bắt đầu bằng 0",
    "address Địa chỉ không được để trống"
  ]
}
```

### Response `409 Conflict` — SĐT đã tồn tại
```json
{
  "statusCode": "409",
  "title": "Conflict",
  "detail": "Số điện thoại đã được sử dụng bởi khách hàng khác",
  "errorCode": "CUSTOMER_PHONE_CONFLICT"
}
```

---

## 4. PUT /api/v1/customers/{id}

Cập nhật thông tin khách hàng.

### Path Parameters

| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| `id` | `integer` | ID khách hàng |

### Request Body

Giống [POST /api/v1/customers](#3-post-apiv1customers), nhưng có thêm field `status`:

```json
{
  "name": "Nguyễn Thị A (đã sửa)",
  "phone": "0901234567",
  "secondaryPhone": null,
  "address": "456 Lê Văn C, Phường 5, Quận 3, TP.HCM",
  "specialNotes": "Ghi chú mới",
  "preferredTimeNote": "Buổi chiều 14-16h",
  "source": "FACEBOOK",
  "status": "ACTIVE",
  "latitude": null,
  "longitude": null
}
```

| Field | Kiểu | Bắt buộc | Ghi chú |
|-------|------|----------|---------|
| `status` | `string` | Không | `ACTIVE` \| `INACTIVE` \| `SUSPENDED` |
| Các field còn lại | | | Giống POST |

> **Re-geocoding logic:**
> - Nếu `address` **thay đổi** và `latitude`/`longitude` = `null` → tự động geocode lại địa chỉ mới
> - Nếu `latitude` + `longitude` được cung cấp → dùng trực tiếp, **bỏ qua geocoding**
> - Nếu `address` **không đổi** → giữ nguyên GPS cũ

### Response `200 OK`
```json
{
  "success": true,
  "message": "Cập nhật khách hàng thành công",
  "data": {
    "id": 1,
    "name": "Nguyễn Thị A (đã sửa)",
    "phone": "0901234567",
    "address": "456 Lê Văn C, Phường 5, Quận 3, TP.HCM",
    "latitude": 10.7800,
    "longitude": 106.6950,
    "hasGps": true,
    "source": "FACEBOOK",
    "status": "ACTIVE",
    "updatedAt": "2026-03-22T11:00:00"
  }
}
```

### Response `200 OK` — Geocoding lại thất bại
```json
{
  "success": true,
  "message": "Cập nhật khách hàng thành công",
  "warning": "Không thể xác định tọa độ GPS. Vui lòng nhập thủ công.",
  "data": {
    "id": 1,
    "hasGps": false,
    ...
  }
}
```

---

## 5. DELETE /api/v1/customers/{id}

Vô hiệu hóa khách hàng (soft delete). Tất cả ca làm việc `SCHEDULED` trong tương lai sẽ bị **hủy tự động**.

> Khách hàng **không bao giờ bị xóa vật lý** khỏi database.

### Path Parameters

| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| `id` | `integer` | ID khách hàng |

### Request Example
```
DELETE /api/v1/customers/1
Authorization: Bearer eyJhbGci...
```

### Response `200 OK`
```json
{
  "success": true,
  "message": "Đã vô hiệu hóa KH. 3 ca tương lai đã bị hủy."
}
```

Nếu không có ca nào bị hủy:
```json
{
  "success": true,
  "message": "Đã vô hiệu hóa khách hàng thành công."
}
```

> **FE nên:** Parse số ca bị hủy từ message hoặc hiển thị confirm dialog trước khi gọi API.

---

## 6. PUT /api/v1/customers/{id}/gps

Cập nhật tọa độ GPS thủ công cho khách hàng. Dùng khi auto-geocoding thất bại.

### Path Parameters

| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| `id` | `integer` | ID khách hàng |

### Request Body
```json
{
  "latitude": 10.7769,
  "longitude": 106.7009
}
```

| Field | Kiểu | Bắt buộc | Validation |
|-------|------|----------|------------|
| `latitude` | `number` | ✅ | `-90.0` đến `90.0` |
| `longitude` | `number` | ✅ | `-180.0` đến `180.0` |

### Response `200 OK`
```json
{
  "success": true,
  "message": "Cập nhật GPS thành công",
  "data": {
    "id": 1,
    "name": "Nguyễn Thị A",
    "phone": "0901234567",
    "address": "123 Nguyễn Văn B, Phường 1, Quận 1, TP.HCM",
    "latitude": 10.7769,
    "longitude": 106.7009,
    "hasGps": true,
    "status": "ACTIVE",
    "updatedAt": "2026-03-22T11:30:00"
  }
}
```

---

## 7. POST /api/v1/customers/{id}/geocode

Trigger re-geocoding từ địa chỉ hiện tại của khách hàng. Hữu ích khi Google Maps API bị gián đoạn lúc tạo KH.

### Path Parameters

| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| `id` | `integer` | ID khách hàng |

### Request Example
```
POST /api/v1/customers/1/geocode
Authorization: Bearer eyJhbGci...
```
*(Không có request body)*

### Response `200 OK` — Thành công
```json
{
  "success": true,
  "message": "Re-geocode thành công",
  "data": {
    "success": true,
    "latitude": 10.7769,
    "longitude": 106.7009,
    "formattedAddress": "123 Nguyễn Văn B, Phường Bến Nghé, Quận 1, Thành phố Hồ Chí Minh, Việt Nam",
    "message": "Cập nhật tọa độ GPS thành công"
  }
}
```

### Response `200 OK` — Geocode thất bại (Google Maps không tìm thấy địa chỉ)
```json
{
  "success": true,
  "message": "Re-geocode thất bại",
  "data": {
    "success": false,
    "message": "Không thể xác định tọa độ GPS. Vui lòng nhập thủ công."
  }
}
```

> **FE nên:** Kiểm tra `data.success` để biết geocoding có thành công hay không.

---

## 8. GET /api/v1/customers/active-with-gps

Lấy danh sách tất cả khách hàng đang `ACTIVE` và **có tọa độ GPS**. Dùng cho dropdown khi tạo ca làm việc.

> **Không phân trang** — trả về toàn bộ danh sách (thường < 500 records).

### Request Example
```
GET /api/v1/customers/active-with-gps
Authorization: Bearer eyJhbGci...
```

### Response `200 OK`
```json
{
  "success": true,
  "message": "Lấy danh sách khách hàng thành công",
  "data": [
    {
      "id": 1,
      "name": "Nguyễn Thị A",
      "phone": "0901234567",
      "address": "123 Nguyễn Văn B, Phường 1, Quận 1, TP.HCM",
      "latitude": 10.7769,
      "longitude": 106.7009
    },
    {
      "id": 2,
      "name": "Trần Thị B",
      "phone": "0912345678",
      "address": "456 Lê Văn C, Phường 5, Quận 3, TP.HCM",
      "latitude": 10.7800,
      "longitude": 106.6950
    }
  ]
}
```

---

## 9. POST /api/v1/customers/import

Import danh sách khách hàng từ file Excel (`.xlsx`).

### Request

- **Content-Type:** `multipart/form-data`
- **Field:** `file` (file `.xlsx`, tối đa 10MB)

```
POST /api/v1/customers/import
Authorization: Bearer eyJhbGci...
Content-Type: multipart/form-data

file: <customers.xlsx>
```

### Cấu trúc file Excel

| Cột | Tiêu đề | Bắt buộc | Ghi chú |
|-----|---------|----------|---------|
| A | Tên KH | ✅ | |
| B | SĐT chính | ✅ | 10 chữ số, bắt đầu `0` |
| C | SĐT phụ | Không | |
| D | Địa chỉ | ✅ | |
| E | Ghi chú đặc biệt | Không | |
| F | Giờ ưa thích | Không | |
| G | Nguồn KH | Không | `ZALO`/`FACEBOOK`/`REFERRAL`/`HOTLINE`/`OTHER`. Mặc định: `OTHER` |

> Dòng đầu tiên là **header**, dữ liệu bắt đầu từ **dòng 2**.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Import hoàn tất: 38 thành công, 2 thất bại",
  "data": {
    "success": 38,
    "failed": 2,
    "noGps": 5,
    "errors": [
      {
        "row": 3,
        "reason": "Thiếu địa chỉ"
      },
      {
        "row": 8,
        "reason": "SĐT không hợp lệ (phải 10 chữ số, bắt đầu bằng 0)"
      }
    ],
    "warnings": [
      {
        "row": 5,
        "name": "Nguyễn Thị B",
        "reason": "Không tìm được tọa độ GPS"
      },
      {
        "row": 12,
        "name": "Lê Văn C",
        "reason": "Không tìm được tọa độ GPS"
      }
    ]
  }
}
```

### Giải thích response

| Field | Ý nghĩa |
|-------|---------|
| `success` | Số dòng import thành công |
| `failed` | Số dòng bị bỏ qua do lỗi validation (không import) |
| `noGps` | Số dòng import được nhưng geocoding thất bại (có trong `warnings`) |
| `errors[]` | Danh sách dòng bị lỗi — `row` là số thứ tự dòng trong Excel (bắt đầu từ 2) |
| `warnings[]` | Dòng import được nhưng cần chú ý (VD: không có GPS) |

> **FE nên:** Hiển thị bảng tổng kết với tab `Lỗi` và tab `Cảnh báo`. Cho phép download lại danh sách lỗi.

### Response `400 Bad Request` — File không hợp lệ
```json
{
  "statusCode": "400",
  "title": "Bad Request",
  "detail": "File import không hợp lệ (phải là .xlsx và không được rỗng)"
}
```

### Response `400 Bad Request` — File quá lớn (> 10MB)
```json
{
  "statusCode": "400",
  "title": "File Too Large",
  "detail": "Kích thước file vượt quá giới hạn cho phép (tối đa 10MB)"
}
```

---

## 10. GET /api/v1/customers/import/template

Tải file Excel template mẫu để import khách hàng.

### Request Example
```
GET /api/v1/customers/import/template
Authorization: Bearer eyJhbGci...
```

### Response

- **Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Content-Disposition:** `attachment; filename="customer_import_template.xlsx"`
- **Body:** Binary (file `.xlsx`)

> **FE:** Dùng `window.open(url)` hoặc tạo thẻ `<a href="..." download>` để tải file tự động.

---

## 11. GET /api/v1/customers/export

Xuất danh sách khách hàng ra file Excel (.xlsx). Hỗ trợ đầy đủ bộ lọc giống [GET /api/v1/customers](#1-get-apiv1customers).

### Query Parameters

Giống hệt endpoint lấy danh sách: `keyword`, `status`, `source`, `hasGps`.
*Lưu ý: Không dùng `page` và `size` vì xuất toàn bộ dữ liệu khớp lọc.*

### Request Example
```
GET /api/v1/customers/export?status=ACTIVE&source=ZALO
Authorization: Bearer eyJhbGci...
```

### Response

- **Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Content-Disposition:** `attachment; filename="customers_export.xlsx"`
- **Body:** Binary (file `.xlsx`)

---

## 12. Enum Reference


### CustomerStatus

| Giá trị | Ý nghĩa | Có thể gán ca? |
|---------|---------|---------------|
| `ACTIVE` | Đang hoạt động bình thường | ✅ |
| `INACTIVE` | Đã vô hiệu hóa | ❌ |
| `SUSPENDED` | Tạm ngưng, chờ Admin kích hoạt | ❌ |

### CustomerSource

| Giá trị | Ý nghĩa |
|---------|---------|
| `ZALO` | Từ kênh Zalo |
| `FACEBOOK` | Từ Facebook |
| `REFERRAL` | Được giới thiệu |
| `HOTLINE` | Qua đường dây nóng |
| `OTHER` | Khác (mặc định) |

---

## 12. Error Codes

| HTTP | ErrorCode | Mô tả |
|------|-----------|-------|
| `404` | — | Không tìm thấy khách hàng |
| `409` | `CUSTOMER_PHONE_CONFLICT` | SĐT đã tồn tại trong hệ thống |
| `400` | `IMPORT_FILE_INVALID` | File import không phải `.xlsx` hoặc rỗng |
| `400` | — | Validation lỗi (field `fieldErrors` chứa chi tiết) |
| `401` | — | Chưa đăng nhập / token hết hạn |
| `403` | — | Không đủ quyền (không phải ADMIN) |

---

## Ghi Chú Chung cho FE

### 1. Xử lý field `warning`
Khi response có field `warning` (không null):
- Hiển thị **banner cảnh báo màu vàng** phía trên form chi tiết
- Hiển thị nút **"Cập nhật GPS thủ công"** → mở modal nhập `latitude`, `longitude`
- Sau khi nhập GPS → gọi `PUT /api/v1/customers/{id}/gps`

### 2. Xử lý `hasGps`
- `hasGps: true` → hiển thị preview bản đồ (Google Maps Embed / Leaflet)
- `hasGps: false` → hiển thị cảnh báo "Chưa có tọa độ GPS" + nút re-geocode hoặc nhập thủ công

### 3. Xử lý trạng thái KH
- `ACTIVE` → badge xanh lá
- `INACTIVE` → badge xám
- `SUSPENDED` → badge cam/vàng

### 4. Phân trang (Pagination)
Response danh sách trả về object kiểu `Page` của Spring:
```
data.content[]       — mảng dữ liệu
data.totalElements   — tổng số bản ghi
data.totalPages      — tổng số trang
data.number          — trang hiện tại (0-based)
data.size            — số bản ghi/trang
data.first           — có phải trang đầu?
data.last            — có phải trang cuối?
```

### 5. Import Excel — UX gợi ý
```
1. Nút "Tải template" → GET /import/template
2. User điền dữ liệu vào template
3. Nút "Chọn file" → upload → POST /import
4. Hiển thị dialog kết quả:
   - Tab "Tổng kết": success=38, failed=2, noGps=5
   - Tab "Danh sách lỗi": bảng row/reason
   - Tab "Cảnh báo GPS": bảng row/name/reason (cần cập nhật GPS sau)
```

### 6. Active-with-GPS endpoint
Endpoint `GET /active-with-gps` dùng riêng cho **dropdown tạo ca làm việc** — trả về list phẳng (không phân trang), format nhỏ gọn hơn list chính.

---

*Cập nhật lần cuối: 2026-03-22 | Backend: PointTrack BE v0.0.1*
