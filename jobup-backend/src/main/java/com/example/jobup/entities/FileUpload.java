package com.example.jobup.entities;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Document(collection = "file_uploads")
@CompoundIndexes({
        @CompoundIndex(name = "owner_cat_active_idx", def = "{'ownerId': 1, 'category': 1, 'isActive': 1}"),
        @CompoundIndex(name = "uploader_time_idx", def = "{'uploadedBy': 1, 'uploadedAt': -1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUpload {
    @Id
    private String id;
    
    private String originalFileName;
    private String storedFileName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private FileType fileType;
    private FileCategory category;
    
    // Owner information
    private String uploadedBy;
    private String ownerId; // Can be userId, workerId, or jobPostId

    @CreatedDate
    private Instant uploadedAt;
    @LastModifiedDate
    private Instant updatedAt;

    @Builder.Default
    private boolean isActive = true;
    
    // Metadata for images
    private Integer width;
    private Integer height;

    private String sha256; // optional: set this after computing hash of the file content

    public enum FileType {
        IMAGE_JPEG("image/jpeg", "jpg"),
        IMAGE_PNG("image/png", "png"),
        IMAGE_GIF("image/gif", "gif"),
        DOCUMENT_PDF("application/pdf", "pdf"),
        DOCUMENT_DOC("application/msword", "doc"),
        DOCUMENT_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        
        private final String mimeType;
        private final String extension;
        
        FileType(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }
        
        public String getMimeType() {
            return mimeType;
        }
        
        public String getExtension() {
            return extension;
        }

        public static FileType from(String mimeType, String originalName) {
            String m = mimeType == null ? "" : mimeType.toLowerCase();
            if (m.startsWith("image/jpeg") || m.equals("image/jpg")) return IMAGE_JPEG;
            if (m.startsWith("image/png")) return IMAGE_PNG;
            if (m.startsWith("image/gif")) return IMAGE_GIF;
            if (m.startsWith("application/pdf")) return DOCUMENT_PDF;
            if (m.startsWith("application/msword")) return DOCUMENT_DOC;
            if (m.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                return DOCUMENT_DOCX;

            // Fallback by extension (covers browsers that omit/alter mime)
            String name = originalName == null ? "" : originalName.toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return IMAGE_JPEG;
            if (name.endsWith(".png")) return IMAGE_PNG;
            if (name.endsWith(".gif")) return IMAGE_GIF;
            if (name.endsWith(".pdf")) return DOCUMENT_PDF;
            if (name.endsWith(".doc")) return DOCUMENT_DOC;
            if (name.endsWith(".docx")) return DOCUMENT_DOCX;

            throw new IllegalArgumentException("Unsupported file type: " + mimeType + " / " + originalName);
        }


        public boolean isImage() {
            return this == IMAGE_JPEG || this == IMAGE_PNG || this == IMAGE_GIF;
        }
        
        public boolean isDocument() {
            return this == DOCUMENT_PDF || this == DOCUMENT_DOC || this == DOCUMENT_DOCX;
        }
    }
    
    public enum FileCategory {
        PROFILE_PICTURE,
        JOB_ATTACHMENT,
        WORKER_PORTFOLIO,
        WORKER_CERTIFICATE
    }
}
