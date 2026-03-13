package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
