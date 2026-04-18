package com.nimbusdrive.service;

import com.nimbusdrive.dto.LoginResponse;
import com.nimbusdrive.dto.LoginRequest;
import com.nimbusdrive.dto.RegisterRequest;
import com.nimbusdrive.model.User;
import com.nimbusdrive.repository.UserRepository;
import com.nimbusdrive.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TwoFactorService twoFactorService;

    public String register(RegisterRequest request){
        //Step1 : check if username exists
        if(userRepository.findByUsername(request.getUsername()).isPresent()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken!");
        }

        //Step 2 : check if email exists
        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered!");
        }

        //Step 3 : Create new user object
        User user =new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        //Step 4 : Encrypt password
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        //Step 5 : Save to database
        userRepository.save(user);

        //Step 6 : Return success
        return "User registered successfully !";
    }

    public LoginResponse login(LoginRequest request) {

        // Step 1: Find by username or email
        User user = userRepository
                .findByUsername(request.getUsernameOrEmail())
                .orElseGet(() -> userRepository
                        .findByEmail(request.getUsernameOrEmail())
                        .orElse(null));

        if(user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials!");
        }

        // Step 2: Check password
        if(!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials!");
        }

        // Step 3: If 2FA enabled, send OTP and require verification
        if (Boolean.TRUE.equals(user.getIs2faEnabled())) {
            twoFactorService.generateAndSendOtp(user);
            return LoginResponse.builder()
                    .requires2FA(true)
                    .message("OTP sent to your email")
                    .build();
        }

        // Step 4: Generate and return token
        String jwt = jwtUtil.generateToken(user.getUsername());
        return LoginResponse.builder()
                .token(jwt)
                .requires2FA(false)
                .build();
    }

    public LoginResponse verifyOtpAndLogin(String username, String otpCode) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials!");
        }

        boolean isValid = twoFactorService.verifyOtp(user, otpCode);
        if (!isValid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }

        String jwt = jwtUtil.generateToken(user.getUsername());
        return LoginResponse.builder()
                .token(jwt)
                .requires2FA(false)
                .build();
    }




}
