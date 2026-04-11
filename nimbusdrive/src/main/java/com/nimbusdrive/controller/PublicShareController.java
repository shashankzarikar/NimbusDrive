package com.nimbusdrive.controller;

import com.nimbusdrive.dto.FileDownloadResult;
import com.nimbusdrive.dto.ShareInfoResponse;
import com.nimbusdrive.service.FileShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicShareController {

    private final FileShareService fileShareService;

    @GetMapping("/api/share/{token}/info")
    public ResponseEntity<ShareInfoResponse> getShareInfo(@PathVariable("token") String token) {
        ShareInfoResponse shareInfoResponse = fileShareService.getShareInfo(token);
        return ResponseEntity.ok(shareInfoResponse);
    }

    @GetMapping("/api/share/{token}/preview")
    public ResponseEntity<byte[]> previewSharedFile(@PathVariable("token") String token) {
        FileDownloadResult result = fileShareService.previewSharedFile(token);
        return ResponseEntity.ok()
                .header("Content-Type", result.getContentType())
                .header("Content-Disposition", "inline; filename=\"" + result.getFileName() + "\"")
                .body(result.getBytes());
    }

    @GetMapping("/api/share/{token}/download")
    public ResponseEntity<byte[]> downloadSharedFile(@PathVariable("token") String token) {
        FileDownloadResult result = fileShareService.downloadSharedFile(token);
        return ResponseEntity.ok()
                .header("Content-Type", result.getContentType())
                .header("Content-Disposition", "attachment; filename=\"" + result.getFileName() + "\"")
                .body(result.getBytes());
    }
}

