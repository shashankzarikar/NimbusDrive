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
public class ShareInfoResponse {

    private String fileName;
    private String sharedBy;
    private LocalDateTime expiresAt;
    private boolean previewable;
    private Long fileSize;
}

