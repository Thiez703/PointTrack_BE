package com.chamcong.repository;

import com.chamcong.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.isUsed = true WHERE t.user.id = :userId AND t.isUsed = false")
    void invalidateAllByUserId(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}

