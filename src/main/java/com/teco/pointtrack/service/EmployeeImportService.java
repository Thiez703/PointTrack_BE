package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.employee.EmployeeImportRow;
import com.teco.pointtrack.dto.employee.ImportResult;
import com.teco.pointtrack.entity.SalaryLevel;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.repository.RoleRepository;
import com.teco.pointtrack.repository.SalaryLevelRepository;
import com.teco.pointtrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Excel import cho nhân viên.
 * Columns: Họ tên | Số điện thoại | Email | Cấp bậc | Khu vực | Ngày vào làm | Kỹ năng
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeImportService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    // Excel column indices
    private static final int COL_NAME       = 0;
    private static final int COL_PHONE      = 1;
    private static final int COL_EMAIL      = 2;
    private static final int COL_LEVEL      = 3;
    private static final int COL_AREA       = 4;
    private static final int COL_HIRED      = 5;
    private static final int COL_SKILLS     = 6;

    private final UserRepository        userRepository;
    private final RoleRepository        roleRepository;
    private final SalaryLevelRepository salaryLevelRepository;
    private final PasswordEncoder       passwordEncoder;
    private final PasswordService       passwordService;
    private final DataFormatter          dataFormatter = new DataFormatter();

    // ─────────────────────────────────────────────────────────────────────────
    // IMPORT
    // ─────────────────────────────────────────────────────────────────────────

    public ImportResult importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("IMPORT_FILE_INVALID");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BadRequestException("IMPORT_FILE_INVALID");
        }

        List<EmployeeImportRow> rows;
        try {
            rows = parseExcel(file);
        } catch (IOException e) {
            throw new BadRequestException("IMPORT_FILE_INVALID");
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("IMPORT_FILE_INVALID");
        }

        // ── Pre-load maps để validate nhanh ───────────────────────────────────
        Map<String, SalaryLevel> levelsByName = new HashMap<>();
        salaryLevelRepository.findAllByDeletedAtIsNullOrderByBaseSalaryAsc()
                .forEach(l -> levelsByName.put(l.getName().toLowerCase(), l));

        // Phones/emails trong file (để phát hiện trùng nội bộ)
        Set<String> seenPhones = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();

        int successCount = 0;
        List<ImportResult.ImportError> errors = new ArrayList<>();

        for (EmployeeImportRow row : rows) {
            String errorReason = validateRow(row, levelsByName, seenPhones, seenEmails);
            if (errorReason != null) {
                errors.add(ImportResult.ImportError.builder()
                        .row(row.getRowNumber())
                        .phone(row.getPhone())
                        .email(row.getEmail())
                        .reason(errorReason)
                        .build());
                continue;
            }

            seenPhones.add(row.getPhone());
            seenEmails.add(row.getEmail().toLowerCase());

            try {
                createUserFromRow(row, levelsByName);
                successCount++;
            } catch (Exception ex) {
                log.warn("Import row {} thất bại: {}", row.getRowNumber(), ex.getMessage());
                errors.add(ImportResult.ImportError.builder()
                        .row(row.getRowNumber())
                        .phone(row.getPhone())
                        .email(row.getEmail())
                        .reason(ex.getMessage())
                        .build());
            }
        }

        return ImportResult.builder()
                .success(successCount)
                .failed(errors.size())
                .errors(errors)
                .build();
    }

    /**
     * Mỗi row là 1 transaction độc lập – cho phép partial success.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createUserFromRow(EmployeeImportRow row, Map<String, SalaryLevel> levelsByName) {
        SalaryLevel level = levelsByName.get(row.getSalaryLevelName().toLowerCase());

        var userRole = roleRepository.findBySlug("USER")
                .orElseThrow(() -> new IllegalStateException("Role USER chưa được tạo"));

        String tempPassword = passwordService.generateTempPassword();

        LocalDate hiredDate = parseDate(row.getHiredDate());

        User user = User.builder()
                .fullName(row.getFullName().trim())
                .phoneNumber(row.getPhone())
                .email(row.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .area(row.getArea())
                .skills(row.getSkills())
                .status(UserStatus.ACTIVE)
                .isFirstLogin(true)
                .startDate(hiredDate)
                .role(userRole)
                .salaryLevel(level)
                .build();

        userRepository.save(user);

        passwordService.sendTempPasswordEmail(
                user.getEmail(), user.getFullName(), tempPassword, user.getPhoneNumber());

        log.info("Import: tạo NV row={} phone={}", row.getRowNumber(), row.getPhone());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEMPLATE DOWNLOAD
    // ─────────────────────────────────────────────────────────────────────────

    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Nhân viên");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Họ tên *", "Số điện thoại *", "Email *",
                    "Cấp bậc *", "Khu vực", "Ngày vào làm (yyyy-MM-dd)", "Kỹ năng (cách nhau bởi dấu phẩy)"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Sample row
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("Nguyễn Văn A");
            sample.createCell(1).setCellValue("0901234567");
            sample.createCell(2).setCellValue("nva@example.com");
            sample.createCell(3).setCellValue("Cấp 1");
            sample.createCell(4).setCellValue("Quận 1");
            sample.createCell(5).setCellValue("2024-01-15");
            sample.createCell(6).setCellValue("tam_be, ve_sinh");

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Trả về null nếu row hợp lệ, hoặc thông báo lỗi đầu tiên.
     */
    public String validateRow(EmployeeImportRow row,
                               Map<String, SalaryLevel> levelsByName,
                               Set<String> seenPhones,
                               Set<String> seenEmails) {

        if (row.getFullName() == null || row.getFullName().isBlank()) {
            return "Họ tên không được để trống";
        }
        if (row.getPhone() == null || !PHONE_PATTERN.matcher(row.getPhone()).matches()) {
            return "Số điện thoại không hợp lệ (phải 10 chữ số, bắt đầu bằng 0)";
        }
        if (row.getEmail() == null || !EMAIL_PATTERN.matcher(row.getEmail()).matches()) {
            return "Email không hợp lệ";
        }
        if (row.getSalaryLevelName() == null || row.getSalaryLevelName().isBlank()) {
            return "Cấp bậc không được để trống";
        }
        if (!levelsByName.containsKey(row.getSalaryLevelName().toLowerCase())) {
            return "Cấp bậc '" + row.getSalaryLevelName() + "' không tồn tại trong hệ thống";
        }

        // Trùng trong file
        if (seenPhones.contains(row.getPhone())) {
            return "Số điện thoại trùng lặp trong file";
        }
        if (seenEmails.contains(row.getEmail().toLowerCase())) {
            return "Email trùng lặp trong file";
        }

        // Trùng trong DB
        if (userRepository.existsByPhoneNumberAndDeletedAtIsNull(row.getPhone())) {
            return "Số điện thoại đã tồn tại trong hệ thống";
        }
        if (userRepository.existsByEmailAndDeletedAtIsNull(row.getEmail().toLowerCase())) {
            return "Email đã tồn tại trong hệ thống";
        }

        return null; // valid
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Excel parsing
    // ─────────────────────────────────────────────────────────────────────────

    private List<EmployeeImportRow> parseExcel(MultipartFile file) throws IOException {
        List<EmployeeImportRow> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            // Skip header row (row 0)
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;

                EmployeeImportRow dto = new EmployeeImportRow();
                dto.setRowNumber(r + 1); // 1-indexed for user-friendly errors
                dto.setFullName(getCellString(row, COL_NAME));
                dto.setPhone(getCellString(row, COL_PHONE));
                dto.setEmail(getCellString(row, COL_EMAIL));
                dto.setSalaryLevelName(getCellString(row, COL_LEVEL));
                dto.setArea(getCellString(row, COL_AREA));
                dto.setHiredDate(getCellString(row, COL_HIRED));
                dto.setSkills(parseSkills(getCellString(row, COL_SKILLS)));
                rows.add(dto);
            }
        }
        return rows;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                yield dataFormatter.formatCellValue(cell).trim();
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int c = COL_NAME; c <= COL_SKILLS; c++) {
            String val = getCellString(row, c);
            if (val != null && !val.isBlank()) return false;
        }
        return true;
    }

    /**
     * Parse "tam_be, ve_sinh" → ["tam_be","ve_sinh"]
     */
    private List<String> parseSkills(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        String[] formats = {"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy"};
        for (String fmt : formats) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern(fmt));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
