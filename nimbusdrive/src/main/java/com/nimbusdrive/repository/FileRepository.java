package com.nimbusdrive.repository;

import com.nimbusdrive.model.FileEntity;
import com.nimbusdrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Page<FileEntity> findByUploadedBy(User username , Pageable pageable);
}
