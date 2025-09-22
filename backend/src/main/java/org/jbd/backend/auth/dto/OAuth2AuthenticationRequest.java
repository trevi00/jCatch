package org.jbd.backend.auth.dto;

public class OAuth2AuthenticationRequest {
    
    private String provider;
    private String redirectUri;
    private String state;
    
    public OAuth2AuthenticationRequest() {}
    
    public OAuth2AuthenticationRequest(String provider, String redirectUri, String state) {
        this.provider = provider;
        this.redirectUri = redirectUri;
        this.state = state;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
}