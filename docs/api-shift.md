# API Đặc Tả – Module Quản Lý Ca Làm Việc (Shift)

**Base URL:** `http://localhost:8080/api`
**Auth:** Bearer Token (JWT) — gửi qua header `Authorization: Bearer <token>`

---

## Mục Lục

1. [Cấu trúc Response chung](#cấu-trúc-response-chung)
2. [Vòng đời trạng thái ca (State Machine)](#vòng-đời-trạng-thái-ca-state-machine)
3. [Schemas dùng chung](#schemas-dùng-chung)
4. [Lấy danh sách ca làm việc](#1-get-apiv1shifts)
5. [Tạo ca đơn lẻ](#2-post-apiv1shifts)
6. [Cập nhật ca làm việc](#3-put-apiv1shiftsid)
7. [Huỷ ca làm việc](#4-delete-apiv1shiftsid)
8. [Tạo ca lặp lại](#5-post-apiv1shiftsrecurring)
9. [Danh sách ca trống](#6-get-apiv1shiftsopen)
10. [Nhân viên nhận ca trống](#7-post-apiv1shiftsidclaim)
11. [Nhân viên xác nhận đi làm](#8-post-apiv1shiftsidconfirm)
12. [Check-in](#9-post-apiv1shiftsidcheck-in)
13. [Check-out](#10-post-apiv1shiftsidcheck-out)
14. [Kiểm tra conflict trước khi lưu](#11-get-apiv1shiftsconflict-check)
15. [Nhân viên rảnh theo khung giờ](#12-get-apiv1shiftsavailable-employees)
16. [Copy lịch tuần](#13-post-apiv1shiftscopy-week)
17. [Enum Reference](#enum-reference)
18. [Error Codes](#error-codes)

---

## Cấu Trúc Response Chung

```json
{
  "success": true,
  "data": { ... },
  "message": "Thông báo thành công"
}
```

```json
{
  "success": false,
  "error": {
    "code": "SHIFT_CONFLICT",
    "message": "Mô tả lỗi"
  }
}
```

---

## Vòng Đời Trạng Thái Ca (State Machine)

```
[Admin tạo - có NV]      [Admin tạo - không có NV]
       │                          │
       ▼                          ▼
   ASSIGNED ◄──────────────── PUBLISHED
       │          (NV claim)      │ (ai cũng xem được)
       │
       │ (NV xác nhận)
       ▼
   CONFIRMED
       │
       │ (NV check-in)
       ▼
   IN_PROGRESS
       │
       │ (NV check-out)           │ (hệ thống tự động sau 2h)
       ▼                          ▼
   COMPLETED                 MISSING_OUT

   ASSIGNED/CONFIRMED ──► MISSED  (hệ thống tự động: quá giờ không check-in)
   Bất kỳ trạng thái nào ──► CANCELLED  (Admin huỷ, trừ COMPLETED/IN_PROGRESS)
```

> **Lưu ý SCHEDULED:** Trạng thái legacy, hành vi giống `ASSIGNED` (dữ liệu cũ). Frontend có thể hiển thị như `ASSIGNED`.

---

## Schemas Dùng Chung

### ShiftResponse

```json
{
  "id": 42,

  "employeeId": 5,           // null nếu ca trống (PUBLISHED)
  "employeeName": "Nguyễn Văn A",  // null nếu ca trống

  "customerId": 12,
  "customerName": "Hộ Bà Trần Thị B",
  "customerLatitude": 10.7769,
  "customerLongitude": 106.7009,
  "customerAddress": "123 Nguyễn Trãi, Phường 2, Quận 5, TP.HCM",

  "templateId": 3,           // null nếu không dùng template
  "templateName": "Ca sáng",

  "packageId": 7,            // null nếu không thuộc gói

  "shiftDate": "2026-04-07",
  "startTime": "08:00",
  "endTime": "16:00",
  "durationMinutes": 480,

  "shiftType": "NORMAL",     // NORMAL | HOLIDAY | OT_EMERGENCY
  "otMultiplier": 1.0,       // 1.0 | 1.5 (OT) | 2.0 (HOLIDAY)
  "status": "ASSIGNED",
  "notes": "Ghi chú tuỳ chọn",

  "checkInTime": "2026-04-07T08:03:22",  // null nếu chưa check-in
  "checkInLat": 10.7771,
  "checkInLng": 106.7010,
  "checkInDistanceMeters": 28.5,         // null nếu KH chưa có GPS
  "checkInPhoto": "https://...",         // null nếu trong geofence và đúng giờ

  "checkOutTime": "2026-04-07T16:05:10", // null nếu chưa check-out
  "checkOutLat": 10.7772,
  "checkOutLng": 106.7011,
  "checkOutDistanceMeters": 31.0,

  "actualMinutes": 482,      // null cho đến khi check-out

  "createdAt": "2026-03-22T10:00:00",
  "updatedAt": "2026-03-22T10:00:00"
}
```

### ConflictCheckResponse

```json
{
  "hasConflict": true,
  "conflictType": "OVERLAP",       // "OVERLAP" | "BUFFER" | null
  "detail": "Ca đề xuất 08:00-10:00 trùng giờ với ca hiện tại 09:00-11:00 (id=15)",
  "conflictingShiftId": 15,        // ID ca gây xung đột (null nếu không có conflict)
  "minutesShort": null             // số nguyên (chỉ khi BUFFER)
}
```

---

## 1. GET /api/v1/shifts

**Lấy danh sách ca làm việc theo tuần/tháng/nhân viên.**

**Quyền:** `ADMIN`

### Query Parameters

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `week` | string | Không | Tuần ISO: `2026-W12`. Ưu tiên hơn month/year |
| `month` | integer | Không | Tháng (1-12) |
| `year` | integer | Không | Năm. Dùng kèm `month` |
| `employeeId` | Long | Không | Lọc theo nhân viên. Null = tất cả |

> Nếu không truyền gì → mặc định trả về tuần hiện tại.

### Response `200`

```json
{
  "success": true,
  "data": {
    "5": [
      { "id": 42, "employeeId": 5, ... },
      { "id": 43, "employeeId": 5, ... }
    ],
    "6": [
      { "id": 44, "employeeId": 6, ... }
    ],
    "null": [
      { "id": 45, "employeeId": null, "status": "PUBLISHED", ... }
    ]
  },
  "message": "Lấy danh sách ca thành công"
}
```

> **Lưu ý:** Key `"null"` chứa các **ca trống** (PUBLISHED, chưa có nhân viên).
> Key còn lại là `employeeId` dạng string.

---

## 2. POST /api/v1/shifts

**Tạo ca đơn lẻ.** Nếu không truyền `employeeId` → ca trống (PUBLISHED).

**Quyền:** `ADMIN`

### Request Body

```json
{
  "employeeId": 5,           // bỏ trống hoặc null → tạo ca trống (PUBLISHED)
  "customerId": 12,          // bắt buộc
  "templateId": 3,           // tuỳ chọn
  "shiftDate": "2026-04-07", // bắt buộc
  "startTime": "08:00",      // bắt buộc
  "endTime": "16:00",        // bắt buộc
  "shiftType": "NORMAL",     // bắt buộc: NORMAL | HOLIDAY | OT_EMERGENCY
  "notes": "Ghi chú"         // tuỳ chọn
}
```

> **Quy tắc `endTime`:**
> - `NORMAL` / `HOLIDAY`: `endTime` phải **sau** `startTime` (không qua đêm)
> - `OT_EMERGENCY`: `endTime` có thể **trước** `startTime` (ca qua đêm, VD: 22:00 → 06:00)

### Response `201`

```json
{
  "success": true,
  "data": { /* ShiftResponse */ },
  "message": "Tạo ca thành công"
}
```

### Lỗi thường gặp

| HTTP | Error Code | Trường hợp |
|---|---|---|
| 400 | `BAD_REQUEST` | Thiếu field bắt buộc, endTime sai quy tắc |
| 400 | `SHIFT_OVERNIGHT_INVALID` | NORMAL/HOLIDAY có endTime ≤ startTime |
| 409 | `SHIFT_CONFLICT` | Nhân viên bị trùng giờ với ca khác |
| 409 | `SHIFT_BUFFER` | Khoảng cách giữa 2 ca < 15 phút (hoặc < thời gian di chuyển) |
| 404 | `NOT_FOUND` | employeeId/customerId/templateId không tồn tại |

---

## 3. PUT /api/v1/shifts/{id}

**Cập nhật thông tin ca.** Không được sửa khi ca đang `IN_PROGRESS` hoặc `COMPLETED`.

**Quyền:** `ADMIN`

### Path Parameter

| Tham số | Mô tả |
|---|---|
| `id` | ID ca cần sửa |

### Request Body

Cùng format với [POST /shifts](#2-post-apiv1shifts).

> Nếu xoá `employeeId` (gửi null) → ca chuyển về `PUBLISHED`.
> Nếu thêm/đổi `employeeId` khi ca đang `CONFIRMED` → giữ nguyên `CONFIRMED`.
> Nếu thêm/đổi `employeeId` khi ca đang `ASSIGNED` → giữ nguyên `ASSIGNED`.

### Response `200`

```json
{
  "success": true,
  "data": { /* ShiftResponse */ },
  "message": "Cập nhật ca thành công"
}
```

### Lỗi thường gặp

| HTTP | Error Code | Trường hợp |
|---|---|---|
| 400 | `BAD_REQUEST` | Ca đang `IN_PROGRESS` hoặc đã `COMPLETED` |
| 409 | `SHIFT_CONFLICT` | Trùng giờ với ca khác sau khi đổi NV |

---

## 4. DELETE /api/v1/shifts/{id}

**Huỷ ca** (soft delete – chuyển trạng thái sang `CANCELLED`).

**Quyền:** `ADMIN`

> Không thể huỷ ca đang `IN_PROGRESS` hoặc đã `COMPLETED`.

### Response `200`

```json
{
  "success": true,
  "data": null,
  "message": "Huỷ ca thành công"
}
```

---

## 5. POST /api/v1/shifts/recurring

**Tạo ca lặp lại theo lịch tuần.** VD: NV A làm tại KH X vào Thứ 2-4-6 từ 08:00-16:00 trong 3 tháng.

**Quyền:** `ADMIN`

### Request Body

```json
{
  "employeeId": 5,                          // bỏ trống → tạo hàng loạt ca trống (PUBLISHED)
  "customerId": 12,                         // bắt buộc
  "templateId": 3,                          // tuỳ chọn
  "startDate": "2026-04-01",               // bắt buộc
  "endDate": "2026-06-30",                 // bắt buộc. Tối đa 180 ngày từ startDate
  "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],  // bắt buộc, ít nhất 1 ngày
  "startTime": "08:00",                    // bắt buộc
  "endTime": "16:00",                      // bắt buộc
  "shiftType": "NORMAL",                   // bắt buộc
  "notes": "Ghi chú"                       // tuỳ chọn
}
```

**Giá trị `daysOfWeek`:** `MONDAY` | `TUESDAY` | `WEDNESDAY` | `THURSDAY` | `FRIDAY` | `SATURDAY` | `SUNDAY`

### Response `201`

```json
{
  "success": true,
  "data": {
    "created": 38,
    "skipped": 2,
    "createdShiftIds": [101, 102, 103, ...],
    "conflicts": [
      {
        "hasConflict": true,
        "conflictType": "OVERLAP",
        "detail": "[2026-04-07] Ca đề xuất 08:00-16:00 trùng giờ với ca hiện tại 07:00-09:00 (id=20)",
        "minutesShort": null
      }
    ]
  },
  "message": "Tạo ca lặp lại hoàn tất: 38 ca đã tạo, 2 ca bỏ qua"
}
```

> **Lưu ý:**
> - Ca **bị bỏ qua** (`skipped`) là những ngày có OVERLAP conflict. Buffer violation không bỏ qua.
> - Nếu `employeeId` null → toàn bộ ca tạo ra đều ở trạng thái `PUBLISHED`.

### Lỗi thường gặp

| HTTP | Error Code | Trường hợp |
|---|---|---|
| 400 | `BAD_REQUEST` | endDate trước startDate, hoặc khoảng cách > 180 ngày |

---

## 6. GET /api/v1/shifts/open

**Danh sách ca trống** (status = `PUBLISHED`, chưa có nhân viên) từ hôm nay trở đi.

**Quyền:** `ADMIN`, `EMPLOYEE`

### Response `200`

```json
{
  "success": true,
  "data": [
    {
      "id": 99,
      "employeeId": null,
      "employeeName": null,
      "customerId": 12,
      "customerName": "Hộ Bà Trần Thị B",
      "customerAddress": "123 Nguyễn Trãi, Quận 5",
      "shiftDate": "2026-04-07",
      "startTime": "08:00",
      "endTime": "16:00",
      "durationMinutes": 480,
      "shiftType": "NORMAL",
      "status": "PUBLISHED",
      ...
    }
  ],
  "message": "Lấy danh sách ca trống thành công"
}
```

---

## 7. POST /api/v1/shifts/{id}/claim

**Nhân viên tự nhận ca trống.** Ca chuyển `PUBLISHED → ASSIGNED`.

**Quyền:** `ADMIN`, `EMPLOYEE`

### Path Parameter

| Tham số | Mô tả |
|---|---|
| `id` | ID ca muốn nhận |

### Query Parameter

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `employeeId` | Long | Có | ID nhân viên muốn nhận ca |

### Response `200`

```json
{
  "success": true,
  "data": { /* ShiftResponse với status = "ASSIGNED" */ },
  "message": "Nhận ca thành công. Trạng thái: ASSIGNED."
}
```

### Lỗi thường gặp

| HTTP | Error Code | Trường hợp |
|---|---|---|
| 400 | `BAD_REQUEST` | Ca không ở trạng thái `PUBLISHED` |
| 400 | `BAD_REQUEST` | Ca đã có người nhận (race condition) |
| 409 | `CONFLICT` | Nhân viên bị trùng ca với shift khác |
| 404 | `NOT_FOUND` | Ca hoặc nhân viên không tồn tại |

---

## 8. POST /api/v1/shifts/{id}/confirm

**Nhân viên xác nhận sẽ đi làm.** Ca chuyển `ASSIGNED → CONFIRMED`.

**Quyền:** `ADMIN`, `EMPLOYEE`

### Path Parameter

| Tham số | Mô tả |
|---|---|
| `id` | ID ca muốn xác nhận |

### Query Parameter

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `employeeId` | Long | Có | ID nhân viên xác nhận (phải đúng với NV được gán) |

### Response `200`

```json
{
  "success": true,
  "data": { /* ShiftResponse với status = "CONFIRMED" */ },
  "message": "Xác nhận ca thành công. Trạng thái: CONFIRMED."
}
```

### Lỗi thường gặp

| HTTP | Error Code | Trường hợp |
|---|---|---|
| 400 | `BAD_REQUEST` | `employeeId` không khớp với nhân viên được gán |
| 400 | `BAD_REQUEST` | Ca không ở trạng thái `ASSIGNED` (hoặc legacy `SCHEDULED`) |

---

## 9. POST /api/v1/shifts/{id}/check-in

**Nhân viên check-in ca làm việc.** Ca chuyển `ASSIGNED/CONFIRMED → IN_PROGRESS`.

**Quyền:** `ADMIN`, `EMPLOYEE`

### Path Parameter

| Tham số | Mô tả |
|---|---|
| `id` | ID ca cần check-in |

### Request Body

```json
{
  "latitude": 10.7769,       // bắt buộc
  "longitude": 106.7009,     // bắt buộc
  "photoUrl": "https://..."  // BẮT BUỘC nếu ngoài geofence (withinGeofence = false)
}
```

> **Luồng xử lý ảnh:**
> 1. FE upload ảnh trước lên `POST /api/v1/files/upload`
> 2. Nhận về URL, rồi gửi URL đó vào field `photoUrl` ở đây

### Response `200`

```json
{
  "success": true,
  "data": {
    "withinGeofence": true,
    "distanceMeters": 28.5,
    "geofenceRadiusMeters": 100.0,   // -1 nếu khách hàng chưa có GPS
    "actionTime": "2026-04-07T08:03:22",
    "shiftStatus": "IN_PROGRESS",
    "message": "Check-in thành công"
  },
  "message": "Check-in thành công"
}
```

**Khi ngoài geofence (nhưng đã gửi ảnh):**

```json
{
  "success": true,
  "data": {
    "withinGeofence": false,
    "distanceMeters": 350.0,
    "geofenceRadiusMeters": 100.0,
    "actionTime": "2026-04-07T08:03:22",
    "shiftStatus": "IN_PROGRESS",
    "message": "Check-in ghi nhận nhưng bạn đang cách vị trí 350m (giới hạn 100m). Ảnh hiện trường đã được ghi nhận."
  },
  "message": "Check-in ghi nhận..."
}
```

### Lỗi thường gặp

| HTTP | Error Code | Trường hợp |
|---|---|---|
| 400 | `BAD_REQUEST` | Ca không ở trạng thái cho phép check-in (phải là `ASSIGNED`, `SCHEDULED`, hoặc `CONFIRMED`) |
| 400 | `BAD_REQUEST` | Ca đã được check-in trước đó |
| 400 | `BAD_REQUEST` | Ngoài geofence nhưng không gửi `photoUrl` |

---

## 10. POST /api/v1/shifts/{id}/check-out

**Nhân viên check-out kết thúc ca.** Ca chuyển `IN_PROGRESS → COMPLETED`. Hệ thống tự tính `actualMinutes`.

**Quyền:** `ADMIN`, `EMPLOYEE`

### Request Body

Cùng format với [check-in](#9-post-apiv1shiftsidcheck-in).

```json
{
  "latitude": 10.7769,
  "longitude": 106.7009,
  "photoUrl": null   // tuỳ chọn khi check-out
}
```

### Response `200`

```json
{
  "success": true,
  "data": {
    "withinGeofence": true,
    "distanceMeters": 31.0,
    "geofenceRadiusMeters": 100.0,
    "actionTime": "2026-04-07T16:05:10",
    "shiftStatus": "COMPLETED",
    "message": "Check-out thành công. Thực tế làm việc: 482 phút."
  },
  "message": "Check-out thành công. Thực tế làm việc: 482 phút."
}
```

### Lỗi thường gặp

| HTTP | Error Code | Trường hợp |
|---|---|---|
| 400 | `BAD_REQUEST` | Ca chưa được check-in (`status ≠ IN_PROGRESS`) |

---

## 11. GET /api/v1/shifts/conflict-check

**Kiểm tra conflict trước khi lưu** (dùng để highlight cảnh báo trên UI trước khi submit).

**Quyền:** `ADMIN`

### Query Parameters

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `employeeId` | Long | Có | ID nhân viên |
| `shiftDate` | string | Có | Định dạng `yyyy-MM-dd` |
| `startTime` | string | Có | Định dạng `HH:mm` |
| `endTime` | string | Có | Định dạng `HH:mm` |
| `shiftType` | string | Có | `NORMAL` / `HOLIDAY` / `OT_EMERGENCY` |
| `excludeShiftId` | Long | Không | ID ca đang sửa (để tránh tự conflict với chính nó) |

### Response `200`

**Không có conflict:**
```json
{
  "success": true,
  "data": {
    "hasConflict": false,
    "conflictType": null,
    "detail": null,
    "minutesShort": null
  },
  "message": "Kiểm tra conflict hoàn tất"
}
```

**Có conflict – OVERLAP:**
```json
{
  "success": true,
  "data": {
    "hasConflict": true,
    "conflictType": "OVERLAP",
    "detail": "Ca đề xuất 08:00-10:00 trùng giờ với ca hiện tại 09:00-11:00 (id=15)",
    "minutesShort": null
  },
  "message": "Kiểm tra conflict hoàn tất"
}
```

**Có conflict – BUFFER:**
```json
{
  "success": true,
  "data": {
    "hasConflict": true,
    "conflictType": "BUFFER",
    "detail": "Ca hiện tại (id=15) kết thúc 10:00, ca đề xuất bắt đầu 10:10 → cần 15 phút di chuyển, thiếu 5 phút",
    "minutesShort": 5
  },
  "message": "Kiểm tra conflict hoàn tất"
}
```

> **Gợi ý UX:** Gọi API này mỗi khi người dùng thay đổi `employeeId`, `shiftDate`, `startTime`, hoặc `endTime` trên form tạo/sửa ca (debounce 500ms).

---

## 12. GET /api/v1/shifts/available-employees

**Lấy danh sách nhân viên còn rảnh** trong khung giờ chỉ định (dùng cho dropdown chọn nhân viên khi tạo ca).

**Quyền:** `ADMIN`

### Query Parameters

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `shiftDate` | string | Có | Định dạng `yyyy-MM-dd` |
| `startTime` | string | Có | Định dạng `HH:mm` |
| `endTime` | string | Có | Định dạng `HH:mm` |
| `shiftType` | string | Không | Mặc định `NORMAL` |

### Response `200`

```json
{
  "success": true,
  "data": [
    {
      "employeeId": 5,
      "employeeName": "Nguyễn Văn A",
      "phoneNumber": "0901234567",
      "nextShiftEndTime": "07:30"   // null nếu không có ca nào trong ngày
    },
    {
      "employeeId": 8,
      "employeeName": "Trần Thị B",
      "phoneNumber": "0912345678",
      "nextShiftEndTime": null
    }
  ],
  "message": "Lấy danh sách nhân viên rảnh thành công"
}
```

> Nhân viên có `BUFFER` nhẹ vẫn xuất hiện trong danh sách này (không bị ẩn). FE nên gọi thêm `conflict-check` nếu cần cảnh báo chi tiết.

---

## 13. POST /api/v1/shifts/copy-week

**Sao chép toàn bộ lịch của tuần nguồn sang tuần đích.** Chỉ copy ca ở trạng thái `ASSIGNED`, `SCHEDULED`, `CONFIRMED`. Ca bị conflict OVERLAP ở tuần đích sẽ bị bỏ qua.

**Quyền:** `ADMIN`

### Request Body

```json
{
  "sourceWeek": "2026-W12",   // bắt buộc, định dạng yyyy-Www
  "targetWeek": "2026-W13"    // bắt buộc
}
```

### Response `200`

```json
{
  "success": true,
  "data": {
    "copied": 14,
    "skipped": 2,
    "conflicts": [
      {
        "hasConflict": true,
        "conflictType": "OVERLAP",
        "detail": "[2026-03-30 Nguyễn Văn A] Ca đề xuất 08:00-16:00 trùng giờ với ca hiện tại...",
        "minutesShort": null
      }
    ]
  },
  "message": "Copy tuần hoàn tất: 14 ca đã copy, 2 ca bỏ qua"
}
```

---

## Enum Reference

### ShiftStatus

| Giá trị | Mô tả | Hành động tiếp theo |
|---|---|---|
| `DRAFT` | Ca nháp, chưa đăng | Admin publish hoặc xoá |
| `PUBLISHED` | Ca trống, đang chờ NV đăng ký | NV claim → `ASSIGNED` |
| `ASSIGNED` | Đã gán NV, chờ xác nhận | NV confirm → `CONFIRMED` |
| `SCHEDULED` | Legacy (= `ASSIGNED`) | Hiển thị như `ASSIGNED` |
| `CONFIRMED` | NV đã xác nhận sẽ đến | NV check-in → `IN_PROGRESS` |
| `IN_PROGRESS` | Đang làm việc | NV check-out → `COMPLETED` |
| `COMPLETED` | Hoàn thành | Không thể sửa/huỷ |
| `MISSED` | Vắng mặt (hệ thống tự set) | Chỉ xem |
| `MISSING_OUT` | Quên check-out (hệ thống tự set) | Chỉ xem |
| `CANCELLED` | Đã huỷ | Chỉ xem |

### ShiftType

| Giá trị | Mô tả | `otMultiplier` mặc định |
|---|---|---|
| `NORMAL` | Ca thường trong tuần | `1.0` |
| `HOLIDAY` | Ca lễ/tết | `2.0` |
| `OT_EMERGENCY` | Ca ngoài giờ khẩn cấp, cho phép qua đêm | `1.5` |

### DayOfWeek (dùng trong recurring)

`MONDAY` | `TUESDAY` | `WEDNESDAY` | `THURSDAY` | `FRIDAY` | `SATURDAY` | `SUNDAY`

---

## Error Codes

| HTTP | Error Code | Mô tả |
|---|---|---|
| 400 | `BAD_REQUEST` | Dữ liệu đầu vào không hợp lệ |
| 400 | `SHIFT_OVERNIGHT_INVALID` | Ca NORMAL/HOLIDAY có endTime ≤ startTime |
| 400 | `SHIFT_CONFLICT` | Trùng giờ với ca khác (OVERLAP) |
| 400 | `SHIFT_BUFFER` | Khoảng cách giữa 2 ca quá ngắn |
| 401 | `UNAUTHORIZED` | Chưa đăng nhập / token hết hạn |
| 403 | `FORBIDDEN` | Không đủ quyền |
| 404 | `NOT_FOUND` | Tài nguyên không tồn tại |
| 409 | `CONFLICT` | Race condition khi nhiều NV cùng claim một ca |

---

## Ví Dụ Luồng Hoàn Chỉnh (FE Reference)

### Luồng Admin lập lịch hàng tuần

```
1. Gọi GET /shifts/available-employees?shiftDate=2026-04-07&startTime=08:00&endTime=16:00
   → Hiển thị dropdown chọn nhân viên

2. (Tuỳ chọn) Gọi GET /shifts/conflict-check?employeeId=5&...
   → Hiển thị cảnh báo nếu có conflict

3. Gọi POST /shifts với employeeId=5
   → Ca tạo ra với status = ASSIGNED

4. App gửi push notification cho NV (backend log)
```

### Luồng tạo hàng loạt ca lặp lại

```
1. Admin chọn: NV A, KH X, Thứ 2-4-6, từ 01/04 đến 30/06, 08:00-16:00
2. Gọi POST /shifts/recurring
3. Hiển thị kết quả: "38 ca đã tạo, 2 ca bỏ qua"
4. Hiển thị chi tiết conflicts (nếu có) để Admin xử lý thủ công
```

### Luồng ca trống (NV tự đăng ký)

```
1. Admin tạo ca trống: POST /shifts (không có employeeId)
   → status = PUBLISHED

2. NV mở App, gọi GET /shifts/open
   → Thấy danh sách ca chưa có người

3. NV chọn ca phù hợp, gọi POST /shifts/{id}/claim?employeeId=5
   → status = ASSIGNED

4. NV xác nhận: POST /shifts/{id}/confirm?employeeId=5
   → status = CONFIRMED
```

### Luồng check-in ngoài geofence

```
1. NV mở App, tới địa điểm KH
2. App lấy GPS thực tế
3. App gọi POST /shifts/{id}/check-in với lat/lng
   → Response: withinGeofence = false, distanceMeters = 350

4. App hiển thị cảnh báo: "Bạn đang cách vị trí 350m"
5. App yêu cầu NV chụp ảnh hiện trường
6. App upload ảnh: POST /api/v1/files/upload → nhận URL

7. App gọi lại POST /shifts/{id}/check-in với photoUrl
   → status = IN_PROGRESS, check-in thành công
```
