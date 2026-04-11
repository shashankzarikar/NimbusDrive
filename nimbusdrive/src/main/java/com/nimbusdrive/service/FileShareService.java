package com.nimbusdrive.service;

import com.nimbusdrive.dto.FileDownloadResult;
import com.nimbusdrive.dto.ShareInfoResponse;
import com.nimbusdrive.model.FileEntity;
import com.nimbusdrive.model.FileShare;
import com.nimbusdrive.model.User;
import com.nimbusdrive.repository.FileRepository;
import com.nimbusdrive.repository.FileShareRepository;
import com.nimbusdrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileShareService {

    private static final Set<String> PREVIEWABLE_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "text/plain"
    );

    private final FileShareRepository fileShareRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    private LocalDateTime calculateExpiry(String duration) {
        if (duration == null) {
            return null;
        }

        return switch (duration) {
            case "1_HOUR" -> LocalDateTime.now().plusHours(1);
            case "24_HOURS" -> LocalDateTime.now().plusDays(1);
            case "7_DAYS" -> LocalDateTime.now().plusDays(7);
            case "30_DAYS" -> LocalDateTime.now().plusDays(30);
            case "NEVER" -> null;
            default -> null;
        };
    }

    private FileShare validateToken(String token) {
        FileShare share = fileShareRepository.findByShareToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Share link not found"));

        if (!share.isActive()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Share link has been revoked");
        }

        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Share link has expired");
        }

        return share;
    }

    public FileShare createShareLink(Long fileId, String username, String duration) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (!file.getUploadedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access to file");
        }

        Optional<FileShare> existingActive = fileShareRepository.findByFileAndIsActiveTrue(file);
        if (existingActive.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active share link already exists for this file");
        }

        FileShare share = FileShare.builder()
                .file(file)
                .createdBy(user)
                .shareToken(UUID.randomUUID().toString())
                .expiresAt(calculateExpiry(duration))
                .build();

        return fileShareRepository.save(share);
    }

    public Optional<FileShare> getActiveShareLink(Long fileId, String username) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (!file.getUploadedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access to file");
        }

        return fileShareRepository.findByFileAndIsActiveTrue(file);
    }

    public void revokeShareLink(Long fileId, String username) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (!file.getUploadedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access to file");
        }

        FileShare share = fileShareRepository.findByFileAndIsActiveTrue(file)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active share link found"));

        share.setActive(false);
        fileShareRepository.save(share);
    }

    public List<FileShare> getAllSharedFiles(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return fileShareRepository.findByCreatedByAndIsActiveTrue(user);
    }

    public ShareInfoResponse getShareInfo(String token) {
        FileShare share = validateToken(token);

        return ShareInfoResponse.builder()
                .fileName(share.getFile().getFileName())
                .sharedBy(share.getCreatedBy().getUsername())
                .expiresAt(share.getExpiresAt())
                .previewable(PREVIEWABLE_TYPES.contains(share.getFile().getFileType()))
                .fileSize(share.getFile().getFileSize())
                .build();
    }

    public FileDownloadResult previewSharedFile(String token) {
        FileShare share = validateToken(token);
        String fileType = share.getFile().getFileType();

        if (!PREVIEWABLE_TYPES.contains(fileType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This file type cannot be previewed");
        }

        byte[] bytes = s3Service.downloadFile(share.getFile().getS3Key());
        return new FileDownloadResult(share.getFile().getFileName(), fileType, bytes);
    }

    public FileDownloadResult downloadSharedFile(String token) {
        FileShare share = validateToken(token);

        byte[] bytes = s3Service.downloadFile(share.getFile().getS3Key());
        return new FileDownloadResult(share.getFile().getFileName(), share.getFile().getFileType(), bytes);
    }
}

