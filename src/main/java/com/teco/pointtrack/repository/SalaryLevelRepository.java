package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.SalaryLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryLevelRepository extends JpaRepository<SalaryLevel, Long> {

    /** Tìm cấp bậc theo tên, chưa bị soft delete */
    Optional<SalaryLevel> findByNameAndDeletedAtIsNull(String name);

    /** Kiểm tra tên cấp bậc trùng (chưa bị soft delete) — dùng khi tạo mới */
    boolean existsByNameAndDeletedAtIsNull(String name);

    /** Kiểm tra tên cấp bậc trùng với level khác (chưa bị soft delete) — dùng khi cập nhật */
    boolean existsByNameAndIdNotAndDeletedAtIsNull(String name, Long id);

    /** Lấy cấp bậc theo ID, chưa bị soft delete */
    Optional<SalaryLevel> findByIdAndDeletedAtIsNull(Long id);

    /** Lấy tất cả cấp bậc chưa bị xoá, sắp xếp theo baseSalary tăng dần */
    List<SalaryLevel> findAllByDeletedAtIsNullOrderByBaseSalaryAsc();

    /** Kiểm tra cấp bậc tồn tại và chưa bị xoá */
    boolean existsByIdAndDeletedAtIsNull(Long id);
}

