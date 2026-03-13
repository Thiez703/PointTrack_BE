const express = require("express");
const cors = require("cors");

const app = express();
const PORT = process.env.PORT || 3000;

// ── Middleware ────────────────────────────────────────────────────────────────
app.use(cors());
app.use(express.json());

// ── In-memory data store ─────────────────────────────────────────────────────
let employees = [];
let nextId = 1;

/** Generate mã nhân viên: NV001, NV002, ... */
function generateMaNV() {
  return `NV${String(nextId).padStart(3, "0")}`;
}

// ── Seed data ────────────────────────────────────────────────────────────────
function seedData() {
  const now = new Date().toISOString();
  employees.push(
    {
      id: nextId,
      ma_nv: generateMaNV(),
      ho_ten: "Nguyen Van A",
      email: "nguyenvana@example.com",
      so_dien_thoai: "0901234567",
      phong_ban: "Phòng IT",
      chuc_vu: "Developer",
      ngay_vao_lam: "2024-01-15",
      luong: 15000000,
      trang_thai: "active",
      created_at: now,
      updated_at: now,
    }
  );
  nextId++;
  employees.push(
    {
      id: nextId,
      ma_nv: generateMaNV(),
      ho_ten: "Tran Thi B",
      email: "tranthib@example.com",
      so_dien_thoai: "0912345678",
      phong_ban: "Phòng HR",
      chuc_vu: "Manager",
      ngay_vao_lam: "2023-06-01",
      luong: 25000000,
      trang_thai: "active",
      created_at: now,
      updated_at: now,
    }
  );
  nextId++;
}

seedData();

// ── Validation helpers ───────────────────────────────────────────────────────
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^[0-9]{10,11}$/;
const VALID_TRANG_THAI = ["active", "inactive"];

/**
 * Validate employee payload.
 * @param {object} data        — request body
 * @param {boolean} isPartial  — true for PATCH (fields are optional)
 * @param {number|null} excludeId — employee id to exclude from email-uniqueness check
 * @returns {{ valid: boolean, errors: string[] }}
 */
function validateEmployee(data, isPartial = false, excludeId = null) {
  const errors = [];

  // ho_ten
  if (!isPartial || data.ho_ten !== undefined) {
    if (!data.ho_ten || typeof data.ho_ten !== "string" || !data.ho_ten.trim()) {
      errors.push("ho_ten is required and must be a non-empty string.");
    }
  }

  // email
  if (!isPartial || data.email !== undefined) {
    if (!data.email || typeof data.email !== "string" || !data.email.trim()) {
      errors.push("email is required and must be a non-empty string.");
    } else if (!EMAIL_REGEX.test(data.email)) {
      errors.push("email must be a valid email format.");
    } else {
      const duplicate = employees.find(
        (e) => e.email.toLowerCase() === data.email.toLowerCase() && e.id !== excludeId
      );
      if (duplicate) {
        // We'll flag this separately so the caller can return 409
        errors.push("EMAIL_CONFLICT");
      }
    }
  }

  // so_dien_thoai
  if (data.so_dien_thoai !== undefined && data.so_dien_thoai !== null && data.so_dien_thoai !== "") {
    if (!PHONE_REGEX.test(data.so_dien_thoai)) {
      errors.push("so_dien_thoai must be 10-11 digits.");
    }
  }

  // luong
  if (data.luong !== undefined && data.luong !== null) {
    if (typeof data.luong !== "number" || data.luong < 0) {
      errors.push("luong must be a non-negative number.");
    }
  }

  // trang_thai
  if (data.trang_thai !== undefined && data.trang_thai !== null) {
    if (!VALID_TRANG_THAI.includes(data.trang_thai)) {
      errors.push('trang_thai must be "active" or "inactive".');
    }
  }

  return { valid: errors.length === 0, errors };
}

// ── Helper: build success / error responses ──────────────────────────────────
function success(res, data, message = "Success", status = 200) {
  return res.status(status).json({ success: true, data, message });
}

function fail(res, message, errors = [], status = 400) {
  return res.status(status).json({ success: false, message, errors });
}

// ── Routes ───────────────────────────────────────────────────────────────────

// 1. GET /api/nhan-vien — paginated list
app.get("/api/nhan-vien", (req, res) => {
  let result = [...employees];

  // Filter by phong_ban
  if (req.query.phong_ban) {
    result = result.filter(
      (e) => e.phong_ban && e.phong_ban.toLowerCase() === req.query.phong_ban.toLowerCase()
    );
  }

  // Filter by trang_thai
  if (req.query.trang_thai) {
    result = result.filter((e) => e.trang_thai === req.query.trang_thai);
  }

  // Search by ho_ten or ma_nv (case-insensitive)
  if (req.query.search) {
    const s = req.query.search.toLowerCase();
    result = result.filter(
      (e) =>
        e.ho_ten.toLowerCase().includes(s) ||
        e.ma_nv.toLowerCase().includes(s)
    );
  }

  // Pagination
  const total = result.length;
  const page = Math.max(parseInt(req.query.page, 10) || 1, 1);
  const limit = Math.max(parseInt(req.query.limit, 10) || 10, 1);
  const totalPages = Math.ceil(total / limit) || 1;
  const start = (page - 1) * limit;
  const paginatedData = result.slice(start, start + limit);

  return res.status(200).json({
    success: true,
    data: paginatedData,
    pagination: { page, limit, total, totalPages },
  });
});

