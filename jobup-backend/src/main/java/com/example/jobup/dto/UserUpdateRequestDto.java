package com.example.jobup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "DTO used to update user information")
public class UserUpdateRequestDto {
    
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Schema(example = "johndoe")
    private String username;
    
    @Email(message = "Email should be valid")
    @Schema(example = "john.doe@example.com")
    private String email;
    
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(example = "newpassword123")
    private String newPassword;
    
    @Schema(example = "currentpassword123")
    private String currentPassword;
} 