package com.example.jobup.controller;

import com.example.jobup.dto.FileUploadResponseDto;
import com.example.jobup.dto.JobPostDto;
import com.example.jobup.services.JobPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.jobup.services.FileUploadService;
import com.example.jobup.entities.FileUpload;


import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobPostController {
    private final JobPostService jobPostService;
    private final FileUploadService fileUploadService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JobPostDto createPost(@RequestBody JobPostDto dto, java.security.Principal principal) {
        // trust server identity
        dto.setCreatedById(principal.getName());
        return jobPostService.createPost(dto);
    }

    @PostMapping(
            path = "/multipart",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JobPostDto createPostMultipart(@RequestPart("post") JobPostDto dto,
                                          @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                          java.security.Principal principal) {
        // 1) Create the post first
// make the server authoritative for the creator
        dto.setCreatedById(principal.getName());
        JobPostDto created = jobPostService.createPost(dto);

        // 2) If files were sent, upload them as JOB_ATTACHMENT owned by the post
        if (files != null && !files.isEmpty()) {
            List<String> fileIds = files.stream()
                    .map(f -> fileUploadService.uploadFile(
                            f,
                            FileUpload.FileCategory.JOB_ATTACHMENT,
                            created.getId(),        // ownerId = postId
                            principal.getName()
                    ))
                    .map(FileUploadResponseDto::getId)
                    .toList();

            // 3) Persist the link in the post (if you added linkAttachments in service)
            // jobPostService.linkAttachments(created.getId(), fileIds);

            // If you didn't add linkAttachments, make sure your UI reads attachments by ownerId via the files API.
        }
        return created;
    }

    @GetMapping(value = "/{postId}/attachments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileUploadResponseDto> getPostAttachments(@PathVariable String postId) {
        return fileUploadService.getFilesByOwnerAndCategory(postId, FileUpload.FileCategory.JOB_ATTACHMENT);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JobPostDto> getAllPosts() {
        return jobPostService.getAllPosts();
    }

    @PostMapping("/{postId}/like")
    public JobPostDto likePost(@PathVariable String postId, java.security.Principal principal) {
        return jobPostService.likePost(postId, principal.getName());
    }

    @PostMapping("/{postId}/save")
    public JobPostDto savePost(@PathVariable String postId, java.security.Principal principal) {
        return jobPostService.savePost(postId, principal.getName());
    }

    @PostMapping("/{postId}/comments")
    public JobPostDto addComment(@PathVariable String postId,
                                 @RequestBody JobPostDto.CommentDto commentDto,
                                 java.security.Principal principal) {
        // prevent spoofing the author
        commentDto.setAuthorId(principal.getName());
        return jobPostService.addComment(postId, commentDto);
    }

    @GetMapping(value = "/saved/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JobPostDto> getSavedPostsByUserId(@PathVariable String userId) {
        return jobPostService.getSavedPostsByUserId(userId);
    }

    @GetMapping(value = "/created-by/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JobPostDto> getPostsByCreatorId(@PathVariable String userId) {
        return jobPostService.getPostsByCreatorId(userId);
    }
}