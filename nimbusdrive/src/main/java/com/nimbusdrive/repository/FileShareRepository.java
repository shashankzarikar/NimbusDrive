package com.nimbusdrive.repository;

import com.nimbusdrive.model.FileEntity;
import com.nimbusdrive.model.FileShare;
import com.nimbusdrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileShareRepository extends JpaRepository<FileShare, Long> {

    Optional<FileShare> findByFileAndIsActiveTrue(FileEntity file);

    Optional<FileShare> findByShareToken(String shareToken);

    List<FileShare> findByCreatedByAndIsActiveTrue(User createdBy);
}

