package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.personnel.EmployeePageRequest;
import com.teco.pointtrack.dto.personnel.UpdateEmployeeRequest;
import com.teco.pointtrack.dto.personnel.UpdateEmployeeStatusRequest;
import com.teco.pointtrack.dto.user.RoleDto;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.entity.SalaryLevel;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.SalaryLevelRepository;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonnelService {

    private final UserRepository userRepository;
    private final SalaryLevelRepository salaryLevelRepository;

    @Transactional(readOnly = true)
    public Page<UserDetail> getEmployees(EmployeePageRequest req) {
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (req.getSearch() != null && !req.getSearch().isBlank()) {
                String search = "%" + req.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), search),
                        cb.like(cb.lower(root.get("phoneNumber")), search)
                ));
            }
            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return userRepository.findAll(spec, pageable).map(this::toUserDetail);
    }

    @Transactional(readOnly = true)
    public UserDetail getEmployeeById(Long id) {
        User user = findActiveUser(id);
        return toUserDetail(user);
    }

    @Transactional
    public UserDetail updateEmployee(Long id, UpdateEmployeeRequest request) {
        User user = findActiveUser(id);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhoneNumber() != null) {
            boolean phoneConflict = userRepository
                    .findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                    .filter(u -> !u.getId().equals(id))
                    .isPresent();
            if (phoneConflict) {
                throw new ConflictException("Số điện thoại đã được sử dụng");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            user.setDateOfBirth(parseDate(request.getDateOfBirth()));
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            user.setStartDate(parseDate(request.getStartDate()));
        }

        userRepository.save(user);
        return toUserDetail(user);
    }

    @Transactional
    public UserDetail assignSalaryLevel(Long id, Long salaryLevelId) {
        User user = findActiveUser(id);
        SalaryLevel salaryLevel = salaryLevelRepository.findByIdAndDeletedAtIsNull(salaryLevelId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấp bậc lương ID=" + salaryLevelId));
        
        user.setSalaryLevel(salaryLevel);
        userRepository.save(user);
        log.info("Assigned salary level {} to employee {}", salaryLevel.getName(), user.getFullName());
        return toUserDetail(user);
    }

    @Transactional
    public UserDetail updateEmployeeStatus(Long id, UpdateEmployeeStatusRequest request) {
        User user = findActiveUser(id);
        if (user.getStatus() == request.getStatus()) {
            throw new BadRequestException("Trạng thái không thay đổi");
        }
        user.setStatus(request.getStatus());
        userRepository.save(user);
        return toUserDetail(user);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        User user = findActiveUser(id);
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("Soft-deleted employee id={}", id);
    }

    private User findActiveUser(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên ID=" + id));
    }

    private LocalDate parseDate(String dateStr) {
        String[] formats = {"dd/MM/yyyy", "yyyy-MM-dd"};
        for (String format : formats) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException ignored) {}
        }
        throw new BadRequestException("Định dạng ngày tháng không hợp lệ (hỗ trợ dd/MM/yyyy hoặc yyyy-MM-dd): " + dateStr);
    }

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
                .salaryLevelId(user.getSalaryLevel() != null ? user.getSalaryLevel().getId() : null)
                .salaryLevelName(user.getSalaryLevel() != null ? user.getSalaryLevel().getName() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
