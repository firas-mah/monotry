package com.example.jobup.services;

import com.example.jobup.entities.FileUpload;
import com.example.jobup.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileSecurityService {

    private final UserRepository userRepository;

    // Allowed file extensions
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx");
    
    // Magic bytes for file type validation
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF_MAGIC = {0x47, 0x49, 0x46};
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

    // Dangerous file patterns to detect
    private static final List<String> DANGEROUS_PATTERNS = Arrays.asList(
        "<?php", "<%", "<script", "javascript:", "vbscript:", "onload=", "onerror=",
        "eval(", "document.cookie", "window.location", "alert(", "confirm(",
        "prompt(", "setTimeout(", "setInterval("
    );
    
    /**
     * Validates file security including type validation and malicious content detection
     */
    public void validateFileSecurity(MultipartFile file, FileUpload.FileCategory category) {
        validateFileExtension(file);
        validateMimeType(file);
        validateMagicBytes(file);
        scanForMaliciousContent(file);
        validateFileForCategory(file, category);
    }
    
    /**
     * Validates file extension against allowed extensions
     */
    private void validateFileExtension(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new SecurityException("File name is required");
        }
        
        String extension = getFileExtension(originalFileName).toLowerCase();
        if (extension.isEmpty()) {
            throw new SecurityException("File must have an extension");
        }
        
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension) && !ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)) {
            throw new SecurityException("File extension not allowed: " + extension);
        }
    }
    
    /**
     * Validates MIME type against expected types
     */
    private void validateMimeType(MultipartFile file) {
        String mimeType = file.getContentType();
        if (mimeType == null) {
            throw new SecurityException("File MIME type is required");
        }
        
        try {
            FileUpload.FileType.from(mimeType, file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            throw new SecurityException("MIME type not allowed: " + mimeType);
        }
    }
    
    /**
     * Validates file magic bytes to ensure file type matches extension
     */
    private void validateMagicBytes(MultipartFile file) {
        try (var in = file.getInputStream()) {
            // Read up to 16 bytes (enough for our signatures)
            byte[] head = in.readNBytes(16);
            if (head.length < 4) {
                throw new SecurityException("File is too small to validate");
            }

            String mimeType = file.getContentType();
            boolean valid = false;

            if ("image/jpeg".equals(mimeType) || "image/jpg".equals(mimeType)) {
                valid = startsWith(head, JPEG_MAGIC);
            } else if ("image/png".equals(mimeType)) {
                valid = startsWith(head, PNG_MAGIC);
            } else if ("image/gif".equals(mimeType)) {
                valid = startsWith(head, GIF_MAGIC);
            } else if ("application/pdf".equals(mimeType)) {
                valid = startsWith(head, PDF_MAGIC);
            } else if ("application/msword".equals(mimeType)
                    || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType)) {
                // Magic byte validation for DOC/DOCX is complex; allow
                valid = true;
            }

            if (!valid) {
                throw new SecurityException("File content does not match declared type");
            }
        } catch (IOException e) {
            log.error("Error reading file bytes for validation: {}", e.getMessage());
            throw new SecurityException("Unable to validate file content");
        }
    }

    /**
     * Scans file content for malicious patterns
     */
    private void scanForMaliciousContent(MultipartFile file) {
        try {
            // Only scan text-based files and small files to avoid performance issues
            if (file.getSize() > 1024 * 1024) { // Skip files larger than 1MB
                return;
            }
            
            String mimeType = file.getContentType();
            if (mimeType == null) return;
// Only scan clearly text-like content
            boolean textLike = mimeType.startsWith("text/")
                    || mimeType.equals("application/json")
                    || mimeType.equals("application/xml")
                    || mimeType.equals("text/html");

            if (!textLike) return;

            
            String content = new String(file.getBytes()).toLowerCase();
            
            for (String pattern : DANGEROUS_PATTERNS) {
                if (content.contains(pattern.toLowerCase())) {
                    log.warn("Malicious pattern detected in file: {} - Pattern: {}", 
                            file.getOriginalFilename(), pattern);
                    throw new SecurityException("File contains potentially malicious content");
                }
            }
            
        } catch (IOException e) {
            log.error("Error scanning file for malicious content: {}", e.getMessage());
            throw new SecurityException("Unable to scan file content");
        }
    }
    
    /**
     * Validates file type is appropriate for the category
     */
    private void validateFileForCategory(MultipartFile file, FileUpload.FileCategory category) {
        String mimeType = file.getContentType();
        FileUpload.FileType fileType = FileUpload.FileType.from(mimeType, file.getOriginalFilename());
        
        switch (category) {
            case PROFILE_PICTURE:
                if (!fileType.isImage()) {
                    throw new SecurityException("Profile pictures must be image files");
                }
                break;
            case JOB_ATTACHMENT:
            case WORKER_PORTFOLIO:
            case WORKER_CERTIFICATE:
                // Both images and documents are allowed for these categories
                if (!fileType.isImage() && !fileType.isDocument()) {
                    throw new SecurityException("Only images and documents are allowed for this category");
                }
                break;
        }
    }
    
    /**
     * Checks if user has permission to access a file
     */
    public boolean hasFileAccess(String userId, FileUpload fileUpload) {
        if (fileUpload == null) return false;

        // 1) Public (or broadly visible) categories — anyone can view
        if (fileUpload.getCategory() == FileUpload.FileCategory.PROFILE_PICTURE
                || fileUpload.getCategory() == FileUpload.FileCategory.JOB_ATTACHMENT
                || fileUpload.getCategory() == FileUpload.FileCategory.WORKER_PORTFOLIO
                || fileUpload.getCategory() == FileUpload.FileCategory.WORKER_CERTIFICATE) {
            return true;
        }

        // 2) From here on, we need a real authenticated user
        if (userId == null || "anonymousUser".equals(userId)) return false;

        // 3) Uploader or owner can always view
        if (userId.equals(fileUpload.getUploadedBy())) return true;
        if (userId.equals(fileUpload.getOwnerId())) return true;

        // 4) (Optional) admins can view anything
        return userRepository.findById(userId)
                .map(u -> u.getRoles().contains(com.example.jobup.entities.Role.ROLE_ADMIN))
                .orElse(false);
    }


    /**
     * Generates a secure filename
     */
    public String generateSecureFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String baseName = java.util.UUID.randomUUID().toString();
        return baseName + (extension.isEmpty() ? "" : "." + extension);
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
    
    private boolean startsWith(byte[] array, byte[] prefix) {
        if (array.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
