package com.example.jobup.repositories;

import com.example.jobup.entities.FileUpload;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepository extends MongoRepository<FileUpload, String> {
    
    List<FileUpload> findByOwnerIdAndCategoryAndIsActiveTrue(String ownerId, FileUpload.FileCategory category);
    
    List<FileUpload> findByOwnerIdAndIsActiveTrue(String ownerId);
    
    List<FileUpload> findByUploadedByAndIsActiveTrue(String uploadedBy);
    
    Optional<FileUpload> findByIdAndIsActiveTrue(String id);
    
    Optional<FileUpload> findByStoredFileNameAndIsActiveTrue(String storedFileName);
    
    List<FileUpload> findByCategoryAndIsActiveTrue(FileUpload.FileCategory category);
    
    void deleteByOwnerIdAndCategory(String ownerId, FileUpload.FileCategory category);
}
