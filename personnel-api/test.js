const http = require("http");
const fs = require("fs");

const PORT = 3005;
const BASE = `http://localhost:${PORT}`;
const results = [];

function log(msg) {
  results.push(msg);
}

function request(method, path, body) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, BASE);
    const opts = {
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method,
      headers: { "Content-Type": "application/json" },
    };
    const req = http.request(opts, (res) => {
      let data = "";
      res.on("data", (c) => (data += c));
      res.on("end", () => {
        resolve({ status: res.statusCode, body: JSON.parse(data) });
      });
    });
    req.on("error", (e) => reject(e));
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function runTests() {
  try {
    // Test 1: GET all employees (seeded)
    log("=== Test 1: GET /api/nhan-vien ===");
    let r = await request("GET", "/api/nhan-vien");
    log(`Status: ${r.status} | success: ${r.body.success} | count: ${r.body.data.length} | total: ${r.body.pagination.total}`);
    log(r.status === 200 && r.body.data.length === 2 ? "✅ PASS" : "❌ FAIL");

    // Test 2: GET single employee
    log("\n=== Test 2: GET /api/nhan-vien/1 ===");
    r = await request("GET", "/api/nhan-vien/1");
    log(`Status: ${r.status} | ma_nv: ${r.body.data.ma_nv} | ho_ten: ${r.body.data.ho_ten}`);
    log(r.status === 200 && r.body.data.ma_nv === "NV001" ? "✅ PASS" : "❌ FAIL");

    // Test 3: GET not found
    log("\n=== Test 3: GET /api/nhan-vien/999 ===");
    r = await request("GET", "/api/nhan-vien/999");
    log(`Status: ${r.status} | message: ${r.body.message}`);
    log(r.status === 404 ? "✅ PASS" : "❌ FAIL");

    // Test 4: POST create employee
    log("\n=== Test 4: POST /api/nhan-vien ===");
    r = await request("POST", "/api/nhan-vien", {
      ho_ten: "Le Van C",
      email: "levanc@example.com",
      so_dien_thoai: "0923456789",
      phong_ban: "Phòng Kế toán",
      chuc_vu: "Accountant",
      ngay_vao_lam: "2025-03-01",
      luong: 12000000,
    });
    log(`Status: ${r.status} | ma_nv: ${r.body.data.ma_nv} | id: ${r.body.data.id}`);
    log(r.status === 201 && r.body.data.ma_nv === "NV003" ? "✅ PASS" : "❌ FAIL");

    // Test 5: POST validation error
    log("\n=== Test 5: POST validation error ===");
    r = await request("POST", "/api/nhan-vien", { email: "bad" });
    log(`Status: ${r.status} | errors: ${JSON.stringify(r.body.errors)}`);
    log(r.status === 400 ? "✅ PASS" : "❌ FAIL");

    // Test 6: POST duplicate email (409)
    log("\n=== Test 6: POST duplicate email ===");
    r = await request("POST", "/api/nhan-vien", {
      ho_ten: "Dup",
      email: "levanc@example.com",
    });
    log(`Status: ${r.status} | message: ${r.body.message}`);
    log(r.status === 409 ? "✅ PASS" : "❌ FAIL");

    // Test 7: PUT update
    log("\n=== Test 7: PUT /api/nhan-vien/3 ===");
    r = await request("PUT", "/api/nhan-vien/3", {
      ho_ten: "Le Van C Updated",
      email: "levanc_new@example.com",
      luong: 15000000,
    });
    log(`Status: ${r.status} | ho_ten: ${r.body.data.ho_ten}`);
    log(r.status === 200 && r.body.data.ho_ten === "Le Van C Updated" ? "✅ PASS" : "❌ FAIL");

    // Test 8: PATCH partial update
    log("\n=== Test 8: PATCH /api/nhan-vien/3 ===");
    r = await request("PATCH", "/api/nhan-vien/3", { luong: 20000000 });
    log(`Status: ${r.status} | luong: ${r.body.data.luong} | ho_ten: ${r.body.data.ho_ten}`);
    log(r.status === 200 && r.body.data.luong === 20000000 ? "✅ PASS" : "❌ FAIL");

    // Test 9: PATCH cannot change id/ma_nv
    log("\n=== Test 9: PATCH cannot change id ===");
    r = await request("PATCH", "/api/nhan-vien/3", { id: 999 });
    log(`Status: ${r.status} | message: ${r.body.message}`);
    log(r.status === 400 ? "✅ PASS" : "❌ FAIL");

    // Test 10: DELETE
    log("\n=== Test 10: DELETE /api/nhan-vien/3 ===");
    r = await request("DELETE", "/api/nhan-vien/3");
    log(`Status: ${r.status} | deleted id: ${r.body.data.id}`);
    log(r.status === 200 && r.body.data.id === 3 ? "✅ PASS" : "❌ FAIL");

    // Test 11: DELETE not found
    log("\n=== Test 11: DELETE not found ===");
    r = await request("DELETE", "/api/nhan-vien/3");
    log(`Status: ${r.status}`);
    log(r.status === 404 ? "✅ PASS" : "❌ FAIL");

    // Test 12: Search
    log("\n=== Test 12: Search ===");
    r = await request("GET", "/api/nhan-vien?search=nguyen");
    log(`Status: ${r.status} | count: ${r.body.data.length}`);
    log(r.status === 200 && r.body.data.length === 1 ? "✅ PASS" : "❌ FAIL");

    // Test 13: Pagination
    log("\n=== Test 13: Pagination ===");
    r = await request("GET", "/api/nhan-vien?page=1&limit=1");
    log(`Status: ${r.status} | data.length: ${r.body.data.length} | totalPages: ${r.body.pagination.totalPages}`);
    log(r.status === 200 && r.body.data.length === 1 && r.body.pagination.totalPages === 2 ? "✅ PASS" : "❌ FAIL");

  } catch (err) {
    log(`\n❌ ERROR: ${err.message}`);
  }

  fs.writeFileSync("C:/DU_AN_BAP/PointTrack_BE/personnel-api/test_results.txt", results.join("\n"));
  process.exit(0);
}

runTests();

