# PointTrack — Hệ thống Chấm công & Quản lý Ca

Nền tảng Web Responsive dành cho doanh nghiệp dịch vụ tại nhà (tắm bé, vệ sinh, sửa chữa điện nước, v.v.). Đặc thù: nhân viên di chuyển liên tục giữa các địa điểm khách hàng, ca làm việc không cố định văn phòng, cần kiểm soát chặt vị trí thực tế khi chấm công.

---

## Tính năng cốt lõi

| Nhóm | Mô tả |
|------|-------|
| **Chấm công GPS Fencing** | Xác thực vị trí theo tọa độ nhà khách hàng, bán kính 50m |
| **Quản lý ca cứng (Fixed Duration Shift)** | Ca có thời lượng cố định + buffer di chuyển 15 phút |
| **Sắp ca Drag & Drop** | Hỗ trợ ca đơn lẻ và gói dịch vụ lặp lại |
| **Giải trình đa loại** | Đi muộn, checkout trễ, GPS ngoài vùng |
| **OT tự động** | Ngày Lễ/Tết và ca đột xuất |
| **Bảng công Heatmap** | Chốt công, xuất Excel |
| **Tính lương tạm** | Theo ca cứng và cấp bậc nhân viên |

---

## Tech Stack

| Thành phần | Công nghệ |
|-----------|-----------|
| Backend | Java 21, Spring Boot 3.5.5 |
| Bảo mật | Spring Security + JWT (JJWT 0.12.5) |
| Database | MySQL 8 |
| Cache / Token Blacklist | Redis |
| ORM | Spring Data JPA (Hibernate) |
| API Docs | Swagger / OpenAPI (SpringDoc 2.8.5) |
| Build | Maven 3.9+ |
| Deploy | Docker + Docker Compose |

---

## Yêu cầu môi trường

