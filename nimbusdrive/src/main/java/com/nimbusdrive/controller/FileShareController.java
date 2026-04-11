package com.nimbusdrive.controller;

import com.nimbusdrive.dto.ApiResponse;
import com.nimbusdrive.dto.FileShareResponse;
import com.nimbusdrive.model.FileShare;
import com.nimbusdrive.service.FileShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class FileShareController {

    private final FileShareService fileShareService;

    @PostMapping("/api/files/{id}/share")
    public ResponseEntity<FileShareResponse> createShareLink(
            @PathVariable("id") Long id,
            @RequestParam String duration
    ) {
        String username = getLoggedInUsername();
        FileShare share = fileShareService.createShareLink(id, username, duration);

        FileShareResponse response = mapToResponse(share);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/api/files/{id}/share")
    public ResponseEntity<?> getActiveShareLink(@PathVariable("id") Long id) {
        String username = getLoggedInUsername();
        Optional<FileShare> shareOpt = fileShareService.getActiveShareLink(id, username);

        if (shareOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse(false, "No active share link found"));
        }

        return ResponseEntity.ok(mapToResponse(shareOpt.get()));
    }

    @DeleteMapping("/api/files/{id}/share")
    public ResponseEntity<ApiResponse> revokeShareLink(@PathVariable("id") Long id) {
        String username = getLoggedInUsername();
        fileShareService.revokeShareLink(id, username);
        return ResponseEntity.ok(new ApiResponse(true, "Share link revoked successfully"));
    }

    @GetMapping("/api/files/shared")
    public ResponseEntity<List<FileShareResponse>> getAllSharedFiles() {
        String username = getLoggedInUsername();
        List<FileShare> shares = fileShareService.getAllSharedFiles(username);

        List<FileShareResponse> response = shares.stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    private String getLoggedInUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private FileShareResponse mapToResponse(FileShare share) {
        return FileShareResponse.builder()
                .fileId(share.getFile().getId())
                .fileName(share.getFile().getFileName())
                .shareToken(share.getShareToken())
                .expiresAt(share.getExpiresAt())
                .createdAt(share.getCreatedAt())
                .build();
    }
}

