package com.nimbusdrive.service;

import com.nimbusdrive.dto.LoginRequest;
import com.nimbusdrive.dto.RegisterRequest;
import com.nimbusdrive.model.User;
import com.nimbusdrive.repository.UserRepository;
import com.nimbusdrive.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service

public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public String register(RegisterRequest request){
        //Step1 : check if username exists
        if(userRepository.findByUsername(request.getUsername()).isPresent()){
            return "Username already taken !";
        }

        //Step 2 : check if email exists
        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            return "Email already registered !";
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

    public String login(LoginRequest request) {

        // Step 1: Find by username or email
        User user = userRepository
                .findByUsername(request.getUsernameOrEmail())
                .orElseGet(() -> userRepository
                        .findByEmail(request.getUsernameOrEmail())
                        .orElse(null));

        if(user == null) {
            return "User not found!";
        }

        // Step 2: Check password
        if(!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {
            return "Invalid password!";
        }

        // Step 3: Generate and return token
        return jwtUtil.generateToken(user.getUsername());
    }




}
