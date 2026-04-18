package com.nimbusdrive.service;

import com.nimbusdrive.model.OtpToken;
import com.nimbusdrive.model.User;
import com.nimbusdrive.repository.OtpTokenRepository;
import com.nimbusdrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    @Transactional
    public void generateAndSendOtp(User user) {
        otpTokenRepository.deleteAllByUser(user);


        String otpCode = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        OtpToken token = OtpToken.builder()
                .user(user)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        otpTokenRepository.save(token);

        emailService.sendEmail(
                user.getEmail(),
                "NimbusDrive — Your verification code",
                "Your NimbusDrive verification code is: " + otpCode + "\n\n" +
                        "This code expires in 10 minutes.\n" +
                        "Do not share this code with anyone."
        );
    }

    public boolean verifyOtp(User user, String otpCode) {
        return otpTokenRepository
                .findByUserAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(user, otpCode, LocalDateTime.now())
                .map(token -> {
                    token.setUsed(true);
                    otpTokenRepository.save(token);
                    return true;
                })
                .orElse(false);
    }

    public void enable2FA(User user) {
        user.setIs2faEnabled(true);
        userRepository.save(user);
    }

    public void disable2FA(User user) {
        user.setIs2faEnabled(false);
        userRepository.save(user);
    }
}

