package com.nimbusdrive.repository;

import com.nimbusdrive.model.OtpToken;
import com.nimbusdrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findByUserAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(User user, String otpCode, LocalDateTime now);

    void deleteAllByUser(User user);

    Optional<OtpToken> findFirstByUserAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(User user, LocalDateTime now);
}

