package com.example.jobup.services;

import com.example.jobup.dto.JobPostDto;
import com.example.jobup.entities.JobPost;
import com.example.jobup.entities.Notification;
import com.example.jobup.entities.NotificationType;
import com.example.jobup.repositories.JobPostRepository;
import com.example.jobup.repositories.UserRepository;
import com.example.jobup.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobPostService {
    private final JobPostRepository jobPostRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public JobPostDto createPost(JobPostDto dto) {
        JobPost post = JobPost.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .location(dto.getLocation())
                .createdAt(Instant.now())
                .createdById(dto.getCreatedById())
                .createdByName(dto.getCreatedByName())
                .attachmentFileIds(dto.getAttachmentFileIds() != null ? dto.getAttachmentFileIds() : new ArrayList<>())
                .build();
        return toDto(jobPostRepository.save(post));
    }

    public List<JobPostDto> getAllPosts() {
        return jobPostRepository
                .findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }


    public JobPostDto likePost(String postId, String workerId) {
        JobPost post = jobPostRepository.findById(postId).orElseThrow();
        boolean wasLiked = post.getLikes().contains(workerId);

        if (wasLiked) {
            post.getLikes().remove(workerId);
        } else {
            post.getLikes().add(workerId);

            // Create notification for post owner (only if it's not the owner liking their own post)
            if (!workerId.equals(post.getCreatedById())) {
                try {
                    User worker = userRepository.findById(workerId).orElse(null);
                    String workerName = worker != null ? worker.getUsername() : "Someone";

                    notificationService.createNotification(
                        post.getCreatedById(),
                        post.getCreatedByName(),
                        workerId,
                        workerName,
                        postId,
                        post.getTitle(),
                        NotificationType.POST_LIKED,
                        null
                    );
                } catch (Exception e) {
                    System.err.println("Failed to create like notification: " + e.getMessage());
                }
            }
        }

        return toDto(jobPostRepository.save(post));
    }

    public JobPostDto savePost(String postId, String workerId) {
        JobPost post = jobPostRepository.findById(postId).orElseThrow();
        if (post.getSavedBy().contains(workerId)) {
            post.getSavedBy().remove(workerId);
        } else {
            post.getSavedBy().add(workerId);
        }
        return toDto(jobPostRepository.save(post));
    }

    public JobPostDto addComment(String postId, JobPostDto.CommentDto commentDto) {
        JobPost post = jobPostRepository.findById(postId).orElseThrow();
        JobPost.Comment comment = JobPost.Comment.builder()
                .id(UUID.randomUUID().toString())
                .authorId(commentDto.getAuthorId())
                .authorName(commentDto.getAuthorName())
                .content(commentDto.getContent())
                .createdAt(Instant.now())
                .build();
        post.getComments().add(comment);

        // Create notification for post owner (only if it's not the owner commenting on their own post)
        if (!commentDto.getAuthorId().equals(post.getCreatedById())) {
            try {
                User commenter = userRepository.findById(commentDto.getAuthorId()).orElse(null);
                String commenterName = commenter != null ? commenter.getUsername() : "Someone";
                
                notificationService.createNotification(
                    post.getCreatedById(),
                    post.getCreatedByName(),
                    commentDto.getAuthorId(),
                    commenterName,  // ✅ Now fetched from database like likes
                    postId,
                    post.getTitle(),
                    NotificationType.POST_COMMENTED,
                    null
                );
            } catch (Exception e) {
                System.err.println("Failed to create comment notification: " + e.getMessage());
            }
        }

        return toDto(jobPostRepository.save(post));
    }

    public List<JobPostDto> getSavedPostsByUserId(String userId) {
        return jobPostRepository
                .findBySavedByContains(userId, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }


    public List<JobPostDto> getPostsByCreatorId(String userId) {
        return jobPostRepository
                .findByCreatedByIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }


    private JobPostDto toDto(JobPost post) {
        return JobPostDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .description(post.getDescription())
                .location(post.getLocation())
                .createdAt(post.getCreatedAt())
                .createdById(post.getCreatedById())
                .createdByName(post.getCreatedByName())
                .likes(post.getLikes())
                .savedBy(post.getSavedBy())
                .attachmentFileIds(post.getAttachmentFileIds())
                .comments(post.getComments() != null ? post.getComments().stream().map(c ->
                        JobPostDto.CommentDto.builder()
                                .id(c.getId())
                                .authorId(c.getAuthorId())
                                .authorName(c.getAuthorName())
                                .content(c.getContent())
                                .createdAt(c.getCreatedAt())
                                .build()
                ).collect(Collectors.toList()) : new java.util.ArrayList<>())
                .build();
    }
}
