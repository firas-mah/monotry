package com.example.jobup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "DTO returned after successful authentication")
public class AuthResponseDto {
    
    @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(example = "[\"ROLE_CLIENT\"]")
    private List<String> roles;

    private String userId;

    @Schema(example = "johndoe")
    private String username;

    @Schema(example = "john.doe@example.com")
    private String email;
}
