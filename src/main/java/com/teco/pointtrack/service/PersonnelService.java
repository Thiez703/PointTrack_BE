package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.personnel.EmployeePageRequest;
import com.teco.pointtrack.dto.personnel.UpdateEmployeeRequest;
import com.teco.pointtrack.dto.personnel.UpdateEmployeeStatusRequest;
import com.teco.pointtrack.dto.user.RoleDto;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonnelService {

    private final UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy danh sách nhân viên (phân trang + lọc)
    // GET /api/v1/personnel
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<UserDetail> getEmployees(EmployeePageRequest req) {

        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // BR-22: luôn loại bỏ user đã soft delete
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Tìm kiếm theo fullName hoặc email (case-insensitive)
            if (req.getSearch() != null && !req.getSearch().isBlank()) {
                String search = "%" + req.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), search),
                        cb.like(cb.lower(root.get("email")), search)
                ));
            }

            // Lọc theo trạng thái
            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(this::toUserDetail);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy thông tin nhân viên theo ID
    // GET /api/v1/personnel/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserDetail getEmployeeById(Long id) {
        User user = findActiveUser(id);
        return toUserDetail(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cập nhật thông tin nhân viên
    // PUT /api/v1/personnel/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public UserDetail updateEmployee(Long id, UpdateEmployeeRequest request) {
        User user = findActiveUser(id);

        // Chỉ cập nhật các field khác null
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        // Kiểm tra uniqueness số điện thoại
        if (request.getPhoneNumber() != null) {
            boolean phoneConflict = userRepository
                    .findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                    .filter(u -> !u.getId().equals(id))
                    .isPresent();
            if (phoneConflict) {
                throw new ConflictException("PHONE_NUMBER_CONFLICT");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            user.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            user.setStartDate(LocalDate.parse(request.getStartDate()));
        }

        userRepository.save(user);
        return toUserDetail(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cập nhật trạng thái nhân viên
    // PATCH /api/v1/personnel/{id}/status
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public UserDetail updateEmployeeStatus(Long id, UpdateEmployeeStatusRequest request) {
        User user = findActiveUser(id);

        // Không cho phép cập nhật nếu trạng thái không thay đổi
        if (user.getStatus() == request.getStatus()) {
            throw new BadRequestException("INVALID_USER_STATUS_REQUEST");
        }

        user.setStatus(request.getStatus());
        userRepository.save(user);
        return toUserDetail(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Xoá nhân viên (soft delete — BR-22)
    // DELETE /api/v1/personnel/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteEmployee(Long id) {
        User user = findActiveUser(id);

        // BR-22: soft delete — đánh dấu deletedAt và set INACTIVE
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        log.info("Soft-deleted employee id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tìm user chưa bị soft delete. Ném NotFoundException nếu không tìm thấy.
     */
    private User findActiveUser(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", id));
    }

    /**
     * Map entity User → DTO UserDetail
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
                .role(user.getRole() != null ? new RoleDto(user.getRole()) : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