Trước khi bắt đầu, cần cài đặt:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (bắt buộc để chạy DB và Redis)
- [JDK 21](https://adoptium.net/) (để chạy Spring Boot)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) hoặc VS Code (khuyến nghị)
- Git

> **Sinh viên mới:** Chỉ cần cài Docker là đủ để chạy MySQL + Redis. Không cần cài MySQL hay Redis trực tiếp trên máy.

---

## Cấu trúc thư mục

```
D:/PointTrack_BE/
├── src/
│   ├── main/
│   │   ├── java/com/teco/pointtrack/
│   │   │   ├── PointTrackApplication.java     ← Điểm khởi động ứng dụng
│   │   │   ├── common/                        ← AuthUtils (lấy user từ SecurityContext)
│   │   │   ├── config/                        ← Security, Redis, Swagger, DataSeeder...
│   │   │   ├── controller/                    ← REST API endpoints
│   │   │   ├── dto/                           ← Request / Response objects
│   │   │   │   ├── auth/                      ← LoginRequest, SignupRequest, AuthResponse...
│   │   │   │   ├── common/                    ← ApiResponse, MessageResponse
│   │   │   │   └── user/                      ← UserDetail, RoleDto
│   │   │   ├── entity/                        ← JPA Entities (User, Role, Permission)
│   │   │   │   └── enums/                     ← UserStatus, Gender, AuthProvider...
│   │   │   ├── exception/                     ← Exception classes + GlobalExceptionHandler
│   │   │   ├── logging/                       ← LoggingInterceptor
│   │   │   ├── repository/                    ← Spring Data JPA Repositories
│   │   │   ├── security/                      ← JWT Filter, CustomUserDetails, @RequirePermission
│   │   │   │   ├── annotation/
│   │   │   │   └── aspect/
│   │   │   ├── service/                       ← Business Logic (AuthService, CaptchaService)
│   │   │   └── utils/                         ← JwtUtils, CookieUtils, MessagesUtils
│   │   └── resources/
│   │       ├── application.yml                ← Active profile (dev mặc định)
│   │       ├── application-dev.yml            ← Config local development
│   │       ├── application-prod.yml           ← Config production (dùng env vars)
│   │       └── messages/messages.properties   ← Error messages
├── .env                                       ← Biến môi trường local (KHÔNG commit)
├── docker-compose.yml                         ← MySQL + Redis + App
├── Dockerfile                                 ← Build image Spring Boot
└── pom.xml                                    ← Maven dependencies
```

---

## Hướng dẫn chạy nhanh (dành cho team)

### Cách 1 — Chạy DB bằng Docker, chạy Spring Boot bằng IDE (khuyến nghị khi dev)

**Bước 1:** Clone project

```bash
git clone <repo-url>
cd PointTrack_BE
```

**Bước 2:** Copy file `.env` (hỏi team lead để lấy file `.env` thực tế, hoặc dùng mẫu dưới đây)

```bash
# Nếu chưa có .env, tạo mới với nội dung mặc định:
cp .env.example .env   # hoặc tự tạo theo mục ".env mẫu" bên dưới
```

**Bước 3:** Khởi động MySQL và Redis bằng Docker

```bash
# Chỉ chạy DB + Redis (không chạy app)
docker compose up db redis -d
```

Kiểm tra các container đã lên chưa:

```bash
docker compose ps
```

Kết quả mong đợi:
```
NAME               STATUS
pointtrack-db      running (healthy)
pointtrack-redis   running (healthy)
```

**Bước 4:** Chạy Spring Boot từ IDE

- Mở IntelliJ IDEA → Open project `D:/PointTrack_BE`
- Tìm class `PointTrackApplication.java` → nhấn **Run** (▶)
- Hoặc chạy bằng terminal:

```bash
./mvnw spring-boot:run
```

> Profile mặc định là `dev` — kết nối MySQL tại `localhost:3306/pointtrack_db`

**Bước 5:** Truy cập Swagger UI để test API

```
http://localhost:8080/swagger-ui.html
```

---

### Cách 2 — Chạy toàn bộ stack bằng Docker Compose (production-like)

```bash
docker compose up --build -d
```

App sẽ chạy tại: `http://localhost:8080`

Xem log:

```bash
docker compose logs -f app
```

Dừng lại:

```bash
docker compose down
```

Dừng và xóa cả data:

```bash
docker compose down -v
```

---

## File `.env` mẫu (dành cho local dev)

Tạo file `.env` tại root project với nội dung:

```dotenv
# Database
MYSQL_ROOT_PASSWORD=123456
MYSQL_DATABASE=pointtrack_db
MYSQL_USER=pointtrack_user
MYSQL_PASSWORD=pointtrack_pass
MYSQL_PORT=3307

# Redis
REDIS_PORT=6379

# JWT (thay bằng chuỗi ngẫu nhiên dài ≥ 32 ký tự khi deploy thật)
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_ISSUER=pointtrack

# Captcha Turnstile — key test luôn pass, dùng được khi dev
TURNSTILE_SECRET_KEY=1x0000000000000000000000000000000AA

# Google OAuth (để trống nếu chưa cần)
GOOGLE_CLIENT_ID=

# CORS
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
APP_COOKIE_SECURE=false
```

> **Lưu ý:** File `.env` đã có trong `.gitignore`. Không commit file này lên Git vì chứa thông tin nhạy cảm.

---

## API Authentication

Swagger UI: `http://localhost:8080/swagger-ui.html`

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `POST` | `/api/auth/signup` | Đăng ký tài khoản mới | Không |
| `POST` | `/api/auth/login` | Đăng nhập (email/SĐT + password) | Không |
| `POST` | `/api/auth/google-login` | Đăng nhập bằng Google | Không |
| `POST` | `/api/auth/refresh` | Làm mới Access Token | Không |
| `POST` | `/api/auth/logout` | Đăng xuất, thu hồi token | Bearer Token |
| `GET` | `/api/auth/me` | Lấy thông tin người dùng hiện tại | Bearer Token |

### Ví dụ gọi API bằng curl

**Đăng ký:**
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Nguyễn Văn A",
    "contact": "nguyenvana@gmail.com",
    "password": "123456",
    "confirmPassword": "123456",
    "captchaToken": "XXXX"
  }'
```

> **Dev tip:** `captchaToken` có thể điền bất kỳ chuỗi nào khi dùng test key Turnstile (`1x000...AA`).

**Đăng nhập:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "contact": "admin@pointtrack.com",
    "password": "Admin@123",
    "captchaToken": "XXXX"
  }'
```

**Gọi API có auth:**
```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <access_token>"
```

### Tài khoản mặc định (tự seed khi khởi động)

| Email | Password | Role |
|-------|----------|------|
| `admin@pointtrack.com` | `Admin@123` | ADMIN |

---

## Cách hệ thống bảo mật hoạt động (cho sinh viên mới)

```
Request → JWTFilter → SecurityConfig → Controller → Service → Repository → DB
              ↓
        Đọc Bearer Token từ Header
              ↓
        JwtUtils.validateToken()  ←→  Redis (kiểm tra blacklist)
              ↓
        CustomUserDetailsService.loadUserByUsername()
              ↓
        Gán Authentication vào SecurityContext
```

