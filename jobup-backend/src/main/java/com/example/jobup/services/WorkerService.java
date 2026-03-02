package com.example.jobup.services;

import com.example.jobup.dto.WorkerCreateDto;
import com.example.jobup.dto.WorkerUpdateDto;
import com.example.jobup.dto.WorkerResponseDto;
import com.example.jobup.entities.User;
import com.example.jobup.entities.Worker;
import com.example.jobup.mapper.WorkerMapper;
import com.example.jobup.repositories.UserRepository;
import com.example.jobup.repositories.WorkerRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class WorkerService implements IWorkerService {

    private final WorkerRepo workerRepo;
    private final WorkerMapper workerMapper;
    private final UserRoleService userRoleService;
    private final UserRepository userRepository;

    public List<WorkerResponseDto> getAllWorkers(String excludeUserId) {
        var stream = workerRepo.findAll().stream();
        if (excludeUserId != null) {
            stream = stream.filter(w -> !excludeUserId.equals(w.getId())); // Worker.id == userId in your design
        }
        return stream.map(workerMapper::toResponseDto).collect(Collectors.toList());
    }

    // keep old method for compatibility
    public List<WorkerResponseDto> getAllWorkers() {
        return getAllWorkers(null);
    }

    public List<WorkerResponseDto> searchByLocation(String location, String excludeUserId) {
        var stream = workerRepo.findByLocationIgnoreCase(location).stream();
        if (excludeUserId != null) {
            stream = stream.filter(w -> !excludeUserId.equals(w.getId()));
        }
        return stream.map(workerMapper::toResponseDto).collect(Collectors.toList());
    }

    public List<WorkerResponseDto> searchByJobType(String jobType, String excludeUserId) {
        var stream = workerRepo.findByJobTypeIgnoreCase(jobType).stream();
        if (excludeUserId != null) {
            stream = stream.filter(w -> !excludeUserId.equals(w.getId()));
        }
        return stream.map(workerMapper::toResponseDto).collect(Collectors.toList());
    }

    public Optional<WorkerResponseDto> getWorkerById(String id) {
        return workerRepo.findById(id)
                .map(workerMapper::toResponseDto);
    }

    public WorkerResponseDto createWorker(WorkerCreateDto dto) {
        // Récupérer le User pour obtenir son fullName
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Worker entity = workerMapper.toEntity(dto);
        entity.setFullName(user.getUsername()); // Récupérer depuis User
        entity.setId(dto.getUserId());
        
        Worker saved = workerRepo.save(entity);
        
        // Ajouter le rôle WORKER à l'utilisateur
        userRoleService.addWorkerRole(dto.getUserId());
        
        return workerMapper.toResponseDto(saved);
    }

    public WorkerResponseDto updateWorker(String id, WorkerUpdateDto dto) {
        Worker existing = workerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        // Mise à jour partielle des champs
        workerMapper.updateWorkerFromDto(dto, existing);

        Worker updated = workerRepo.save(existing);
        return workerMapper.toResponseDto(updated);
    }

    public void deleteWorker(String id) {
        workerRepo.deleteById(id);
    }

    public List<WorkerResponseDto> searchByLocation(String location) {
        return workerRepo.findByLocationIgnoreCase(location)
                .stream()
                .map(workerMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    public List<WorkerResponseDto> searchByJobType(String jobType) {
        return workerRepo.findByJobTypeIgnoreCase(jobType)
                .stream()
                .map(workerMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // WorkerService.java

    public List<WorkerResponseDto> getAllWorkersExcept(String userId) {
        return workerRepo.findByIdNot(userId)
                .stream().map(workerMapper::toResponseDto).collect(Collectors.toList());
    }

    public List<WorkerResponseDto> searchByLocationExcept(String location, String userId) {
        return workerRepo.findByLocationIgnoreCaseAndIdNot(location, userId)
                .stream().map(workerMapper::toResponseDto).collect(Collectors.toList());
    }

    public List<WorkerResponseDto> searchByJobTypeExcept(String jobType, String userId) {
        return workerRepo.findByJobTypeIgnoreCaseAndIdNot(jobType, userId)
                .stream().map(workerMapper::toResponseDto).collect(Collectors.toList());
    }

}
