const http = require("http");
const fs = require("fs");
const path = require("path");
const app = require("./server");

const PORT = 4567;
const BASE = `http://localhost:${PORT}`;
const OUTPUT_FILE = path.join(__dirname, "test_results.txt");
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
        try {
          resolve({ status: res.statusCode, body: JSON.parse(data) });
        } catch (e) {
          resolve({ status: res.statusCode, body: data });
        }
      });
    });
    req.on("error", (e) => reject(e));
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function runTests() {
  const server = app.listen(PORT);
  await new Promise((r) => setTimeout(r, 500));

  try {
    // Test 1: GET all
    log("=== Test 1: GET /api/nhan-vien ===");
    let r = await request("GET", "/api/nhan-vien");
    log("Status: " + r.status + " | count: " + r.body.data.length + " | total: " + r.body.pagination.total);
    log(r.status === 200 && r.body.data.length === 2 ? "PASS" : "FAIL");

    // Test 2: GET by id
    log("=== Test 2: GET /api/nhan-vien/1 ===");
    r = await request("GET", "/api/nhan-vien/1");
    log("Status: " + r.status + " | ma_nv: " + r.body.data.ma_nv);
    log(r.status === 200 && r.body.data.ma_nv === "NV001" ? "PASS" : "FAIL");

    // Test 3: GET 404
    log("=== Test 3: GET /api/nhan-vien/999 ===");
    r = await request("GET", "/api/nhan-vien/999");
    log("Status: " + r.status);
    log(r.status === 404 ? "PASS" : "FAIL");

    // Test 4: POST create
    log("=== Test 4: POST /api/nhan-vien ===");
    r = await request("POST", "/api/nhan-vien", {
      ho_ten: "Le Van C",
      email: "levanc@example.com",
      so_dien_thoai: "0923456789",
      luong: 12000000,
    });
    log("Status: " + r.status + " | ma_nv: " + r.body.data.ma_nv);
    log(r.status === 201 && r.body.data.ma_nv === "NV003" ? "PASS" : "FAIL");

    // Test 5: POST validation
    log("=== Test 5: POST validation ===");
    r = await request("POST", "/api/nhan-vien", { email: "bad" });
    log("Status: " + r.status);
    log(r.status === 400 ? "PASS" : "FAIL");

    // Test 6: POST dup email 409
    log("=== Test 6: POST duplicate email ===");
    r = await request("POST", "/api/nhan-vien", { ho_ten: "Dup", email: "levanc@example.com" });
    log("Status: " + r.status);
    log(r.status === 409 ? "PASS" : "FAIL");

    // Test 7: PUT
    log("=== Test 7: PUT /api/nhan-vien/3 ===");
    r = await request("PUT", "/api/nhan-vien/3", {
      ho_ten: "Le Van C Updated",
      email: "levanc_new@example.com",
      luong: 15000000,
    });
    log("Status: " + r.status + " | ho_ten: " + r.body.data.ho_ten);
    log(r.status === 200 && r.body.data.ho_ten === "Le Van C Updated" ? "PASS" : "FAIL");

    // Test 8: PATCH
    log("=== Test 8: PATCH /api/nhan-vien/3 ===");
    r = await request("PATCH", "/api/nhan-vien/3", { luong: 20000000 });
    log("Status: " + r.status + " | luong: " + r.body.data.luong);
    log(r.status === 200 && r.body.data.luong === 20000000 ? "PASS" : "FAIL");

    // Test 9: PATCH id blocked
    log("=== Test 9: PATCH id blocked ===");
    r = await request("PATCH", "/api/nhan-vien/3", { id: 999 });
    log("Status: " + r.status);
    log(r.status === 400 ? "PASS" : "FAIL");

    // Test 10: DELETE
    log("=== Test 10: DELETE /api/nhan-vien/3 ===");
    r = await request("DELETE", "/api/nhan-vien/3");
    log("Status: " + r.status + " | deleted id: " + r.body.data.id);
    log(r.status === 200 && r.body.data.id === 3 ? "PASS" : "FAIL");

    // Test 11: DELETE 404
    log("=== Test 11: DELETE 404 ===");
    r = await request("DELETE", "/api/nhan-vien/3");
    log("Status: " + r.status);
    log(r.status === 404 ? "PASS" : "FAIL");

    // Test 12: Search
    log("=== Test 12: Search ===");
    r = await request("GET", "/api/nhan-vien?search=nguyen");
    log("Status: " + r.status + " | count: " + r.body.data.length);
    log(r.status === 200 && r.body.data.length === 1 ? "PASS" : "FAIL");

    // Test 13: Pagination
    log("=== Test 13: Pagination ===");
    r = await request("GET", "/api/nhan-vien?page=1&limit=1");
    log("Status: " + r.status + " | data.length: " + r.body.data.length + " | totalPages: " + r.body.pagination.totalPages);
    log(r.status === 200 && r.body.data.length === 1 && r.body.pagination.totalPages === 2 ? "PASS" : "FAIL");

    // Summary
    const total = results.filter((l) => l === "PASS" || l === "FAIL").length;
    const passed = results.filter((l) => l === "PASS").length;
    log("\n=== SUMMARY: " + passed + "/" + total + " passed ===");
  } catch (err) {
    log("ERROR: " + err.message);
  }

  server.close();
  fs.writeFileSync(OUTPUT_FILE, results.join("\n"), "utf8");
  process.exit(0);
}

process.on("uncaughtException", (err) => {
  fs.writeFileSync(OUTPUT_FILE, "UNCAUGHT: " + err.stack, "utf8");
  process.exit(1);
});

process.on("unhandledRejection", (err) => {
  fs.writeFileSync(OUTPUT_FILE, "UNHANDLED: " + (err.stack || err), "utf8");
  process.exit(1);
});

runTests();



