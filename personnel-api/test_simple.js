const fs = require("fs");
const path = require("path");
const OUTPUT = path.join(__dirname, "test_results.txt");

try {
  const app = require("./server");
  const server = app.listen(4567, () => {
    fs.writeFileSync(OUTPUT, "SERVER_STARTED\n", "utf8");

    const http = require("http");
    http.get("http://localhost:4567/api/nhan-vien", (res) => {
      let data = "";
      res.on("data", (c) => (data += c));
      res.on("end", () => {
        fs.appendFileSync(OUTPUT, "STATUS: " + res.statusCode + "\n");
        fs.appendFileSync(OUTPUT, "BODY: " + data + "\n");
        server.close(() => {
          fs.appendFileSync(OUTPUT, "DONE\n");
          process.exit(0);
        });
      });
    }).on("error", (e) => {
      fs.appendFileSync(OUTPUT, "HTTP_ERROR: " + e.message + "\n");
      process.exit(1);
    });
  });

  server.on("error", (e) => {
    fs.writeFileSync(OUTPUT, "LISTEN_ERROR: " + e.message + "\n");
    process.exit(1);
  });
} catch (e) {
  fs.writeFileSync(OUTPUT, "REQUIRE_ERROR: " + e.stack + "\n");
  process.exit(1);
}

setTimeout(() => {
  fs.appendFileSync(OUTPUT, "TIMEOUT\n");
  process.exit(1);
}, 10000);

