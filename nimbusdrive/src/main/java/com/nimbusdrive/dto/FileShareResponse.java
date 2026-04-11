package com.nimbusdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShareResponse {

    private Long fileId;
    private String fileName;
    private String shareToken;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}

