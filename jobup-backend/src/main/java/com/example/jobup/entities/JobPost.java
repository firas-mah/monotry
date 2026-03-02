package com.example.jobup.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "job_posts")
@CompoundIndexes({
        @CompoundIndex(name = "createdBy_createdAt_idx", def = "{'createdById': 1, 'createdAt': -1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPost {
    @Id
    private String id;
    private String title;
    private String description;
    private String location;
    private Instant createdAt;
    private String createdById;
    private String createdByName;
    @Builder.Default
    private List<String> likes = new ArrayList<>();
    @Builder.Default
    private List<String> savedBy = new ArrayList<>();
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    // File attachments for job posts
    @Builder.Default
    private List<String> attachmentFileIds = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comment {
        private String id;
        private String authorId;
        private String authorName;
        private String content;
        private Instant createdAt;
    }
}