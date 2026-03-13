# Personnel Management API (P2-BE-01)

RESTful API for Personnel (Nhân viên) management.

## Quick Start

```bash
npm install
npm start
```

Server runs at **http://localhost:3000** by default.  
Set the `PORT` environment variable to change it.

---

## Data Model — NhanVien (Employee)

| Field          | Type     | Notes                              |
| -------------- | -------- | ---------------------------------- |
| id             | number   | Auto-increment                     |
| ma_nv          | string   | Auto-generated (NV001, NV002, ...) |
| ho_ten         | string   | **Required**                       |
| email          | string   | **Required**, unique, valid format |
| so_dien_thoai  | string   | 10-11 digits                       |
| phong_ban      | string   | Department                         |
| chuc_vu        | string   | Position                           |
| ngay_vao_lam   | string   | YYYY-MM-DD                         |
| luong          | number   | Non-negative                       |
| trang_thai     | enum     | `active` \| `inactive`             |
| created_at     | string   | ISO datetime                       |
| updated_at     | string   | ISO datetime                       |

---

## API Endpoints

### 1. List employees (paginated)

```
GET /api/nhan-vien?page=1&limit=10&phong_ban=Phòng IT&trang_thai=active&search=nguyen
```

**Query parameters** (all optional):

| Param      | Description                                |
| ---------- | ------------------------------------------ |
| page       | Page number (default 1)                    |
| limit      | Items per page (default 10)                |
| phong_ban  | Filter by department (case-insensitive)    |
| trang_thai | Filter by status (`active` / `inactive`)   |
| search     | Search by `ho_ten` or `ma_nv` (case-insensitive) |

**Response 200:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "ma_nv": "NV001",
      "ho_ten": "Nguyen Van A",
      "email": "nguyenvana@example.com",
      "so_dien_thoai": "0901234567",
      "phong_ban": "Phòng IT",
      "chuc_vu": "Developer",
      "ngay_vao_lam": "2024-01-15",
      "luong": 15000000,
      "trang_thai": "active",
      "created_at": "2026-03-11T12:00:00.000Z",
      "updated_at": "2026-03-11T12:00:00.000Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 10,
    "total": 2,
    "totalPages": 1
  }
}
```

---

### 2. Get single employee

```
GET /api/nhan-vien/1
```

**Response 200:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "ma_nv": "NV001",
    "ho_ten": "Nguyen Van A",
    "email": "nguyenvana@example.com",
    "so_dien_thoai": "0901234567",
    "phong_ban": "Phòng IT",
    "chuc_vu": "Developer",
    "ngay_vao_lam": "2024-01-15",
    "luong": 15000000,
    "trang_thai": "active",
    "created_at": "2026-03-11T12:00:00.000Z",
    "updated_at": "2026-03-11T12:00:00.000Z"
  },
  "message": "Success"
}
```

**Response 404:**

```json
{
  "success": false,
  "message": "Employee not found.",
  "errors": []
}
```

---

### 3. Create employee

```
POST /api/nhan-vien
Content-Type: application/json

{
  "ho_ten": "Le Van C",
  "email": "levanc@example.com",
  "so_dien_thoai": "0923456789",
  "phong_ban": "Phòng Kế toán",
  "chuc_vu": "Accountant",
  "ngay_vao_lam": "2025-03-01",
  "luong": 12000000,
  "trang_thai": "active"
}
```

**Response 201:**

```json
{
  "success": true,
  "data": {
    "id": 3,
    "ma_nv": "NV003",
    "ho_ten": "Le Van C",
    "email": "levanc@example.com",
    "so_dien_thoai": "0923456789",
    "phong_ban": "Phòng Kế toán",
    "chuc_vu": "Accountant",
    "ngay_vao_lam": "2025-03-01",
    "luong": 12000000,
    "trang_thai": "active",
    "created_at": "2026-03-11T12:00:00.000Z",
    "updated_at": "2026-03-11T12:00:00.000Z"
  },
  "message": "Employee created successfully."
}
```

**Response 400 (validation error):**

```json
{
  "success": false,
  "message": "Validation failed.",
  "errors": [
    "ho_ten is required and must be a non-empty string.",
    "email is required and must be a non-empty string."
  ]
}
```

**Response 409 (duplicate email):**

```json
{
  "success": false,
  "message": "Email already exists.",
  "errors": []
}
```

---

### 4. Full update employee

```
PUT /api/nhan-vien/1
Content-Type: application/json

{
  "ho_ten": "Nguyen Van A Updated",
  "email": "nguyenvana_updated@example.com",
  "so_dien_thoai": "0901234567",
  "phong_ban": "Phòng IT",
  "chuc_vu": "Senior Developer",
  "ngay_vao_lam": "2024-01-15",
  "luong": 20000000,
  "trang_thai": "active"
}
```

**Response 200:**

```json
{
  "success": true,
  "data": { "...updated employee..." },
  "message": "Employee updated successfully."
}
```

---

### 5. Partial update employee

```
PATCH /api/nhan-vien/1
Content-Type: application/json

{
  "luong": 22000000,
  "chuc_vu": "Tech Lead"
}
```

> **Note:** `id` and `ma_nv` cannot be changed via PATCH.

**Response 200:**

```json
{
  "success": true,
  "data": { "...updated employee..." },
  "message": "Employee updated successfully."
}
```

---

### 6. Delete employee

```
DELETE /api/nhan-vien/1
```

**Response 200:**

```json
{
  "success": true,
  "data": { "...deleted employee..." },
  "message": "Employee deleted successfully."
}
```

---

## HTTP Status Codes

| Code | Meaning          |
| ---- | ---------------- |
| 200  | OK               |
| 201  | Created          |
| 400  | Validation error |
| 404  | Not found        |
| 409  | Conflict (dup email) |

---

## Project Structure

```
personnel-api/
├── package.json
├── server.js
└── README.md
```

