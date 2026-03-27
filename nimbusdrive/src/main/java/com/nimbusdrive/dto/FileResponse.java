package com.nimbusdrive.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileResponse {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private Boolean isPublic;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
}

