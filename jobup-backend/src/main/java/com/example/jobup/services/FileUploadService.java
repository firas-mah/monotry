package com.example.jobup.services;

import com.example.jobup.dto.FileUploadResponseDto;
import com.example.jobup.entities.FileUpload;
import com.example.jobup.repositories.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.jobup.repositories.JobPostRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final FileUploadRepository fileUploadRepository;
    private final FileSecurityService fileSecurityService;
    private final JobPostRepository jobPostRepository;


    @Value("${file.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${file.upload.max-size.image:5242880}") // 5MB
    private long maxImageSize;
    
    @Value("${file.upload.max-size.document:10485760}") // 10MB
    private long maxDocumentSize;
    
    public FileUploadResponseDto uploadFile(MultipartFile file, FileUpload.FileCategory category,
                                          String ownerId, String uploadedBy) {
        try {
            // Enhanced security validation
            fileSecurityService.validateFileSecurity(file, category);
            validateFile(file, category);
            
            // Create secure filename
            String originalFileName = file.getOriginalFilename() == null ? "file" : Paths.get(file.getOriginalFilename()).getFileName().toString();
            String storedFileName = fileSecurityService.generateSecureFileName(originalFileName);
            
            // Create directory structure
            Path categoryPath = Paths.get(uploadDir, category.name().toLowerCase());
            Files.createDirectories(categoryPath);
            
            // Save file
            Path filePath = categoryPath.resolve(storedFileName);
            try (var in = file.getInputStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Get file type and metadata
            FileUpload.FileType fileType = FileUpload.FileType.from(file.getContentType(), originalFileName);
            Integer width = null;
            Integer height = null;
            
            if (fileType.isImage()) {
                try {
                    BufferedImage image = ImageIO.read(filePath.toFile());
                    if (image != null) {
                        width = image.getWidth();
                        height = image.getHeight();
                    }
                } catch (IOException e) {
                    log.warn("Could not read image dimensions for file: {}", originalFileName);
                }
            }
            
            // Save to database
            FileUpload fileUpload = FileUpload.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .filePath(filePath.toString())
                    .contentType(fileType.getMimeType())
                    .fileSize(file.getSize())
                    .fileType(fileType)
                    .category(category)
                    .uploadedBy(uploadedBy)
                    .ownerId(ownerId)
                    .width(width)
                    .height(height)
                    .build();

            try {
                FileUpload savedFile = fileUploadRepository.save(fileUpload);
                // --- link file to the post if it's a job attachment ---
                if (category == FileUpload.FileCategory.JOB_ATTACHMENT && ownerId != null) {
                    jobPostRepository.findById(ownerId).ifPresent(post -> {
                        if (post.getAttachmentFileIds() == null) {
                            post.setAttachmentFileIds(new java.util.ArrayList<>());
                        }
                        if (!post.getAttachmentFileIds().contains(savedFile.getId())) {
                            post.getAttachmentFileIds().add(savedFile.getId());
                            jobPostRepository.save(post);
                        }
                    });
                }
                log.info("File uploaded successfully: {} by user: {}", originalFileName, uploadedBy);
                return mapToResponseDto(savedFile);
            } catch (Exception dbEx) {
                // rollback the physical file if DB save fails
                try { Files.deleteIfExists(filePath); } catch (IOException ignore) {}
                throw dbEx;
            }


        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    // Add near other public methods in FileUploadService
    public Optional<FileUpload> getFileEntityById(String fileId) {
        return fileUploadRepository.findByIdAndIsActiveTrue(fileId);
    }

    public List<FileUploadResponseDto> uploadMultipleFiles(MultipartFile[] files, FileUpload.FileCategory category,
                                                         String ownerId, String uploadedBy) {
        List<FileUploadResponseDto> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                FileUploadResponseDto response = uploadFile(file, category, ownerId, uploadedBy);
                responses.add(response);
            } catch (Exception e) {
                log.error("Error uploading file {}: {}", file.getOriginalFilename(), e.getMessage());
                // Continue with other files, don't fail the entire batch
            }
        }

        return responses;
    }

    public List<FileUploadResponseDto> getFilesByOwnerAndCategory(String ownerId, FileUpload.FileCategory category) {
        return fileUploadRepository.findByOwnerIdAndCategoryAndIsActiveTrue(ownerId, category)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    public List<FileUploadResponseDto> getFilesByOwner(String ownerId) {
        return fileUploadRepository.findByOwnerIdAndIsActiveTrue(ownerId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    public Optional<FileUploadResponseDto> getFileById(String fileId) {
        return fileUploadRepository.findByIdAndIsActiveTrue(fileId)
                .map(this::mapToResponseDto);
    }
    
    public Resource loadFileAsResource(String fileId) {
        try {
            Optional<FileUpload> fileUpload = fileUploadRepository.findByIdAndIsActiveTrue(fileId);
            if (fileUpload.isEmpty()) {
                throw new RuntimeException("File not found: " + fileId);
            }
            
            Path filePath = Paths.get(fileUpload.get().getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + fileId);
            }
        } catch (Exception e) {
            log.error("Error loading file: {}", e.getMessage());
            throw new RuntimeException("Could not load file: " + fileId);
        }
    }
    
    public boolean deleteFile(String fileId, String userId) {
        try {
            Optional<FileUpload> fileUpload = fileUploadRepository.findByIdAndIsActiveTrue(fileId);
            if (fileUpload.isEmpty()) {
                return false;
            }
            
            FileUpload file = fileUpload.get();

            // Check if user has permission to delete
            boolean isUploader = userId.equals(file.getUploadedBy());

// In some categories (PROFILE_PICTURE, WORKER_*), ownerId is a user/worker ID
            boolean isOwnerIdUser = userId.equals(file.getOwnerId());

// For JOB_ATTACHMENT, ownerId is the jobPostId; allow the post creator
            boolean isPostOwner = file.getCategory() == FileUpload.FileCategory.JOB_ATTACHMENT
                    && file.getOwnerId() != null
                    && jobPostRepository.findById(file.getOwnerId())
                    .map(p -> userId.equals(p.getCreatedById()))
                    .orElse(false);

            if (!(isUploader || isOwnerIdUser || isPostOwner)) {
                throw new RuntimeException("Unauthorized to delete this file");
            }


            // Mark as inactive instead of deleting
            file.setActive(false);
            fileUploadRepository.save(file);
            
            // Optionally delete physical file
            try {
                Files.deleteIfExists(Paths.get(file.getFilePath()));
            } catch (IOException e) {
                log.warn("Could not delete physical file: {}", file.getFilePath());
            }
            
            log.info("File deleted: {} by user: {}", file.getOriginalFileName(), userId);
            // --- unlink from post if it was a job attachment ---
            if (file.getCategory() == FileUpload.FileCategory.JOB_ATTACHMENT && file.getOwnerId() != null) {
                jobPostRepository.findById(file.getOwnerId()).ifPresent(post -> {
                    if (post.getAttachmentFileIds() != null && post.getAttachmentFileIds().remove(fileId)) {
                        jobPostRepository.save(post);
                    }
                });
            }
            return true;
            
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }
    
    private void validateFile(MultipartFile file, FileUpload.FileCategory category) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new RuntimeException("File content type is unknown");
        }
        
        try {
            FileUpload.FileType fileType = FileUpload.FileType.from(contentType, file.getOriginalFilename());
            
            // Validate file size based on type
            long maxSize = fileType.isImage() ? maxImageSize : maxDocumentSize;
            if (file.getSize() > maxSize) {
                throw new RuntimeException("File size exceeds maximum allowed size: " + maxSize + " bytes");
            }
            
            // Validate file type for category
            validateFileTypeForCategory(fileType, category);
            
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unsupported file type: " + contentType);
        }
    }
    
    private void validateFileTypeForCategory(FileUpload.FileType fileType, FileUpload.FileCategory category) {
        switch (category) {
            case PROFILE_PICTURE:
                if (!fileType.isImage()) {
                    throw new RuntimeException("Profile pictures must be image files");
                }
                break;
            case JOB_ATTACHMENT:
                // Both images and documents allowed
                break;
            case WORKER_PORTFOLIO:
            case WORKER_CERTIFICATE:
                // Both images and documents allowed
                break;
        }
    }
    

    
    private FileUploadResponseDto mapToResponseDto(FileUpload fileUpload) {
        return FileUploadResponseDto.builder()
                .id(fileUpload.getId())
                .originalFileName(fileUpload.getOriginalFileName())
                .downloadUrl("/api/files/" + fileUpload.getId() + "/download")
                .contentType(fileUpload.getContentType())
                .fileSize(fileUpload.getFileSize())
                .fileType(fileUpload.getFileType())
                .category(fileUpload.getCategory())
                .uploadedAt(fileUpload.getUploadedAt())
                .width(fileUpload.getWidth())
                .height(fileUpload.getHeight())
                .ownerId(fileUpload.getOwnerId())
                .build();
    }
}
