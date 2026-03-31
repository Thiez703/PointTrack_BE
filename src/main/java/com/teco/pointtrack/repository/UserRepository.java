package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmail(String email);

    /** FR-04: tìm user theo reset token để đặt lại mật khẩu */
    Optional<User> findByResetPasswordToken(String token);

    @EntityGraph(attributePaths = {"role"})
    org.springframework.data.domain.Page<User> findAll(org.springframework.data.jpa.domain.Specification<User> spec, org.springframework.data.domain.Pageable pageable);

    /** Chỉ lấy user chưa bị soft delete */
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    /** Tìm user theo số điện thoại (có thể đã bị soft delete) */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /** Tìm user theo email (có thể đã bị soft delete) */
    Optional<User> findByEmail(String email);

    /** Tìm user theo số điện thoại, chưa bị soft delete */
    Optional<User> findByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    /** Kiểm tra nhân viên đang dùng salary level — dùng cho delete validation */
    boolean existsBySalaryLevelIdAndDeletedAtIsNull(Long salaryLevelId);

    /** Lấy tất cả nhân viên đang hoạt động (dùng cho available-employees) */
    List<User> findAllByStatusAndDeletedAtIsNull(UserStatus status);

    /** Kiểm tra email đã tồn tại (chưa bị xóa) */
    boolean existsByEmailAndDeletedAtIsNull(String email);

    /** Kiểm tra phone đã tồn tại (chưa bị xóa) */
    boolean existsByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    /** Kiểm tra phone đã tồn tại (kể cả soft-deleted — tránh vi phạm DB unique) */
    boolean existsByPhoneNumber(String phoneNumber);

    /** Đếm nhân viên theo cấp bậc lương (dùng khi xóa salary level) */
    @Query("SELECT COUNT(u) FROM User u WHERE u.salaryLevel.id = :levelId AND u.deletedAt IS NULL")
    long countBySalaryLevelId(@Param("levelId") Long levelId);

    /**
     * Thống kê tổng hợp nhân viên — 1 query duy nhất, tránh N round-trips.
     *
     * Trả về Object[4]:
     *   [0] total      – tổng NV chưa bị soft delete
     *   [1] active     – ACTIVE
     *   [2] onLeave    – ON_LEAVE
     *   [3] newThisMonth – startDate trong khoảng [monthStart, monthEnd]
     */
    @Query("""
            SELECT
                COUNT(u),
                SUM(CASE WHEN u.status = com.teco.pointtrack.entity.enums.UserStatus.ACTIVE   THEN 1 ELSE 0 END),
                SUM(CASE WHEN u.status = com.teco.pointtrack.entity.enums.UserStatus.ON_LEAVE THEN 1 ELSE 0 END),
                SUM(CASE WHEN u.startDate >= :monthStart AND u.startDate <= :monthEnd          THEN 1 ELSE 0 END)
            FROM User u
            WHERE u.deletedAt IS NULL
            """)
    List<Object[]> fetchSummaryStats(
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd")   LocalDate monthEnd);

    /**
     * Thống kê nhân sự nâng cao — 1 query duy nhất, kèm dữ liệu tháng trước để tính trend.
     *
     * Trả về Object[5]:
     *   [0] total           – tổng NV chưa soft delete
     *   [1] active          – ACTIVE
     *   [2] onLeave         – ON_LEAVE
     *   [3] newThisMonth    – createdAt trong [thisStart, thisEnd]
     *   [4] newLastMonth    – createdAt trong [lastStart, lastEnd]
     */
    @Query("""
            SELECT
                COUNT(u),
                SUM(CASE WHEN u.status = com.teco.pointtrack.entity.enums.UserStatus.ACTIVE   THEN 1 ELSE 0 END),
                SUM(CASE WHEN u.status = com.teco.pointtrack.entity.enums.UserStatus.ON_LEAVE THEN 1 ELSE 0 END),
                SUM(CASE WHEN u.createdAt >= :thisStart AND u.createdAt <= :thisEnd THEN 1 ELSE 0 END),
                SUM(CASE WHEN u.createdAt >= :lastStart AND u.createdAt <= :lastEnd THEN 1 ELSE 0 END)
            FROM User u
            WHERE u.deletedAt IS NULL
            """)
    List<Object[]> fetchStatistics(
            @Param("thisStart") LocalDateTime thisStart,
            @Param("thisEnd")   LocalDateTime thisEnd,
            @Param("lastStart") LocalDateTime lastStart,
            @Param("lastEnd")   LocalDateTime lastEnd);
}
