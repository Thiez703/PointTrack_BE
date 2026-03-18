package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.salary.SalaryLevelRequest;
import com.teco.pointtrack.dto.salary.SalaryLevelResponse;
import com.teco.pointtrack.dto.user.RoleDto;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.entity.SalaryLevel;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.SalaryLevelRepository;
import com.teco.pointtrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryLevelService {

    private final SalaryLevelRepository salaryLevelRepository;
    private final UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy danh sách cấp bậc
    // GET /api/v1/salary-levels
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SalaryLevelResponse> getAllSalaryLevels() {
        return salaryLevelRepository.findAllByDeletedAtIsNullOrderByBaseSalaryAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy thông tin cấp bậc theo ID
    // GET /api/v1/salary-levels/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SalaryLevelResponse getSalaryLevelById(Long id) {
        SalaryLevel level = findActiveLevel(id);
        return toResponse(level);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tạo cấp bậc mới
    // POST /api/v1/salary-levels
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public SalaryLevelResponse createSalaryLevel(SalaryLevelRequest request) {

        // Kiểm tra tên trùng
        if (salaryLevelRepository.existsByNameAndDeletedAtIsNull(request.getName())) {
            throw new ConflictException("SALARY_LEVEL_NAME_CONFLICT");
        }

        SalaryLevel level = SalaryLevel.builder()
                .name(request.getName().trim())
                .baseSalary(request.getBaseSalary())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        SalaryLevel saved = salaryLevelRepository.save(level);
        log.info("Created salary level: name={}", saved.getName());

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cập nhật cấp bậc
    // PUT /api/v1/salary-levels/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public SalaryLevelResponse updateSalaryLevel(Long id, SalaryLevelRequest request) {
        SalaryLevel level = findActiveLevel(id);

        // Kiểm tra tên trùng với level khác
        if (salaryLevelRepository.existsByNameAndIdNotAndDeletedAtIsNull(request.getName(), id)) {
            throw new ConflictException("SALARY_LEVEL_NAME_CONFLICT");
        }

        level.setName(request.getName().trim());
        level.setBaseSalary(request.getBaseSalary());
        level.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            level.setIsActive(request.getIsActive());
        }

        salaryLevelRepository.save(level);
        return toResponse(level);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Xoá cấp bậc (soft delete)
    // DELETE /api/v1/salary-levels/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteSalaryLevel(Long id) {
        SalaryLevel level = findActiveLevel(id);

        // Kiểm tra có nhân viên nào đang dùng level này không
        if (userRepository.existsBySalaryLevelIdAndDeletedAtIsNull(id)) {
            throw new BadRequestException("SALARY_LEVEL_IN_USE");
        }

        // BR-22: soft delete
        level.setDeletedAt(LocalDateTime.now());
        level.setIsActive(false);
        salaryLevelRepository.save(level);

        log.info("Soft-deleted salary level id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gán cấp bậc cho nhân viên
    // PATCH /api/v1/salary-levels/{id}/assign/{employeeId}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public UserDetail assignSalaryLevelToEmployee(Long employeeId, Long salaryLevelId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(employeeId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", employeeId));

        SalaryLevel salaryLevel = findActiveLevel(salaryLevelId);

        user.setSalaryLevel(salaryLevel);
        userRepository.save(user);

        return toUserDetail(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tìm cấp bậc chưa bị soft delete. Ném NotFoundException nếu không tìm thấy.
     */
    private SalaryLevel findActiveLevel(Long id) {
        return salaryLevelRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("SALARY_LEVEL_NOT_FOUND", id));
    }

    /**
     * Map entity SalaryLevel → DTO SalaryLevelResponse
     */
    private SalaryLevelResponse toResponse(SalaryLevel level) {
        return SalaryLevelResponse.builder()
                .id(level.getId())
                .name(level.getName())
                .baseSalary(level.getBaseSalary())
                .description(level.getDescription())
                .isActive(level.getIsActive())
                .createdAt(level.getCreatedAt())
                .updatedAt(level.getUpdatedAt())
                .build();
    }

    /**
     * Map entity User → DTO UserDetail (tương tự PersonnelService)
     */
    private UserDetail toUserDetail(User user) {
        return UserDetail.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .status(user.getStatus())
                .role(user.getRole() != null ? user.getRole().getSlug() : null)
                .roleName(user.getRole() != null ? user.getRole().getDisplayName() : null)
                .salaryLevel(user.getSalaryLevel() != null ? toResponse(user.getSalaryLevel()) : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

