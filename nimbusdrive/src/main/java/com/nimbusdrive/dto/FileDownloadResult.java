package com.nimbusdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDownloadResult {
    private String fileName;
    private String contentType;
    private byte[] bytes;
}

