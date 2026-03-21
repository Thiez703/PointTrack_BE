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
        try {
            String ext        = getExtension(file.getOriginalFilename());
            String fileName   = UUID.randomUUID() + ext;
            LocalDate today   = LocalDate.now();
            String subPath    = "attendance/%d/%02d/%02d".formatted(
                    today.getYear(), today.getMonthValue(), today.getDayOfMonth());

            Path targetDir = Paths.get(uploadDir, subPath);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Trả về URL đầy đủ để FE có thể hiển thị
            return baseUrl + "/uploads/" + subPath + "/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file ảnh: " + e.getMessage(), e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
