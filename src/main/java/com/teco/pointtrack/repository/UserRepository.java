package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /** Chỉ lấy user chưa bị soft delete */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmail(String email);

    /** FR-04: tìm user theo reset token để đặt lại mật khẩu */
    Optional<User> findByResetPasswordToken(String token);
}
