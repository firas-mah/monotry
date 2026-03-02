package com.example.jobup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "DTO used to create a new worker")
public class WorkerCreateDto {
    // ❌ Supprimer fullName - on le récupère depuis User
    
    @Schema(example = "Electrician")
    private String jobType;

    @Schema(example = "+21612345678")
    private String phoneNumber;

    @Schema(example = "Ariana")
    private String location;

    @Schema(example = "etc...")
    private String description;

    @Schema(description = "User ID who becomes a worker")
    private String userId;
}


