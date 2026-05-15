package com.nimbusdrive.service;

import com.nimbusdrive.dto.StorageResponse;
import com.nimbusdrive.model.User;
import com.nimbusdrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public StorageResponse getStorageInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return StorageResponse.builder()
                .storageUsed(user.getStorageUsed())
                .storageLimit(user.getStorageLimit())
                .build();
    }
}

