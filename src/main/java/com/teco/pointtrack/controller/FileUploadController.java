package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Upload file tạm (avatar, ảnh tài liệu...).
 * FE upload trước → nhận URL → đính kèm vào request tạo/cập nhật.
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload", description = "Upload file lên server")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    /**
     * POST /api/v1/files/upload
     * Content-Type: multipart/form-data
     *
     * Form field: "file"
     */
    @Operation(summary = "Upload file tạm (avatar, ảnh...)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadTmpFile(
            @RequestParam("file") MultipartFile file) {

        log.info("Upload file: name={}, size={} bytes, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        String url = fileStorageService.storeTmpFile(file);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("url", url),
                "Upload thành công"
        ));
    }
}
