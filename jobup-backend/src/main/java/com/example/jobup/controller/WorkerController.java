package com.example.jobup.controller;

import com.example.jobup.dto.WorkerCreateDto;
import com.example.jobup.dto.WorkerResponseDto;
import com.example.jobup.dto.WorkerUpdateDto;
import com.example.jobup.services.IWorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkerController {

    private final IWorkerService workerService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<WorkerResponseDto>> getAllWorkers(
            @RequestParam(value = "excludeMe", defaultValue = "false") boolean excludeMe,
            Authentication authentication) {

        String excludeUserId =
                (excludeMe && authentication != null && authentication.isAuthenticated()
                        && !"anonymousUser".equals(authentication.getName()))
                        ? authentication.getName()
                        : null;

        return ResponseEntity.ok(workerService.getAllWorkers(excludeUserId));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkerResponseDto> getWorkerById(@PathVariable String id) {
        return workerService.getWorkerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkerResponseDto> createWorker(@RequestBody WorkerCreateDto dto) {
        return ResponseEntity.ok(workerService.createWorker(dto));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkerResponseDto> updateWorker(@PathVariable String id,
                                                          @RequestBody WorkerUpdateDto dto) {
        return ResponseEntity.ok(workerService.updateWorker(id, dto));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteWorker(@PathVariable String id) {
        workerService.deleteWorker(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/search/location", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<WorkerResponseDto>> searchByLocation(
            @RequestParam String location,
            @RequestParam(value = "excludeMe", defaultValue = "false") boolean excludeMe,
            Authentication authentication) {

        String excludeUserId =
                (excludeMe && authentication != null && authentication.isAuthenticated()
                        && !"anonymousUser".equals(authentication.getName()))
                        ? authentication.getName()
                        : null;

        return ResponseEntity.ok(workerService.searchByLocation(location, excludeUserId));
    }

    @GetMapping(value = "/search/job", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<WorkerResponseDto>> searchByJobType(
            @RequestParam String jobType,
            @RequestParam(value = "excludeMe", defaultValue = "false") boolean excludeMe,
            Authentication authentication) {

        String excludeUserId =
                (excludeMe && authentication != null && authentication.isAuthenticated()
                        && !"anonymousUser".equals(authentication.getName()))
                        ? authentication.getName()
                        : null;

        return ResponseEntity.ok(workerService.searchByJobType(jobType, excludeUserId));
    }
}
