package com.nimbusdrive.service;

import com.nimbusdrive.dto.FileDownloadResult;
import com.nimbusdrive.dto.FileResponse;
import com.nimbusdrive.model.FileEntity;
import com.nimbusdrive.model.User;
import com.nimbusdrive.repository.FileRepository;
import com.nimbusdrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.nimbusdrive.dto.FilePageResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Service s3Service;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    public FileResponse uploadFile(MultipartFile file, String username) throws IOException {
        Set<String> allowedTypes = Set.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain",
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/webp",
                "application/zip",
                "application/x-zip-compressed"
        );

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File type not allowed");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 10MB limit");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String s3Key = s3Service.uploadFile(username, file);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity.setFileSize(file.getSize());
        fileEntity.setFileType(file.getContentType());
        fileEntity.setS3Key(s3Key);
        fileEntity.setUploadedBy(user);
        fileEntity.setUploadedAt(LocalDateTime.now());

        return toFileResponse(fileRepository.save(fileEntity));
    }

    public FilePageResponse getUserFiles(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        Page<FileEntity> filePage = fileRepository.findByUploadedBy(user, pageable);

        List<FileResponse> files = filePage.getContent()
                .stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList());

        return FilePageResponse.builder()
                .files(files)
                .currentPage(filePage.getNumber())
                .totalPages(filePage.getTotalPages())
                .totalFiles(filePage.getTotalElements())
                .build();
    }

    public FileEntity getFileEntity(Long fileId, String username) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (!fileEntity.getUploadedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access to file");
        }

        return fileEntity;
    }

    public FileDownloadResult downloadFile(Long fileId, String username) {
        FileEntity fileEntity = getFileEntity(fileId, username);
        byte[] bytes = s3Service.downloadFile(fileEntity.getS3Key());

        return new FileDownloadResult(
                fileEntity.getFileName(),
                fileEntity.getFileType(),
                bytes
        );
    }

    public void deleteFile(Long fileId, String username) {
        FileEntity fileEntity = getFileEntity(fileId, username);

        s3Service.deleteFile(fileEntity.getS3Key());
        fileRepository.delete(fileEntity);
    }

    private FileResponse toFileResponse(FileEntity fileEntity) {
        return FileResponse.builder()
                .id(fileEntity.getId())
                .fileName(fileEntity.getFileName())
                .fileSize(fileEntity.getFileSize())
                .fileType(fileEntity.getFileType())
                .isPublic(fileEntity.getIsPublic())
                .uploadedAt(fileEntity.getUploadedAt())
                .uploadedBy(fileEntity.getUploadedBy().getUsername())
                .build();
    }
}