- **JWT** được ký bằng HMAC-SHA256 (secret key từ `security.jwt.secret-key`)
- **Logout** = đưa token vào Redis blacklist (hết hạn tự xóa)
- **RBAC** = phân quyền theo Role (`ROLE_ADMIN`, `ROLE_USER`) + Permission (`USER_MANAGE`, v.v.)
- **@RequirePermission("CODE")** = annotation custom để kiểm tra permission trên từng endpoint

---

## Hướng dẫn phát triển tính năng mới (dành cho sinh viên mới)

Thứ tự thêm tính năng mới theo luồng **từ dưới lên**:

```
1. Entity     →  src/main/java/com/teco/pointtrack/entity/
2. Repository →  src/main/java/com/teco/pointtrack/repository/
3. DTO        →  src/main/java/com/teco/pointtrack/dto/
4. Service    →  src/main/java/com/teco/pointtrack/service/
5. Controller →  src/main/java/com/teco/pointtrack/controller/
```

### Ví dụ: Thêm tính năng "Chấm công"

**1. Tạo Entity** (`entity/Attendance.java`)
```java
@Entity
@Table(name = "attendances")
public class Attendance extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime checkInTime;
    private Double checkInLat;
    private Double checkInLng;
    // ...
}
```

**2. Tạo Repository** (`repository/AttendanceRepository.java`)
```java
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByUserIdOrderByCheckInTimeDesc(Long userId);
}
```

**3. Tạo DTO** (`dto/attendance/CheckInRequest.java`)
```java
@Data
public class CheckInRequest {
    @NotNull private Double latitude;
    @NotNull private Double longitude;
}
```

**4. Tạo Service** (`service/AttendanceService.java`)
```java
@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;

    public void checkIn(CheckInRequest request, Long userId) {
        // business logic: kiểm tra GPS fencing, tạo record
    }
}
```

**5. Tạo Controller** (`controller/AttendanceController.java`)
```java
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @RequirePermission("ATTENDANCE_CHECKIN")
    public ResponseEntity<MessageResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        Long userId = AuthUtils.getUserDetail().getId();
        attendanceService.checkIn(request, userId);
        return ResponseEntity.ok(new MessageResponse("Chấm công thành công"));
    }
}
```

---

## Lỗi thường gặp & Cách xử lý

| Lỗi | Nguyên nhân | Cách xử lý |
|-----|-------------|------------|
| `Connection refused` port 3306 | MySQL chưa chạy | Chạy `docker compose up db -d` |
| `Connection refused` port 6379 | Redis chưa chạy | Chạy `docker compose up redis -d` |
| `Could not resolve placeholder 'security.jwt.secret-key'` | Thiếu config trong yml | Kiểm tra `application-dev.yml` |
| `Table 'pointtrack_db.xxx' doesn't exist` | JPA chưa tạo bảng | Đặt `ddl-auto: update` trong dev |
| `401 Unauthorized` khi gọi API | Token hết hạn hoặc sai | Gọi `/api/auth/refresh` để lấy token mới |
| `403 Forbidden` | Không có permission | Kiểm tra Role của user có đủ quyền không |
| `Captcha thất bại` | `captchaToken` sai | Dùng test key `1x000...AA` khi dev |
| Container `pointtrack-db` không healthy | MySQL khởi động chậm | Đợi 30s rồi thử lại, hoặc xem log: `docker compose logs db` |

---

## Lệnh Docker hữu ích

```bash
# Xem log tất cả services
docker compose logs -f

# Xem log riêng từng service
docker compose logs -f db
docker compose logs -f app

# Vào MySQL bên trong container
docker exec -it pointtrack-db mysql -u root -p
# Nhập password: 123456

# Restart một service
docker compose restart app

# Xem danh sách container đang chạy
docker compose ps

# Xóa toàn bộ và bắt đầu lại từ đầu (cẩn thận: mất data)
docker compose down -v && docker compose up -d
```

---

## Convention & Coding Style

- **Package**: `com.teco.pointtrack.<layer>` (entity, repository, service, controller, dto, config...)
- **Entity**: kế thừa `BaseEntity` (có sẵn `createdAt`, `updatedAt`, audit fields)
- **Exception**: throw `BadRequestException`, `NotFoundException`, `ConflictException` — tự động map HTTP status
- **Response wrapper**: dùng `ApiResponse<T>` cho list/detail, `MessageResponse` cho action
- **Phân quyền**: dùng `@RequirePermission("PERMISSION_CODE")` trên method controller
- **Lấy user hiện tại**: `AuthUtils.getUserDetail()` (trả về `UserDetail` từ JWT)
- **Naming**: camelCase Java, snake_case cho column DB, kebab-case URL API (`/api/check-in`)