// 2. GET /api/nhan-vien/:id — single employee
app.get("/api/nhan-vien/:id", (req, res) => {
  const id = parseInt(req.params.id, 10);
  const emp = employees.find((e) => e.id === id);
  if (!emp) return fail(res, "Employee not found.", [], 404);
  return success(res, emp);
});

// 3. POST /api/nhan-vien — create
app.post("/api/nhan-vien", (req, res) => {
  const { valid, errors } = validateEmployee(req.body, false, null);

  if (!valid) {
    if (errors.includes("EMAIL_CONFLICT")) {
      return fail(
        res,
        "Email already exists.",
        errors.filter((e) => e !== "EMAIL_CONFLICT"),
        409
      );
    }
    return fail(res, "Validation failed.", errors, 400);
  }

  const now = new Date().toISOString();
  const newEmployee = {
    id: nextId,
    ma_nv: generateMaNV(),
    ho_ten: req.body.ho_ten.trim(),
    email: req.body.email.trim(),
    so_dien_thoai: req.body.so_dien_thoai || null,
    phong_ban: req.body.phong_ban || null,
    chuc_vu: req.body.chuc_vu || null,
    ngay_vao_lam: req.body.ngay_vao_lam || null,
    luong: req.body.luong ?? null,
    trang_thai: req.body.trang_thai || "active",
    created_at: now,
    updated_at: now,
  };
  nextId++;
  employees.push(newEmployee);

  return success(res, newEmployee, "Employee created successfully.", 201);
});

// 4. PUT /api/nhan-vien/:id — full update
app.put("/api/nhan-vien/:id", (req, res) => {
  const id = parseInt(req.params.id, 10);
  const index = employees.findIndex((e) => e.id === id);
  if (index === -1) return fail(res, "Employee not found.", [], 404);

  const { valid, errors } = validateEmployee(req.body, false, id);

  if (!valid) {
    if (errors.includes("EMAIL_CONFLICT")) {
      return fail(
        res,
        "Email already exists.",
        errors.filter((e) => e !== "EMAIL_CONFLICT"),
        409
      );
    }
    return fail(res, "Validation failed.", errors, 400);
  }

  const existing = employees[index];
  const now = new Date().toISOString();

  employees[index] = {
    id: existing.id,
    ma_nv: existing.ma_nv,
    ho_ten: req.body.ho_ten.trim(),
    email: req.body.email.trim(),
    so_dien_thoai: req.body.so_dien_thoai || null,
    phong_ban: req.body.phong_ban || null,
    chuc_vu: req.body.chuc_vu || null,
    ngay_vao_lam: req.body.ngay_vao_lam || null,
    luong: req.body.luong ?? null,
    trang_thai: req.body.trang_thai || "active",
    created_at: existing.created_at,
    updated_at: now,
  };

  return success(res, employees[index], "Employee updated successfully.");
});

// 5. PATCH /api/nhan-vien/:id — partial update
app.patch("/api/nhan-vien/:id", (req, res) => {
  const id = parseInt(req.params.id, 10);
  const index = employees.findIndex((e) => e.id === id);
  if (index === -1) return fail(res, "Employee not found.", [], 404);

  // Disallow changing id and ma_nv
  if (req.body.id !== undefined || req.body.ma_nv !== undefined) {
    return fail(res, "Fields id and ma_nv cannot be changed.", [], 400);
  }

  const { valid, errors } = validateEmployee(req.body, true, id);

  if (!valid) {
    if (errors.includes("EMAIL_CONFLICT")) {
      return fail(
        res,
        "Email already exists.",
        errors.filter((e) => e !== "EMAIL_CONFLICT"),
        409
      );
    }
    return fail(res, "Validation failed.", errors, 400);
  }

  const allowedFields = [
    "ho_ten",
    "email",
    "so_dien_thoai",
    "phong_ban",
    "chuc_vu",
    "ngay_vao_lam",
    "luong",
    "trang_thai",
  ];

  const existing = employees[index];
  for (const field of allowedFields) {
    if (req.body[field] !== undefined) {
      existing[field] =
        typeof req.body[field] === "string" ? req.body[field].trim() : req.body[field];
    }
  }
  existing.updated_at = new Date().toISOString();

  return success(res, existing, "Employee updated successfully.");
});

// 6. DELETE /api/nhan-vien/:id — delete
app.delete("/api/nhan-vien/:id", (req, res) => {
  const id = parseInt(req.params.id, 10);
  const index = employees.findIndex((e) => e.id === id);
  if (index === -1) return fail(res, "Employee not found.", [], 404);

  const [deleted] = employees.splice(index, 1);
  return success(res, deleted, "Employee deleted successfully.");
});

// ── Start server & log routes ────────────────────────────────────────────────
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`\n🚀  Personnel API running at http://localhost:${PORT}\n`);
    console.log("Available routes:");
    console.log("  GET    /api/nhan-vien");
    console.log("  GET    /api/nhan-vien/:id");
    console.log("  POST   /api/nhan-vien");
    console.log("  PUT    /api/nhan-vien/:id");
    console.log("  PATCH  /api/nhan-vien/:id");
    console.log("  DELETE /api/nhan-vien/:id\n");
  });
}

module.exports = app;

