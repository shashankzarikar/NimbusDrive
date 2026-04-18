package com.nimbusdrive.controller;

import com.nimbusdrive.dto.LoginRequest;
import com.nimbusdrive.dto.LoginResponse;
import com.nimbusdrive.dto.RegisterRequest;
import com.nimbusdrive.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String register(@Valid @RequestBody RegisterRequest request){
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-otp")
    public LoginResponse verifyOtp(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String otpCode = body.get("otpCode");
        return authService.verifyOtpAndLogin(username, otpCode);
    }

}
