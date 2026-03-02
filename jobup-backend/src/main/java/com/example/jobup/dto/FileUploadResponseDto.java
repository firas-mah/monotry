package com.example.jobup.dto;

import com.example.jobup.entities.FileUpload;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "File upload response DTO")
public class FileUploadResponseDto {
    
    @Schema(description = "File ID")
    private String id;
    
    @Schema(description = "Original file name")
    private String originalFileName;
    
    @Schema(description = "File download URL")
    private String downloadUrl;
    
    @Schema(description = "File content type")
    private String contentType;
    
    @Schema(description = "File size in bytes")
    private Long fileSize;
    
    @Schema(description = "File type")
    private FileUpload.FileType fileType;
    
    @Schema(description = "File category")
    private FileUpload.FileCategory category;

    @Schema(description = "Upload timestamp")
    private Instant uploadedAt;
    @Schema(description = "updated timestamp")
    private Instant updatedAt;
    
    @Schema(description = "Image width (for images only)")
    private Integer width;
    
    @Schema(description = "Image height (for images only)")
    private Integer height;
    
    @Schema(description = "Owner ID")
    private String ownerId;
}
