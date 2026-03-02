package com.example.jobup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "DTO returned in responses to the frontend")
public class WorkerResponseDto {

    private String id;
    private String fullName;
    private String jobType;
    private String phoneNumber;
    private String location;
    private double rating;
    private String description;

}
