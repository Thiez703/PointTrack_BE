package com.teco.pointtrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lưu file vào local disk và trả về URL tương đối.
 *
 * TODO (production): Swap sang S3Service / CloudinaryService bằng cách:
 *   1. Extract interface FileStoragePort với method store(MultipartFile) → String
 *   2. Implement S3FileStorageService implements FileStoragePort
 *   3. Đổi @Primary hoặc dùng @ConditionalOnProperty
 *
 * Static resources được serve bởi WebMvcConfig (xem cấu hình bên dưới):
 *   registry.addResourceHandler("/uploads/**")
 *           .addResourceLocations("file:" + uploadDir + "/");
 */
@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Lưu file vào thư mục theo cấu trúc: {uploadDir}/attendance/{year}/{month}/{day}/
     *
     * @return Full URL có thể truy cập từ client
     */
    public String storeAttendancePhoto(MultipartFile file) {
        return storeFile(file, "attendance");
    }

    /**
     * Upload file tạm thời (avatar, tài liệu...).
     * Lưu vào: {uploadDir}/tmp/{year}/{month}/{day}/
     *
     * @return Full URL có thể truy cập từ client
     */
    public String storeTmpFile(MultipartFile file) {
        validateFile(file);
        return storeFile(file, "tmp");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String storeFile(MultipartFile file, String category) {
        try {
            String ext      = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + ext;
            LocalDate today = LocalDate.now();
            String subPath  = "%s/%d/%02d/%02d".formatted(
                    category, today.getYear(), today.getMonthValue(), today.getDayOfMonth());

            Path targetDir = Paths.get(uploadDir, subPath);
            try {
                Files.createDirectories(targetDir);
            } catch (Exception e) {
                System.err.println("Could not create directories: " + e.getMessage());
                return "default_photo.jpg"; // Fallback
            }

            Path targetPath = targetDir.resolve(fileName);
            try {
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                System.err.println("Could not copy file: " + e.getMessage());
                return "default_photo.jpg"; // Fallback
            }

            return baseUrl + "/api/uploads/" + subPath + "/" + fileName;

        } catch (Exception e) {
            return "default_photo.jpg";
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được rỗng");
        }
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (!java.util.Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext)) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh: jpg, jpeg, png, webp, gif");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
