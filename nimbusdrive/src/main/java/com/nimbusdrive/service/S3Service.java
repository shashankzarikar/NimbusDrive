package com.nimbusdrive.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.bucket}")
    private String bucketName;

    public String uploadFile(String username,
                             MultipartFile file) throws IOException {

        String s3Key = username + "/" +
                UUID.randomUUID() + "_" +
                file.getOriginalFilename();

        PutObjectRequest putRequest =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .build();

        s3Client.putObject(putRequest,
                RequestBody.fromBytes(file.getBytes()));

        return s3Key;
    }

    public byte[] downloadFile(String s3Key) {
        GetObjectRequest getRequest =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();

        return s3Client.getObjectAsBytes(getRequest)
                .asByteArray();
    }

    public void deleteFile(String s3Key) {
        DeleteObjectRequest deleteRequest =
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();

        s3Client.deleteObject(deleteRequest);
    }
}