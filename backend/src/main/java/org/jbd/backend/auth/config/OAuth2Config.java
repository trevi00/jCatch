package org.jbd.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth2 Google 인증 설정 클래스
 *
 * Google OAuth2 소셜 로그인에 필요한 설정 정보를 application.yml에서 바인딩하여
 * 관리하는 설정 클래스입니다. 환경별로 다른 OAuth2 설정값들을 외부화하여 관리합니다.
 *
 * 주요 설정:
 * - clientId: Google Developer Console에서 발급받은 클라이언트 ID
 * - clientSecret: Google Developer Console에서 발급받은 클라이언트 시크릿
 * - scope: 요청할 Google API 권한 범위 (openid, profile, email 등)
 * - redirectUri: OAuth2 인증 완료 후 리다이렉트될 URI
 * - authorizationGrantType: OAuth2 인증 플로우 타입 (일반적으로 authorization_code)
 *
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 * @see ConfigurationProperties
 * @see OAuth2AuthenticationSuccessHandler
 * @see OAuth2AuthenticationFailureHandler
 */
@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.google")
public class OAuth2Config {

    /** Google OAuth2 클라이언트 ID */
    private String clientId;

    /** Google OAuth2 클라이언트 시크릿 */
    private String clientSecret;

    /** 요청할 OAuth2 권한 범위 */
    private String scope;

    /** OAuth2 인증 완료 후 리다이렉트 URI */
    private String redirectUri;

    /** OAuth2 인증 플로우 타입 */
    private String authorizationGrantType;
    
    // Getter and Setter 메서드들
    // Spring Boot의 ConfigurationProperties가 application.yml의 값을 바인딩할 때 사용됩니다.

    /**
     * Google OAuth2 클라이언트 ID를 반환합니다.
     * @return Google Developer Console에서 발급받은 클라이언트 ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Google OAuth2 클라이언트 ID를 설정합니다.
     * @param clientId 설정할 클라이언트 ID
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Google OAuth2 클라이언트 시크릿을 반환합니다.
     * @return Google Developer Console에서 발급받은 클라이언트 시크릿
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Google OAuth2 클라이언트 시크릿을 설정합니다.
     * @param clientSecret 설정할 클라이언트 시크릿
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * OAuth2 권한 범위를 반환합니다.
     * @return 요청할 권한 범위 (예: "openid,profile,email")
     */
    public String getScope() {
        return scope;
    }

    /**
     * OAuth2 권한 범위를 설정합니다.
     * @param scope 설정할 권한 범위
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * OAuth2 리다이렉트 URI를 반환합니다.
     * @return 인증 완료 후 리다이렉트될 URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * OAuth2 리다이렉트 URI를 설정합니다.
     * @param redirectUri 설정할 리다이렉트 URI
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * OAuth2 인증 플로우 타입을 반환합니다.
     * @return 인증 플로우 타입 (일반적으로 "authorization_code")
     */
    public String getAuthorizationGrantType() {
        return authorizationGrantType;
    }

    /**
     * OAuth2 인증 플로우 타입을 설정합니다.
     * @param authorizationGrantType 설정할 인증 플로우 타입
     */
    public void setAuthorizationGrantType(String authorizationGrantType) {
        this.authorizationGrantType = authorizationGrantType;
    }
}