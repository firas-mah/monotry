package com.example.jobup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "DTO used for user login")
public class LoginRequestDto {
    
    @NotBlank(message = "Username is required")
    @Schema(example = "johndoe")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Schema(example = "password123")
    private String password;
}
