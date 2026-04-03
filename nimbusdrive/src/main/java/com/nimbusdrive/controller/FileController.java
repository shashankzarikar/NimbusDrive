package com.nimbusdrive.controller;

import com.nimbusdrive.dto.FileDownloadResult;
import com.nimbusdrive.dto.FilePageResponse;
import com.nimbusdrive.dto.FileResponse;
import com.nimbusdrive.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String username = getLoggedInUsername();
        FileResponse savedFile = fileService.uploadFile(file, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFile);
    }

    @GetMapping("")
    public ResponseEntity<FilePageResponse> getUserFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        String username = getLoggedInUsername();
        return ResponseEntity.ok(fileService.getUserFiles(username, page, size));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        String username = getLoggedInUsername();
        FileDownloadResult result = fileService.downloadFile(id, username);
        MediaType mediaType = (result.getContentType() != null && !result.getContentType().isBlank())
                ? MediaType.parseMediaType(result.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", result.getFileName());

        return ResponseEntity.ok()
                .headers(headers)
                .body(result.getBytes());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        String username = getLoggedInUsername();
        fileService.deleteFile(id, username);
        return ResponseEntity.noContent().build();
    }

    private String getLoggedInUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

