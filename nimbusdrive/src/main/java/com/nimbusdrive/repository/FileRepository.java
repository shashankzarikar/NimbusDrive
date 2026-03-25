package com.nimbusdrive.repository;

import com.nimbusdrive.model.FileEntity;
import com.nimbusdrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByUploadedBy(User username);
}
