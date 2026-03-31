package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.ShiftTemplate;
import com.teco.pointtrack.entity.enums.ShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {

    Optional<ShiftTemplate> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByNameAndDeletedAtIsNull(String name);

    boolean existsByNameAndIdNotAndDeletedAtIsNull(String name, Long id);

    Optional<ShiftTemplate> findByNameAndDeletedAtIsNull(String name);

    List<ShiftTemplate> findAllByDeletedAtIsNullOrderByDefaultStartAsc();

    List<ShiftTemplate> findAllByShiftTypeAndDeletedAtIsNullOrderByDefaultStartAsc(ShiftType shiftType);

    /** BR TEMPLATE_IN_USE: kiểm tra template có đang được dùng bởi shift hoặc package chưa */
    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
            FROM Shift s
            WHERE s.template.id = :templateId
              AND s.status <> com.teco.pointtrack.entity.enums.ShiftStatus.CANCELLED
            """)
    boolean isUsedByActiveShifts(@Param("templateId") Long templateId);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
            FROM ServicePackage p
            WHERE p.template.id = :templateId
              AND p.status <> com.teco.pointtrack.entity.enums.PackageStatus.CANCELLED
            """)
    boolean isUsedByActivePackages(@Param("templateId") Long templateId);
}