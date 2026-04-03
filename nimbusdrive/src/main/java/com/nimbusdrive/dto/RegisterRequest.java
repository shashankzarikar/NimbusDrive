package com.nimbusdrive.dto;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RegisterRequest {

     @NotBlank(message="Username cannot be blank")
     private String username;

     @NotBlank(message = "Email cannot be blank")
     @Email(message = "Invalid email format")
     private String email;

     @NotBlank(message = "Password cannot be blank")
     @Size(min = 6, message = "Password must be at least 6 characters")
     private String password;
}
