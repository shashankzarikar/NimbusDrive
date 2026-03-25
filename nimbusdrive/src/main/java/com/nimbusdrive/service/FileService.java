package com.nimbusdrive.service;

import com.nimbusdrive.dto.FileDownloadResult;
import com.nimbusdrive.model.FileEntity;
import com.nimbusdrive.model.User;
import com.nimbusdrive.repository.FileRepository;
import com.nimbusdrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Service s3Service;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    public FileEntity uploadFile(MultipartFile file, String username) throws IOException {
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

        return fileRepository.save(fileEntity);
    }

    public List<FileEntity> getUserFiles(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return fileRepository.findByUploadedBy(user);
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
}

