package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.Customer;
import com.teco.pointtrack.entity.enums.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    // ── Tìm theo ID ───────────────────────────────────────────────────────────

    Optional<Customer> findByIdAndDeletedAtIsNull(Long id);

    // ── Kiểm tra trùng phone ──────────────────────────────────────────────────

    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    boolean existsByPhoneAndIdNotAndDeletedAtIsNull(String phone, Long id);

    Optional<Customer> findByPhoneAndDeletedAtIsNull(String phone);

    // ── Active với GPS — dùng cho dropdown tạo ca ────────────────────────────

    @Query("""
            SELECT c FROM Customer c
            WHERE c.status = com.teco.pointtrack.entity.enums.CustomerStatus.ACTIVE
              AND c.latitude  IS NOT NULL
              AND c.longitude IS NOT NULL
              AND c.deletedAt IS NULL
            ORDER BY c.name ASC
            """)
    List<Customer> findAllActiveWithGps();

    // ── Thống kê ca ───────────────────────────────────────────────────────────

    @Query("""
            SELECT COUNT(s) FROM Shift s
            WHERE s.customer.id = :customerId
              AND s.status <> com.teco.pointtrack.entity.enums.ShiftStatus.CANCELLED
            """)
    long countTotalShifts(@Param("customerId") Long customerId);

    @Query("""
            SELECT COUNT(s) FROM Shift s
            WHERE s.customer.id = :customerId
              AND s.status = com.teco.pointtrack.entity.enums.ShiftStatus.COMPLETED
            """)
    long countCompletedShifts(@Param("customerId") Long customerId);

    @Query("""
            SELECT COUNT(p) FROM ServicePackage p
            WHERE p.customer.id = :customerId
              AND p.status = com.teco.pointtrack.entity.enums.PackageStatus.ACTIVE
            """)
    long countActivePackages(@Param("customerId") Long customerId);

    // ── 10 ca gần nhất ───────────────────────────────────────────────────────

    @Query("""
            SELECT s FROM Shift s
            JOIN FETCH s.employee
            WHERE s.customer.id = :customerId
            ORDER BY s.shiftDate DESC, s.startTime DESC
            """)
    List<com.teco.pointtrack.entity.Shift> findRecentShifts(
            @Param("customerId") Long customerId,
            org.springframework.data.domain.Pageable pageable);

    // ── Soft-delete cascade: huỷ ca tương lai ────────────────────────────────

    @Query("""
            SELECT s FROM Shift s
            WHERE s.customer.id = :customerId
              AND s.shiftDate   >= :today
              AND s.status IN (
                  com.teco.pointtrack.entity.enums.ShiftStatus.SCHEDULED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.ASSIGNED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.CONFIRMED
              )
            """)
    List<com.teco.pointtrack.entity.Shift> findFutureScheduledShifts(
            @Param("customerId") Long customerId,
            @Param("today")      java.time.LocalDate today);
}
