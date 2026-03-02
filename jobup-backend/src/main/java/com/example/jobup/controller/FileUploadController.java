package com.example.jobup.controller;

import com.example.jobup.dto.FileUploadResponseDto;
import com.example.jobup.entities.FileUpload;
import com.example.jobup.repositories.UserRepository;
import com.example.jobup.services.FileUploadService;
import com.example.jobup.services.FileSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "File Upload", description = "File upload and management APIs")
@Slf4j
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final FileSecurityService fileSecurityService;
    private final UserRepository userRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upload a file", description = "Upload a file with specified category and owner")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                content = @Content(schema = @Schema(implementation = FileUploadResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "413", description = "File too large")
    })
    public ResponseEntity<FileUploadResponseDto> uploadFile(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "File category (PROFILE_PICTURE, JOB_ATTACHMENT, WORKER_PORTFOLIO, WORKER_CERTIFICATE)")
            @RequestParam("category") FileUpload.FileCategory category,
            @Parameter(description = "Owner ID (user ID, worker ID, or job post ID)") @RequestParam("ownerId") String ownerId) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String uploadedBy = authentication.getName();

            FileUploadResponseDto response = fileUploadService.uploadFile(file, category, ownerId, uploadedBy);
            // If it's a profile picture, persist it on the user
            if (category == FileUpload.FileCategory.PROFILE_PICTURE) {
                // Only the owner can set their own profile picture (relax if you want admins as well)
                if (!uploadedBy.equals(ownerId)) {
                    return ResponseEntity.status(403).build();
                }
                userRepository.findById(ownerId).ifPresent(u -> {
                    u.setProfilePictureId(response.getId());
                    userRepository.save(u);
                });
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upload multiple files", description = "Upload multiple files with specified category and owner")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid files or parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "413", description = "Files too large")
    })
    public ResponseEntity<List<FileUploadResponseDto>> uploadMultipleFiles(
            @Parameter(description = "Files to upload") @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "File category") @RequestParam("category") FileUpload.FileCategory category,
            @Parameter(description = "Owner ID") @RequestParam("ownerId") String ownerId) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String uploadedBy = authentication.getName();

            List<FileUploadResponseDto> responses = fileUploadService.uploadMultipleFiles(files, category, ownerId, uploadedBy);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("Error uploading multiple files: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get files by owner", description = "Get all files for a specific owner")
    public ResponseEntity<List<FileUploadResponseDto>> getFilesByOwner(
            @Parameter(description = "Owner ID") @PathVariable String ownerId) {
        
        try {
            List<FileUploadResponseDto> files = fileUploadService.getFilesByOwner(ownerId);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("Error getting files by owner: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/owner/{ownerId}/category/{category}")
    @Operation(summary = "Get files by owner and category", description = "Get files for a specific owner and category")
    public ResponseEntity<List<FileUploadResponseDto>> getFilesByOwnerAndCategory(
            @Parameter(description = "Owner ID") @PathVariable String ownerId,
            @Parameter(description = "File category") @PathVariable FileUpload.FileCategory category) {
        
        try {
            List<FileUploadResponseDto> files = fileUploadService.getFilesByOwnerAndCategory(ownerId, category);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("Error getting files by owner and category: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{fileId}")
    @Operation(summary = "Get file info", description = "Get file information by ID")
    public ResponseEntity<FileUploadResponseDto> getFileInfo(
            @Parameter(description = "File ID") @PathVariable String fileId) {
        
        try {
            Optional<FileUploadResponseDto> file = fileUploadService.getFileById(fileId);
            return file.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting file info: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{fileId}/download")
    @Operation(summary = "Download file", description = "Download file by ID")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getName()))
                    ? auth.getName() : null;

            var fileEntityOpt = fileUploadService.getFileEntityById(fileId);
            if (fileEntityOpt.isEmpty()) return ResponseEntity.notFound().build();
            var fileEntity = fileEntityOpt.get();

            if (!fileSecurityService.hasFileAccess(userId, fileEntity)) {
                return ResponseEntity.status(403).build();
            }

            Resource resource = fileUploadService.loadFileAsResource(fileId);
            var file = fileUploadService.getFileById(fileId).orElseThrow();

            String filename = file.getOriginalFileName();
            String contentDisposition = "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" +
                    java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{fileId}/view")
    @Operation(summary = "View file", description = "View file inline by ID")
    public ResponseEntity<Resource> viewFile(@PathVariable String fileId) {
        try {
            var fileEntity = fileUploadService.getFileEntityById(fileId).orElse(null);
            if (fileEntity == null) return ResponseEntity.notFound().build();

            // safely extract user id (may be null if anonymous)
            var auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getName()))
                    ? auth.getName() : null;

            // single source of truth for access
            if (!fileSecurityService.hasFileAccess(userId, fileEntity)) {
                return ResponseEntity.status(403).build();
            }

            Resource resource = fileUploadService.loadFileAsResource(fileId);
            var info = fileUploadService.getFileById(fileId).orElseThrow();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(info.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + info.getOriginalFileName() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error viewing file: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete file", description = "Delete file by ID")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "File ID") @PathVariable String fileId) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication.getName();

            boolean deleted = fileUploadService.deleteFile(fileId, userId);
            if (deleted) {
                // If the deleted file was used as a profile picture, clear it
                userRepository.findById(userId).ifPresent(u -> {
                    if (fileId.equals(u.getProfilePictureId())) {
                        u.setProfilePictureId(null);
                        userRepository.save(u);
                    }
                });
            }
            return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
