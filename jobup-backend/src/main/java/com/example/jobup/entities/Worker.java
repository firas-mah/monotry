package com.example.jobup.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "worker")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Worker {
    @Id
    private String id;

    private String fullName;
    private String jobType;
    private String phoneNumber;
    private String location;
    private double rating;         // average
    private long ratingsCount;     // to recompute average efficiently
    private String description;

    @Builder.Default
    private List<String> portfolioFileIds = new ArrayList<>();

    @Builder.Default
    private List<String> certificateFileIds = new ArrayList<>();
}
