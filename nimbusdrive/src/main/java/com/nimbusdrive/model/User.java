package com.nimbusdrive.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable=false)
    private String username;

    @Column(unique = true , nullable =false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING) //This tells MySQL to store role as "USER" or "ADMIN" text , not as a number
    private Role role = Role.USER;

    private Long storageLimit = 1073741824L; // 1 GB in bytes
    private Long storageUsed = 0L;
    private Boolean isActive = true;
    private LocalDateTime createdAt =LocalDateTime.now();

    public enum Role {
        USER,ADMIN
    }

}
