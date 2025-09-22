package org.jbd.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jbd.backend.user.dto.UserResponseDto;

/**
 * 사용자 인증 응답 DTO
 *
 * 잡았다 플랫폼의 사용자 로그인 성공 시 클라이언트에게 반환되는 인증 정보를 담는 데이터 전송 객체입니다.
 * JWT 기반 토큰 인증을 구현하며, OAuth 2.0 표준을 따라 액세스 토큰과 리프레시 토큰을 포함합니다.
 * 클라이언트는 이 응답을 받아 토큰을 저장하고 후속 API 요청에 사용합니다.
 *
 * 주요 구성 요소:
 * - 액세스 토큰: API 요청 시 인증에 사용하는 JWT 토큰
 * - 리프레시 토큰: 액세스 토큰 갱신에 사용하는 장기 유효 토큰
 * - 토큰 타입: Bearer 토큰 타입 (RFC 6750)
 * - 만료 시간: 액세스 토큰의 유효 기간 (초 단위)
 * - 사용자 정보: 인증된 사용자의 기본 정보
 *
 * JWT 토큰 정보:
 * - 알고리즘: HMAC SHA-256 (HS256)
 * - 유효 기간: 24시간 (86400초)
 * - 페이로드: 사용자 ID, 이메일, 권한, 발행 시간 등
 * - 서명: 서버 비밀키로 무결성 보장
 *
 * OAuth 2.0 준수:
 * - RFC 6749: OAuth 2.0 Authorization Framework
 * - RFC 6750: Bearer Token Usage
 * - access_token, refresh_token, token_type, expires_in 필드
 * - snake_case JSON 프로퍼티 명명 규칙
 *
 * 보안 고려사항:
 * - 토큰은 HTTPS로만 전송
 * - 리프레시 토큰은 secure storage에 저장
 * - 액세스 토큰은 메모리 또는 sessionStorage 권장
 * - 토큰 만료 시 자동 갱신 로직 구현 필요
 *
 * 클라이언트 사용법:
 * ```javascript
 * // Authorization 헤더에 Bearer 토큰 포함
 * headers: {
 *   'Authorization': `Bearer ${response.access_token}`
 * }
 * ```
 *
 * 응답 예시:
 * ```json
 * {
 *   "access_token": "eyJhbGciOiJIUzI1NiJ9...",
 *   "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
 *   "token_type": "Bearer",
 *   "expires_in": 86400,
 *   "user": {
 *     "id": 1,
 *     "email": "user@example.com",
 *     "name": "홍길동"
 *   }
 * }
 * ```
 *
 * Builder 패턴:
 * - 객체 생성의 가독성과 안전성 향상
 * - 메소드 체이닝을 통한 유연한 객체 구성
 * - 불변성 보장 및 유효성 검증 가능
 *
 * 관련 클래스:
 * @see AuthenticationRequest 인증 요청 DTO
 * @see UserResponseDto 사용자 정보 응답 DTO
 * @see org.jbd.backend.auth.service.JwtService JWT 토큰 관리 서비스
 * @see org.jbd.backend.auth.controller.AuthController 인증 컨트롤러
 */
public class AuthenticationResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("token_type")
    private String tokenType = "Bearer";
    
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    private UserResponseDto user;
    
    public AuthenticationResponse() {}
    
    public AuthenticationResponse(String accessToken, String refreshToken, UserResponseDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.expiresIn = 86400L; // 24 hours in seconds
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String accessToken;
        private String refreshToken;
        private UserResponseDto user;
        
        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }
        
        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }
        
        public Builder user(UserResponseDto user) {
            this.user = user;
            return this;
        }
        
        public AuthenticationResponse build() {
            return new AuthenticationResponse(accessToken, refreshToken, user);
        }
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public UserResponseDto getUser() {
        return user;
    }
    
    public void setUser(UserResponseDto user) {
        this.user = user;
    }
}