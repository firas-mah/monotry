package com.example.jobup.security;

import java.security.Principal;

public record CurrentUser(String id, String username) implements Principal {
    @Override 
    public String getName() { 
        return id; 
    }
}