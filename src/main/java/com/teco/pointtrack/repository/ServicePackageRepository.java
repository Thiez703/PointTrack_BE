package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.ServicePackage;
import com.teco.pointtrack.entity.enums.PackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {

    Optional<ServicePackage> findByIdAndStatusNot(Long id, PackageStatus status);

    List<ServicePackage> findAllByOrderByCreatedAtDesc();

    boolean existsByTemplateIdAndStatusNot(Long templateId, PackageStatus status);
}
