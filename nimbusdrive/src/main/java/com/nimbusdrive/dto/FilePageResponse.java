package com.nimbusdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePageResponse {
    private List<FileResponse> files;
    private int currentPage;
    private int totalPages;
    private long totalFiles;
}