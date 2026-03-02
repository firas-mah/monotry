package com.example.jobup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "DTO used to update an existing worker")
public class WorkerUpdateDto {

    @Schema(hidden = true)
    private String id;

    private String fullName;
    private String jobType;
    private String location;
    private String phoneNumber;
    private String description;


    // ❌ pas de rating modifiable ici
}
