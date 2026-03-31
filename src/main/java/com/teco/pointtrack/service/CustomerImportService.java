package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.customer.CustomerImportResult;
import com.teco.pointtrack.dto.customer.CustomerImportRow;
import com.teco.pointtrack.dto.customer.GeoPoint;
import com.teco.pointtrack.entity.Customer;
import com.teco.pointtrack.entity.enums.CustomerSource;
import com.teco.pointtrack.entity.enums.CustomerStatus;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

/**
 * Service import danh sách khách hàng từ file Excel (.xlsx).
 *
 * <h3>Template Excel (7 cột)</h3>
 * <pre>
 * 0 – Tên KH          (bắt buộc)
 * 1 – SĐT chính       (bắt buộc, 10 chữ số, bắt đầu 0)
 * 2 – SĐT phụ         (tuỳ chọn)
 * 3 – Địa chỉ         (bắt buộc)
 * 4 – Ghi chú đặc biệt (tuỳ chọn)
 * 5 – Giờ ưa thích     (tuỳ chọn)
 * 6 – Nguồn KH         (ZALO/FACEBOOK/REFERRAL/HOTLINE/OTHER — mặc định OTHER)
 * </pre>
 *
 * <h3>Chiến lược geocoding</h3>
 * <ul>
 *   <li>Xử lý theo batch 10 dòng, mỗi batch geocode song song</li>
 *   <li>Rate limit: max 10 request đồng thời đến Google Maps API</li>
 *   <li>Nếu geocoding thất bại → import vẫn tiếp tục, thêm vào danh sách warnings</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerImportService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    // Column indices
    private static final int COL_NAME       = 0;
    private static final int COL_PHONE      = 1;
    private static final int COL_SEC_PHONE  = 2;
    private static final int COL_ADDRESS    = 3;
    private static final int COL_NOTES      = 4;
    private static final int COL_PREF_TIME  = 5;
    private static final int COL_SOURCE     = 6;
    private static final int COL_LAT        = 7;
    private static final int COL_LNG        = 8;

    private static final int BATCH_SIZE     = 10;

    /** Giới hạn số request đồng thời đến Google Maps API */
    private static final Semaphore GEOCODE_SEMAPHORE = new Semaphore(10);

    private final CustomerRepository customerRepository;
    private final GeocodingService   geocodingService;
    private final DataFormatter      dataFormatter = new DataFormatter();

    // ─────────────────────────────────────────────────────────────────────────
    // Import
    // ─────────────────────────────────────────────────────────────────────────

    public CustomerImportResult importFromExcel(MultipartFile file) {
        validateFile(file);

        List<CustomerImportRow> rows;
        try {
            rows = parseExcel(file);
        } catch (IOException e) {
            throw new BadRequestException("IMPORT_FILE_INVALID");
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("IMPORT_FILE_INVALID");
        }

        Set<String> seenPhones = new HashSet<>();

        int successCount = 0;
        int noGpsCount   = 0;
        List<CustomerImportResult.ImportError>   errors   = new ArrayList<>();
        List<CustomerImportResult.ImportWarning> warnings = new ArrayList<>();

        // Chia thành batch 10 dòng
        for (int batchStart = 0; batchStart < rows.size(); batchStart += BATCH_SIZE) {
            int batchEnd   = Math.min(batchStart + BATCH_SIZE, rows.size());
            List<CustomerImportRow> batch = rows.subList(batchStart, batchEnd);

            // Geocode song song cho cả batch (chỉ những dòng thiếu tọa độ trong Excel)
            List<CompletableFuture<GeoPoint>> geocodeFutures = new ArrayList<>();
            for (CustomerImportRow row : batch) {
                if (row.getLatitude() == null || row.getLongitude() == null) {
                    if (row.getAddress() != null && !row.getAddress().isBlank()) {
                        geocodeFutures.add(geocodeWithSemaphore(row.getAddress()));
                    } else {
                        geocodeFutures.add(CompletableFuture.completedFuture(null));
                    }
                } else {
                    // Đã có tọa độ trong Excel -> không cần geocode
                    geocodeFutures.add(CompletableFuture.completedFuture(null));
                }
            }

            // Xử lý từng dòng trong batch
            for (int i = 0; i < batch.size(); i++) {
                CustomerImportRow row    = batch.get(i);
                GeoPoint          geoPoint;
                try {
                    geoPoint = geocodeFutures.get(i).get();
                } catch (Exception e) {
                    geoPoint = null;
                }

                // Validate
                String errorReason = validateRow(row, seenPhones);
                if (errorReason != null) {
                    errors.add(CustomerImportResult.ImportError.builder()
                            .row(row.getRowNumber())
                            .reason(errorReason)
                            .build());
                    continue;
                }

                seenPhones.add(row.getPhone());

                // GPS warning nếu cả Excel và geocoding đều không có
                boolean hasExcelGps = row.getLatitude() != null && row.getLongitude() != null;
                if (!hasExcelGps && geoPoint == null) {
                    noGpsCount++;
                    warnings.add(CustomerImportResult.ImportWarning.builder()
                            .row(row.getRowNumber())
                            .name(row.getName())
                            .reason("Không tìm được tọa độ GPS")
                            .build());
                }

                try {
                    createOrUpdateCustomerFromRow(row, geoPoint);
                    successCount++;
                } catch (Exception ex) {
                    log.warn("Import row {} thất bại: {}", row.getRowNumber(), ex.getMessage());
                    errors.add(CustomerImportResult.ImportError.builder()
                            .row(row.getRowNumber())
                            .reason(ex.getMessage())
                            .build());
                }
            }
        }

        log.info("Import KH: success={} failed={} noGps={}", successCount, errors.size(), noGpsCount);

        return CustomerImportResult.builder()
                .success(successCount)
                .failed(errors.size())
                .noGps(noGpsCount)
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Export
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Xuất danh sách khách hàng ra file Excel (.xlsx).
     */
    public byte[] exportToExcel(List<Customer> customers) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Khách hàng");

            // Style cho Header
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Tên KH",
                    "SĐT chính",
                    "SĐT phụ",
                    "Địa chỉ",
                    "Ghi chú đặc biệt",
                    "Giờ ưa thích",
                    "Nguồn KH",
                    "Trạng thái",
                    "Latitude",
                    "Longitude"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill dữ liệu
            int rowIndex = 1;
            for (Customer c : customers) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(c.getName());
                row.createCell(1).setCellValue(c.getPhone());
                row.createCell(2).setCellValue(c.getSecondaryPhone() != null ? c.getSecondaryPhone() : "");
                row.createCell(3).setCellValue(c.getAddress());
                row.createCell(4).setCellValue(c.getSpecialNotes() != null ? c.getSpecialNotes() : "");
                row.createCell(5).setCellValue(c.getPreferredTimeNote() != null ? c.getPreferredTimeNote() : "");
                row.createCell(6).setCellValue(c.getSource().name());
                row.createCell(7).setCellValue(c.getStatus().name());
                row.createCell(8).setCellValue(c.getLatitude() != null ? String.valueOf(c.getLatitude()) : "");
                row.createCell(9).setCellValue(c.getLongitude() != null ? String.valueOf(c.getLongitude()) : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Template Download
    // ─────────────────────────────────────────────────────────────────────────

    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Khách hàng");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Tên KH *",
                    "SĐT chính *",
                    "SĐT phụ",
                    "Địa chỉ *",
                    "Ghi chú đặc biệt",
                    "Giờ ưa thích",
                    "Nguồn KH (ZALO/FACEBOOK/REFERRAL/HOTLINE/OTHER)"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Dòng mẫu
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("Nguyễn Thị A");
            sample.createCell(1).setCellValue("0901234567");
            sample.createCell(2).setCellValue("0987654321");
            sample.createCell(3).setCellValue("123 Nguyễn Văn B, Phường 1, Quận 1, TP.HCM");
            sample.createCell(4).setCellValue("Có chó to, bấm chuông 2 lần");
            sample.createCell(5).setCellValue("Buổi sáng 8-10h");
            sample.createCell(6).setCellValue("ZALO");

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validate một dòng import.
     *
     * @return null nếu hợp lệ, hoặc thông báo lỗi đầu tiên
     */
    public String validateRow(CustomerImportRow row, Set<String> seenPhones) {
        // Tên
        if (row.getName() == null || row.getName().isBlank()) {
            return "Thiếu tên khách hàng";
        }

        // SĐT chính
        if (row.getPhone() == null || !PHONE_PATTERN.matcher(row.getPhone()).matches()) {
            return "SĐT không hợp lệ (phải 10 chữ số, bắt đầu bằng 0)";
        }

        // Địa chỉ
        if (row.getAddress() == null || row.getAddress().isBlank()) {
            return "Thiếu địa chỉ";
        }

        // Trùng trong file (không cho phép)
        if (seenPhones.contains(row.getPhone())) {
            return "SĐT trùng lặp trong file";
        }

        // Trùng trong DB -> OK (chúng ta sẽ cập nhật)
        return null; // hợp lệ
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOrUpdateCustomerFromRow(CustomerImportRow row, GeoPoint geoPoint) {
        Customer customer = customerRepository.findByPhoneAndDeletedAtIsNull(row.getPhone())
                .orElseGet(() -> Customer.builder().phone(row.getPhone()).build());

        customer.setName(row.getName().trim());
        customer.setSecondaryPhone(blankToNull(row.getSecondaryPhone()));
        customer.setAddress(row.getAddress().trim());
        customer.setSpecialNotes(blankToNull(row.getSpecialNotes()));
        customer.setPreferredTimeNote(blankToNull(row.getPreferredTimeNote()));
        if (row.getSource() != null) {
            customer.setSource(row.getSource());
        } else if (customer.getSource() == null) {
            customer.setSource(CustomerSource.OTHER);
        }
        
        if (customer.getStatus() == null) {
            customer.setStatus(CustomerStatus.ACTIVE);
        }

        // Tọa độ: Ưu tiên trong Excel -> rồi đến Geocode -> rồi đến giữ nguyên cũ
        if (row.getLatitude() != null && row.getLongitude() != null) {
            customer.setLatitude(row.getLatitude());
            customer.setLongitude(row.getLongitude());
        } else if (geoPoint != null) {
            customer.setLatitude(geoPoint.latitude());
            customer.setLongitude(geoPoint.longitude());
        }

        customerRepository.save(customer);
        boolean hasGps = customer.getLatitude() != null && customer.getLongitude() != null;
        log.info("Import/Update KH: row={} phone={} hasGps={}", row.getRowNumber(), row.getPhone(), hasGps);
    }

    /** Geocode với rate-limiting (semaphore max 10 concurrent) */
    private CompletableFuture<GeoPoint> geocodeWithSemaphore(String address) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                GEOCODE_SEMAPHORE.acquire();
                try {
                    return geocodingService.geocode(address);
                } finally {
                    GEOCODE_SEMAPHORE.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        });
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("IMPORT_FILE_INVALID");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BadRequestException("IMPORT_FILE_INVALID");
        }
    }

    private List<CustomerImportRow> parseExcel(MultipartFile file) throws IOException {
        List<CustomerImportRow> rows = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;

                CustomerImportRow dto = new CustomerImportRow();
                dto.setRowNumber(r + 1); // 1-indexed (header = dòng 1)
                dto.setName(getCellString(row, COL_NAME));
                dto.setPhone(getCellString(row, COL_PHONE));
                dto.setSecondaryPhone(getCellString(row, COL_SEC_PHONE));
                dto.setAddress(getCellString(row, COL_ADDRESS));
                dto.setSpecialNotes(getCellString(row, COL_NOTES));
                dto.setPreferredTimeNote(getCellString(row, COL_PREF_TIME));
                dto.setSource(parseSource(getCellString(row, COL_SOURCE)));
                dto.setLatitude(getCellDouble(row, COL_LAT));
                dto.setLongitude(getCellDouble(row, COL_LNG));
                rows.add(dto);
            }
        }

        return rows;
    }

    private Double getCellDouble(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
            if (cell.getCellType() == CellType.STRING) return Double.parseDouble(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> dataFormatter.formatCellValue(cell).trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int c = COL_NAME; c <= COL_SOURCE; c++) {
            String val = getCellString(row, c);
            if (val != null && !val.isBlank()) return false;
        }
        return true;
    }

    private CustomerSource parseSource(String raw) {
        if (raw == null || raw.isBlank()) return CustomerSource.OTHER;
        try {
            return CustomerSource.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CustomerSource.OTHER;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
