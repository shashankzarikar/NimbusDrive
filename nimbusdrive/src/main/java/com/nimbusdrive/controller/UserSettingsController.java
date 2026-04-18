package com.nimbusdrive.controller;

import com.nimbusdrive.model.User;
import com.nimbusdrive.repository.UserRepository;
import com.nimbusdrive.service.TwoFactorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserSettingsController {

    private final TwoFactorService twoFactorService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/2fa/status")
    public ResponseEntity<Map<String, Object>> get2faStatus() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(Map.of("is2faEnabled", user.getIs2faEnabled()));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<Map<String, Object>> enable2fa(@RequestBody Map<String, String> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String password = body.get("password");
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        String otpCode = body.get("otpCode");
        if (otpCode == null || otpCode.isBlank()) {
            twoFactorService.generateAndSendOtp(user);
            return ResponseEntity.ok(Map.of(
                    "requiresOtp", true,
                    "message", "OTP sent to your email"
            ));
        }

        boolean valid = twoFactorService.verifyOtp(user, otpCode);
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }

        twoFactorService.enable2FA(user);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Two-factor authentication enabled"
        ));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Map<String, Object>> disable2fa(@RequestBody Map<String, String> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String password = body.get("password");
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        twoFactorService.disable2FA(user);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Two-factor authentication disabled"
        ));
    }
}
