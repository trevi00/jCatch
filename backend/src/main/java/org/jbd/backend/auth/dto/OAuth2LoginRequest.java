package org.jbd.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class OAuth2LoginRequest {
    
    @NotBlank(message = "Google ID token is required")
    private String idToken;
    
    public OAuth2LoginRequest() {}
    
    public OAuth2LoginRequest(String idToken) {
        this.idToken = idToken;
    }
    
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}