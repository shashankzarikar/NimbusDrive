package com.nimbusdrive.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String fileName;

    @Column(nullable=false)
    private String s3Key;

    private String fileType;

    private Long fileSize;

    @Column(nullable=false)
    private String uploadedBy;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    private Boolean isPublic =false;



}
