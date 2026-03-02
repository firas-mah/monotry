package com.example.jobup.services;

import com.example.jobup.entities.Role;
import com.example.jobup.entities.User;
import com.example.jobup.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoleService {
    private final UserRepository userRepository;

    public User addWorkerRole(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.addRole(Role.ROLE_WORKER);
        User savedUser = userRepository.save(user);
        log.info("Added WORKER role to user: {}", user.getUsername());

        return savedUser;
    }
}