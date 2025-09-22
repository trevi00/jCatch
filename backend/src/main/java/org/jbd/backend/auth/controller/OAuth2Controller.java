package org.jbd.backend.auth.controller;

import org.jbd.backend.auth.dto.AuthenticationResponse;
import org.jbd.backend.auth.dto.OAuth2AuthenticationRequest;
import org.jbd.backend.auth.dto.OAuth2LoginRequest;
import org.jbd.backend.auth.service.OAuth2Service;
import org.jbd.backend.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * OAuth2 소셜 로그인 REST API 컨트롤러
 *
 * 잡았다 플랫폼의 OAuth2 기반 소셜 로그인 서비스를 제공하는 REST API 컨트롤러입니다.
 * Google, Naver, Kakao 등의 소셜 플랫폼과 연동하여 사용자가 간편하게 로그인할 수 있도록 지원하며,
 * 보안성과 사용자 경험을 동시에 고려한 소셜 로그인 기능을 제공합니다.
 *
 * 주요 기능:
 * - Google OAuth2 로그인 및 회원가입
 * - Naver OAuth2 로그인 및 회원가입 (향후 구현)
 * - Kakao OAuth2 로그인 및 회원가입 (향후 구현)
 * - 소셜 계정과 기존 계정 연동 및 통합
 * - 소셜 로그인 상태 관리 및 토큰 갱신
 *
 * OAuth2 연동 특징:
 * - 보안 고려 PKCE(Proof Key for Code Exchange) 지원
 * - State 매개변수를 이용한 CSRF 공격 방지
 * - 소셜 플랫폼에서 제공하는 사용자 정보 동기화
 * - 이메일 인증 자동 완료 및 신뢰도 보장
 * - 엑세스 토큰 보안 저장 및 관리
 *
 * 지원 소셜 플랫폼:
 * - Google: Gmail, Google Workspace 계정 지원
 * - Naver: 네이버 개인 및 비즈니스 계정 지원 (예정)
 * - Kakao: 카카오톡 및 카카오 비즈니스 계정 지원 (예정)
 * - LinkedIn: 전문 네트워킹 계정 지원 (예정)
 *
 * 사용 사례:
 * - 신규 사용자의 빠른 회원가입
 * - 기존 사용자의 소셜 계정 연동
 * - 모바일 앱에서의 간편한 로그인
 * - 회사 도메인 이메일로 기업 계정 인증
 * - 다중 디바이스에서의 동기화된 로그인 경험
 *
 * 보안 및 설정:
 * - 특정 프론트엔드 도메인에서만 CORS 접근 권한 부여
 * - OAuth2 토큰의 보안 저장 및 암호화
 * - 소셜 플랫폼 연동 오류 및 예외 상황 처리
 * - 사용자 동의 및 개인정보 수집 정책 준수
 *
 * API 엔드포인트:
 * - POST /auth/oauth2/google: Google OAuth2 로그인
 * - GET /auth/oauth2/google/url: Google OAuth2 로그인 URL 생성
 * - GET /auth/oauth2/google/callback: Google OAuth2 콜백 처리
 * - POST /auth/oauth2/naver: Naver OAuth2 로그인 (향후 구현)
 * - POST /auth/oauth2/kakao: Kakao OAuth2 로그인 (향후 구현)
 */
@RestController
@RequestMapping("/auth/oauth2")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003"})
public class OAuth2Controller {
    
    private final OAuth2Service oauth2Service;
    
    public OAuth2Controller(OAuth2Service oauth2Service) {
        this.oauth2Service = oauth2Service;
    }
    
    /**
     * Google OAuth2를 이용한 로그인 및 회원가입을 처리합니다.
     *
     * Google OAuth2 인증 플로우를 통해 사용자를 로그인시키거나 신규 회원가입을 처리하는 API입니다.
     * Google에서 제공하는 사용자 정보를 활용하여 자동으로 계정을 생성하거나 기존 계정에 로그인합니다.
     * 이메일 인증이 자동으로 완료되며, Google의 보안 인증을 통해 안전한 로그인을 보장합니다.
     *
     * @param request Google OAuth2 로그인 요청 정보
     *                - authorizationCode: Google에서 발급한 인증 코드 (필수)
     *                - redirectUri: 인증 후 리다이렉트될 URI (필수)
     *                - state: CSRF 방지를 위한 상태 값 (선택)
     * @return ResponseEntity<ApiResponse<AuthenticationResponse>> 로그인 결과
     *         - accessToken: JWT 액세스 토큰
     *         - refreshToken: 리프레시 토큰
     *         - user: 사용자 기본 정보
     *         - isNewUser: 신규 가입 여부
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticateWithGoogle(
            @Valid @RequestBody OAuth2LoginRequest request
    ) {
        AuthenticationResponse response = oauth2Service.authenticateWithGoogle(request);
        
        return ResponseEntity.ok(
            ApiResponse.success("Google 로그인 성공", response)
        );
    }
    
    @GetMapping("/google/url")
    public ResponseEntity<ApiResponse<String>> getGoogleAuthUrl(
            @RequestParam(required = false, defaultValue = "http://localhost:3000/auth/callback") String redirectUri,
            @RequestParam(required = false, defaultValue = "GENERAL") String userType,
            @RequestParam(required = false, defaultValue = "SIGNUP") String action
    ) {
        // UUID, userType, action을 조합한 state 생성
        String state = UUID.randomUUID().toString() + "|" + userType + "|" + action;
        String authUrl = oauth2Service.getGoogleAuthorizationUrl(redirectUri, state);
        
        return ResponseEntity.ok(
            ApiResponse.success("Google 인증 URL 생성 성공", authUrl)
        );
    }
    
    @PostMapping("/authorize")
    public ResponseEntity<ApiResponse<String>> getAuthorizationUrl(
            @Valid @RequestBody OAuth2AuthenticationRequest request
    ) {
        if (!"google".equals(request.getProvider())) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("지원하지 않는 OAuth2 제공자입니다: " + request.getProvider())
            );
        }
        
        String state = request.getState() != null ? request.getState() : UUID.randomUUID().toString();
        String redirectUri = request.getRedirectUri() != null ? 
            request.getRedirectUri() : "http://localhost:3000/auth/callback";
            
        String authUrl = oauth2Service.getGoogleAuthorizationUrl(redirectUri, state);
        
        return ResponseEntity.ok(
            ApiResponse.success("인증 URL 생성 성공", authUrl)
        );
    }
    
    @PostMapping("/google/callback")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> handleGoogleCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "redirectUri", required = false, defaultValue = "http://localhost:3000/auth/callback") String redirectUri
    ) {
        try {
            AuthenticationResponse response = oauth2Service.handleGoogleCallback(code, state, redirectUri);
            
            return ResponseEntity.ok(
                ApiResponse.success("Google OAuth2 콜백 처리 성공", response)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("OAuth2 콜백 처리 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
    
    @GetMapping("/google/callback")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> handleGoogleCallbackRedirect(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "redirectUri", required = false, defaultValue = "http://localhost:3000/auth/callback") String redirectUri
    ) {
        try {
            AuthenticationResponse response = oauth2Service.handleGoogleCallback(code, state, redirectUri);
            
            return ResponseEntity.ok(
                ApiResponse.success("Google OAuth2 콜백 처리 성공", response)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("OAuth2 콜백 처리 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
}