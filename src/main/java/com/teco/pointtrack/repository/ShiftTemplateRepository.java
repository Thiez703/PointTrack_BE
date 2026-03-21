package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.ShiftTemplate;
import com.teco.pointtrack.entity.enums.ShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
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
}