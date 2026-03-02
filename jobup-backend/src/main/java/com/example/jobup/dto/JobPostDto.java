package com.example.jobup.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostDto {
    private String id;
    private String title;
    private String description;
    private String location;
    private Instant createdAt;
    private String createdById;
    private String createdByName;
    private List<String> likes;
    private List<String> savedBy;
    private List<CommentDto> comments;
    private List<String> attachmentFileIds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentDto {
        private String id;
        private String authorId;
        private String authorName;
        private String content;
        private Instant createdAt;
    }
}