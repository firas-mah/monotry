package com.example.jobup.controller;

import com.example.jobup.dto.AuthResponseDto;
import com.example.jobup.dto.LoginRequestDto;
import com.example.jobup.dto.RegisterRequestDto;
import com.example.jobup.dto.UserUpdateRequestDto;
import com.example.jobup.entities.User;
import com.example.jobup.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user with ROLE_CLIENT by default")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        try {
            AuthResponseDto response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user and returns JWT token with roles")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        try {
            AuthResponseDto response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/update")
    @Operation(summary = "Update current user info")
    public ResponseEntity<AuthResponseDto> updateCurrentUser(
            @Valid @RequestBody UserUpdateRequestDto request,
            Authentication authentication
    ) {
        String userId = authentication.getName(); // now the DB id
        try {
            AuthResponseDto response = authService.updateUser(userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public ResponseEntity<AuthResponseDto> getCurrentUser(Authentication authentication) {
        // roles from authorities; id from Authentication name
        String userId = authentication.getName();
        var roles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(AuthResponseDto.builder()
                .userId(userId)
                .roles(roles)
                .build());
    }

}

