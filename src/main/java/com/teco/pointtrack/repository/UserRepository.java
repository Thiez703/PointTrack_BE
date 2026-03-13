package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmail(String email);

    /** FR-04: tìm user theo reset token để đặt lại mật khẩu */
    Optional<User> findByResetPasswordToken(String token);

    @EntityGraph(attributePaths = {"role"})
    org.springframework.data.domain.Page<User> findAll(org.springframework.data.jpa.domain.Specification<User> spec, org.springframework.data.domain.Pageable pageable);

    /** Chỉ lấy user chưa bị soft delete */
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    /** Tìm user theo số điện thoại, chưa bị soft delete */
    Optional<User> findByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    /** Kiểm tra nhân viên đang dùng salary level — dùng cho delete validation */
    boolean existsBySalaryLevelIdAndDeletedAtIsNull(Long salaryLevelId);
}
