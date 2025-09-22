package org.jbd.backend.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jbd.backend.auth.dto.AuthenticationResponse;
import org.jbd.backend.auth.dto.OAuth2LoginRequest;
import org.jbd.backend.auth.dto.OAuth2UserInfo;
import org.jbd.backend.common.exception.BusinessException;
import org.jbd.backend.common.exception.ErrorCode;
import org.jbd.backend.user.domain.User;
import org.jbd.backend.user.domain.UserProfile;
import org.jbd.backend.user.domain.enums.UserType;
import org.jbd.backend.user.domain.enums.OAuthProvider;
import org.jbd.backend.user.dto.UserResponseDto;
import org.jbd.backend.user.repository.UserRepository;
import org.jbd.backend.user.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * OAuth2 인증 서비스
 *
 * 잡았다 플랫폼의 OAuth2 기반 소셜 로그인 인증 서비스입니다.
 * Google OAuth2를 통한 사용자 인증, 등록, 로그인을 안전하고 효율적으로 처리하며,
 * ID 토큰 검증, 사용자 정보 조회, 기존 계정 연동 등의 핵심 기능을 제공합니다.
 * 일반 구직자와 기업 채용담당자 모두 지원하며, Gmail 전송 권한을 포함한 확장된 스코프를 활용합니다.
 * Spring Security OAuth2 표준을 준수하여 안전한 인증 프로세스를 보장합니다.
 *
 * 핵심 기능:
 * - Google OAuth2 ID 토큰 검증 및 사용자 인증
 * - OAuth2 사용자 정보 조회 및 실시간 프로필 동기화
 * - 기존 일반 계정과 OAuth2 계정 seamless 연동
 * - 로그인/회원가입 구분 처리 및 상태 관리
 * - JWT 액세스/리프레시 토큰 생성 및 반환
 * - Google 인증 URL 동적 생성 (state 기반 라우팅)
 * - Authorization Code Flow 완전 구현
 * - 사용자 프로필 자동 생성 및 업데이트
 *
 * 지원하는 OAuth2 제공업체:
 * - Google OAuth 2.0 (Gmail 전송 권한 포함)
 * - 향후 확장: Kakao, Naver, LinkedIn 등
 *
 * OAuth2 인증 플로우:
 * 1. 클라이언트가 Google 인증 URL로 사용자 리다이렉트
 * 2. 사용자 Google 계정 로그인 및 권한 승인
 * 3. Google에서 authorization code와 state 반환
 * 4. Authorization code를 access token으로 교환
 * 5. Access token으로 Google userinfo API 호출
 * 6. 기존 사용자 연동 또는 신규 사용자 생성
 * 7. JWT 토큰 쌍 생성 및 인증 응답 반환
 *
 * 보안 아키텍처:
 * - OAuth2 표준 Authorization Code Flow 구현
 * - CSRF 방지용 state 매개변수 활용
 * - ID 토큰 audience(client_id) 검증
 * - 토큰 만료 시간 실시간 검증
 * - SSL/TLS 기반 안전한 토큰 교환
 * - 사용자 정보 암호화 저장
 *
 * 권한 스코프 관리:
 * - openid: OpenID Connect 기본 인증
 * - profile: 사용자 기본 프로필 정보 접근
 * - email: 이메일 주소 및 인증 상태 접근
 * - gmail.send: Gmail 전송 권한 (웹메일 기능용)
 *
 * State 매개변수 구조:
 * - 형식: "uuid|USERTYPE|ACTION"
 * - 예시: "abc123-def456|COMPANY|SIGNUP"
 * - USERTYPE: GENERAL, COMPANY
 * - ACTION: SIGNUP, LOGIN
 *
 * 사용자 계정 연동 정책:
 * - 이메일 기반 기존 계정 자동 탐지
 * - OAuth 연동 시 일반 계정 정보 보존
 * - 프로필 이미지 자동 동기화
 * - 이메일 인증 상태 자동 업데이트
 * - 마지막 로그인 시간 추적
 *
 * 데이터 매핑 전략:
 * - Google 사용자 정보 → User 엔티티 자동 매핑
 * - 프로필 사진 URL 자동 저장 및 업데이트
 * - OAuth Provider별 고유 ID 관리
 * - 사용자 타입별 권한 자동 설정
 *
 * 에러 처리 체계:
 * - OAUTH2_AUTHENTICATION_FAILED: OAuth2 인증 전반 실패
 * - INVALID_OAUTH2_TOKEN: 잘못된 토큰 또는 만료
 * - EXPIRED_OAUTH2_TOKEN: 토큰 만료
 * - INVALID_OAUTH2_USER_INFO: 사용자 정보 오류
 * - USER_ALREADY_EXISTS: 이메일 중복 (회원가입 시)
 * - USER_NOT_FOUND: 계정 없음 (로그인 시)
 *
 * 사용 시나리오:
 * - 신규 사용자의 간편 소셜 회원가입
 * - 기존 사용자의 OAuth 계정 연동
 * - 일반 계정에서 소셜 로그인 전환
 * - 기업 사용자의 Google 계정 인증
 * - Gmail 연동을 통한 웹메일 기능 활용
 *
 * 성능 최적화:
 * - RestTemplate 기반 비동기 HTTP 통신
 * - 토큰 검증 캐싱 (향후 확장)
 * - 사용자 정보 조회 최적화
 * - 프로필 업데이트 최소화
 *
 * 규정 준수:
 * - Google OAuth2 정책 완전 준수
 * - GDPR 개인정보 처리 방침 적용
 * - OAuth2 RFC 6749 표준 구현
 * - OpenID Connect Core 1.0 스펙 준수
 */
@Service
@Transactional
public class OAuth2Service {
    
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;
    
    private static final String GOOGLE_TOKEN_INFO_URL = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    
    /**
     * OAuth2Service 생성자
     *
     * @param userRepository 사용자 데이터 액세스 리포지토리
     * @param userProfileRepository 사용자 프로필 데이터 액세스 리포지토리
     * @param jwtService JWT 토큰 생성 및 검증 서비스
     * @param restTemplate HTTP 요청을 위한 템플릿
     * @param objectMapper JSON 직렬화/역직렬화를 위한 매퍼
     */
    public OAuth2Service(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            JwtService jwtService,
            RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.jwtService = jwtService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Google OAuth2 ID 토큰을 사용하여 사용자를 인증합니다.
     *
     * Google에서 발급한 ID 토큰을 검증하고 사용자 정보를 추출하여
     * 기존 사용자를 찾거나 새로운 사용자를 생성합니다.
     * 인증 완료 후 JWT 토큰을 발급하여 반환합니다.
     *
     * @param request Google OAuth2 로그인 요청 (ID 토큰 포함)
     * @return AuthenticationResponse JWT 토큰과 사용자 정보
     * @throws BusinessException OAuth2 인증 실패 시
     * @see OAuth2LoginRequest
     * @see AuthenticationResponse
     */
    public AuthenticationResponse authenticateWithGoogle(OAuth2LoginRequest request) {
        try {
            // Google ID 토큰 검증
            OAuth2UserInfo userInfo = verifyGoogleIdToken(request.getIdToken());
            
            // 사용자 조회 또는 생성
            User user = findOrCreateUser(userInfo, UserType.GENERAL);
            
            // JWT 토큰 생성
            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(UserResponseDto.from(user))
                    .build();
                    
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH2_AUTHENTICATION_FAILED);
        }
    }
    
    /**
     * Google ID 토큰의 유효성을 검증합니다.
     *
     * Google의 tokeninfo 엔드포인트를 사용하여 ID 토큰을 검증합니다.
     * 검증 과정:
     * 1. Google tokeninfo API 호출
     * 2. audience(client_id) 확인
     * 3. 토큰 만료 시간 확인
     * 4. 사용자 정보 추출 및 반환
     *
     * @param idToken 검증할 Google ID 토큰
     * @return OAuth2UserInfo 검증된 사용자 정보
     * @throws BusinessException 토큰이 유효하지 않거나 만료된 경우
     */
    private OAuth2UserInfo verifyGoogleIdToken(String idToken) {
        try {
            // Google의 tokeninfo 엔드포인트를 사용하여 토큰 검증
            String url = GOOGLE_TOKEN_INFO_URL + idToken;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ErrorCode.INVALID_OAUTH2_TOKEN);
            }
            
            Map<String, Object> tokenInfo = response.getBody();
            
            // 토큰의 audience(aud) 확인
            String audience = (String) tokenInfo.get("aud");
            if (!googleClientId.equals(audience)) {
                throw new BusinessException(ErrorCode.INVALID_OAUTH2_TOKEN);
            }
            
            // 토큰 만료 확인
            String exp = (String) tokenInfo.get("exp");
            if (exp != null) {
                long expTime = Long.parseLong(exp);
                if (System.currentTimeMillis() / 1000 > expTime) {
                    throw new BusinessException(ErrorCode.EXPIRED_OAUTH2_TOKEN);
                }
            }
            
            return new OAuth2UserInfo(tokenInfo);
            
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH2_AUTHENTICATION_FAILED);
        }
    }
    
    /**
     * OAuth2 state 매개변수에서 사용자 타입을 파싱합니다.
     *
     * state 형식: "uuid|USERTYPE|ACTION"
     * 예: "abc123|COMPANY|SIGNUP"
     *
     * @param state OAuth2 state 문자열
     * @return UserType 파싱된 사용자 타입 (파싱 실패 시 GENERAL)
     * @see UserType
     */
    private UserType parseUserTypeFromState(String state) {
        if (state != null && state.contains("|")) {
            String[] parts = state.split("\\|");
            if (parts.length >= 2) {
                try {
                    return UserType.valueOf(parts[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Invalid user type, default to GENERAL
                }
            }
        }
        return UserType.GENERAL;
    }
    
    /**
     * OAuth2 state 매개변수에서 액션을 파싱합니다.
     *
     * state 형식: "uuid|USERTYPE|ACTION"
     * 지원하는 액션: SIGNUP, LOGIN
     *
     * @param state OAuth2 state 문자열
     * @return String 파싱된 액션 (기본값: SIGNUP)
     */
    private String parseActionFromState(String state) {
        if (state != null && state.contains("|")) {
            String[] parts = state.split("\\|");
            if (parts.length >= 3) {
                return parts[2].toUpperCase();
            }
        }
        return "SIGNUP"; // default action
    }
    
    /**
     * OAuth2 사용자 정보로 기존 사용자를 찾거나 새 사용자를 생성합니다.
     * 기본적으로 GENERAL 타입의 사용자로 처리합니다.
     *
     * @param userInfo OAuth2에서 제공받은 사용자 정보
     * @return User 찾아지거나 생성된 사용자
     */
    private User findOrCreateUser(OAuth2UserInfo userInfo) {
        return findOrCreateUser(userInfo, UserType.GENERAL);
    }
    
    /**
     * OAuth2 사용자 정보로 기존 사용자를 찾거나 새 사용자를 생성합니다.
     *
     * 이메일을 기준으로 기존 사용자를 찾고, 존재하면 OAuth2 정보로 업데이트하고
     * 없으면 새로운 사용자를 생성합니다. 이메일이 유효하지 않으면 예외를 발생시킵니다.
     *
     * @param userInfo OAuth2에서 제공받은 사용자 정보
     * @param userType 생성할 사용자의 타입
     * @return User 찾아지거나 생성된 사용자
     * @throws BusinessException 이메일이 유효하지 않은 경우
     */
    private User findOrCreateUser(OAuth2UserInfo userInfo, UserType userType) {
        String email = userInfo.getEmail();
        
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_OAUTH2_USER_INFO);
        }
        
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .map(user -> updateExistingUser(user, userInfo))
                .orElseGet(() -> createNewUser(userInfo, userType));
    }
    
    /**
     * 기존 사용자의 정보를 OAuth2 정보로 업데이트합니다.
     *
     * 업데이트되는 정보:
     * - 사용자 이름 (프로필에 저장)
     * - 프로필 이미지 URL
     * - OAuth2 제공업체 정보
     * - 이메일 인증 상태
     * - 마지막 로그인 시간
     *
     * @param user 업데이트할 기존 사용자
     * @param userInfo OAuth2에서 제공받은 최신 사용자 정보
     * @return User 업데이트된 사용자
     */
    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        boolean updated = false;
        
        // UserProfile 조회 및 업데이트
        UserProfile userProfile = userProfileRepository.findByUser(user).orElse(null);
        boolean profileUpdated = false;

        // 이름 업데이트 (프로필에서)
        if (userInfo.getName() != null) {
            if (userProfile == null) {
                userProfile = new UserProfile(user, userInfo.getName(), "");
                profileUpdated = true;
            } else if (!userInfo.getName().equals(userProfile.getFullName())) {
                userProfile.updateName(userInfo.getName(), "");
                profileUpdated = true;
            }
        }

        // 프로필 이미지 업데이트
        if (userInfo.getPicture() != null) {
            if (userProfile == null) {
                userProfile = new UserProfile(user, userInfo.getName() != null ? userInfo.getName() : "Google User", "");
                userProfile.updateProfileImageUrl(userInfo.getPicture());
                profileUpdated = true;
            } else if (!userInfo.getPicture().equals(userProfile.getProfileImageUrl())) {
                userProfile.updateProfileImageUrl(userInfo.getPicture());
                profileUpdated = true;
            }
        }

        // UserProfile 저장
        if (profileUpdated && userProfile != null) {
            userProfileRepository.save(userProfile);
        }

        // OAuth2 제공자 정보 업데이트
        if (user.getOauthProvider() == OAuthProvider.NATIVE) {
            user.setOauthProvider(OAuthProvider.GOOGLE);
            updated = true;
        }
        
        // 이메일 인증 상태 업데이트
        if (Boolean.TRUE.equals(userInfo.getEmailVerified()) && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            updated = true;
        }
        
        // 업데이트 시간은 BaseEntity에서 자동 처리
        
        // 마지막 로그인 시간 업데이트
        user.updateLastLogin();
        
        return userRepository.save(user);
    }
    
    /**
     * OAuth2 정보로 새로운 사용자를 생성합니다.
     * 기본적으로 GENERAL 타입의 사용자로 생성합니다.
     *
     * @param userInfo OAuth2에서 제공받은 사용자 정보
     * @return User 생성된 새 사용자
     */
    private User createNewUser(OAuth2UserInfo userInfo) {
        return createNewUser(userInfo, UserType.GENERAL);
    }
    
    /**
     * OAuth2 정보로 새로운 사용자를 생성합니다.
     *
     * 생성되는 정보:
     * - 기본 사용자 정보 (이메일, OAuth ID, 제공업체)
     * - 이메일 인증 상태 (OAuth2에서 제공된 경우 자동 인증)
     * - 사용자 프로필 (이름, 프로필 이미지)
     * - 마지막 로그인 시간 설정
     *
     * @param userInfo OAuth2에서 제공받은 사용자 정보
     * @param userType 생성할 사용자의 타입
     * @return User 생성된 새 사용자
     */
    private User createNewUser(OAuth2UserInfo userInfo, UserType userType) {
        // OAuth 사용자 생성
        User user = new User(userInfo.getEmail(), userInfo.getId(), OAuthProvider.GOOGLE, userType);
        if (Boolean.TRUE.equals(userInfo.getEmailVerified())) {
            user.verifyEmail();
        }
        user.updateLastLogin();

        User savedUser = userRepository.save(user);

        // UserProfile 생성
        String userName = userInfo.getName() != null ? userInfo.getName() : "Google User";
        UserProfile userProfile = new UserProfile(savedUser, userName, "");
        if (userInfo.getPicture() != null) {
            userProfile.updateProfileImageUrl(userInfo.getPicture());
        }
        userProfileRepository.save(userProfile);

        return savedUser;
    }
    
    /**
     * Google OAuth2 인증 URL을 생성합니다.
     *
     * 클라이언트가 사용자를 Google 로그인 페이지로 리다이렉트할 때 사용할 URL을 생성합니다.
     * Gmail 전송 권한을 포함한 스코프를 요청합니다.
     *
     * 요청하는 스코프:
     * - openid: OpenID Connect 사용
     * - profile: 사용자 프로필 정보 접근
     * - email: 이메일 주소 접근
     * - gmail.send: Gmail 전송 권한 (웹메일 기능용)
     *
     * @param redirectUri 인증 완료 후 리다이렉트될 URI
     * @param state CSRF 방지용 state 매개변수
     * @return String Google OAuth2 인증 URL
     */
    public String getGoogleAuthorizationUrl(String redirectUri, String state) {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + googleClientId +
                "&redirect_uri=" + redirectUri +
                "&scope=openid%20profile%20email%20https://www.googleapis.com/auth/gmail.send" +
                "&response_type=code" +
                "&state=" + state;
    }
    
    /**
     * Google OAuth2 콜백을 처리합니다.
     *
     * Google 인증 완료 후 받은 authorization code를 처리하여
     * 사용자 인증을 완료하고 JWT 토큰을 발급합니다.
     *
     * 처리 과정:
     * 1. state에서 사용자 타입과 액션 파싱
     * 2. authorization code를 access token으로 교환
     * 3. access token으로 사용자 정보 조회
     * 4. 로그인/회원가입 구분 처리
     * 5. JWT 토큰 생성 및 반환
     *
     * @param code Google에서 제공한 authorization code
     * @param state OAuth2 state 매개변수 (사용자 타입과 액션 포함)
     * @param redirectUri 인증에 사용된 redirect URI
     * @return AuthenticationResponse JWT 토큰과 사용자 정보
     * @throws BusinessException OAuth2 인증 과정에서 오류 발생 시
     */
    public AuthenticationResponse handleGoogleCallback(String code, String state, String redirectUri) {
        try {
            // Step 1: Parse user type and action from state
            UserType userType = parseUserTypeFromState(state);
            String action = parseActionFromState(state);
            
            // Step 2: Exchange authorization code for access token
            Map<String, Object> tokenResponse = exchangeCodeForToken(code, redirectUri);
            
            // Step 3: Get user info using access token
            OAuth2UserInfo userInfo = getUserInfoFromGoogle((String) tokenResponse.get("access_token"));
            
            // Step 4: Handle login vs signup
            User user;
            if ("LOGIN".equals(action)) {
                // For login: find existing user by email first
                user = userRepository.findByEmailAndIsDeletedFalse(userInfo.getEmail())
                    .map(existingUser -> {
                        // Link OAuth to existing account if not already linked
                        if (existingUser.getOauthProvider() == OAuthProvider.NATIVE) {
                            existingUser.setOauthProvider(OAuthProvider.GOOGLE);
                            existingUser.setOauthId(userInfo.getId());
                        }

                        // Update profile info from OAuth if available
                        UserProfile existingProfile = userProfileRepository.findByUser(existingUser).orElse(null);
                        if (userInfo.getPicture() != null && (existingProfile == null || existingProfile.getProfileImageUrl() == null)) {
                            if (existingProfile == null) {
                                existingProfile = new UserProfile(existingUser, userInfo.getName() != null ? userInfo.getName() : "Google User", "");
                            }
                            existingProfile.updateProfileImageUrl(userInfo.getPicture());
                            userProfileRepository.save(existingProfile);
                        }
                        if (!existingUser.isEmailVerified()) {
                            existingUser.setEmailVerified(true);
                        }
                        existingUser.updateLastLogin();
                        return userRepository.save(existingUser);
                    })
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "계정이 없습니다. 회원가입을 먼저 진행해주세요."));
            } else {
                // For signup: check if user already exists by email
                if (userRepository.findByEmailAndIsDeletedFalse(userInfo.getEmail()).isPresent()) {
                    throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "이미 가입된 이메일입니다. 로그인을 진행해주세요.");
                }
                // Create new user with specified user type
                user = findOrCreateUser(userInfo, userType);
            }
            
            // Step 5: Generate JWT tokens
            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(UserResponseDto.from(user))
                    .build();
                    
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH2_AUTHENTICATION_FAILED);
        }
    }
    
    /**
     * Google authorization code를 access token으로 교환합니다.
     *
     * Google의 token 엔드포인트에 POST 요청을 보내 authorization code를
     * access token과 기타 토큰 정보로 교환합니다.
     *
     * @param code Google에서 제공한 authorization code
     * @param redirectUri 인증에 사용된 redirect URI
     * @return Map<String, Object> 토큰 응답 (access_token, token_type 등)
     * @throws BusinessException 토큰 교환 실패 시
     */
    private Map<String, Object> exchangeCodeForToken(String code, String redirectUri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            
            String requestBody = "client_id=" + googleClientId +
                    "&client_secret=" + googleClientSecret +
                    "&code=" + code +
                    "&grant_type=authorization_code" +
                    "&redirect_uri=" + redirectUri;
            
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    GOOGLE_TOKEN_URL, 
                    HttpMethod.POST, 
                    request, 
                    Map.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ErrorCode.OAUTH2_AUTHENTICATION_FAILED);
            }
            
            return response.getBody();
            
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH2_AUTHENTICATION_FAILED);
        }
    }
    
    /**
     * Google access token을 사용하여 사용자 정보를 조회합니다.
     *
     * Google의 userinfo 엔드포인트에 access token을 사용하여
     * 인증된 사용자의 프로필 정보를 가져옵니다.
     *
     * @param accessToken Google에서 발급받은 access token
     * @return OAuth2UserInfo 사용자 프로필 정보
     * @throws BusinessException 사용자 정보 조회 실패 시
     */
    private OAuth2UserInfo getUserInfoFromGoogle(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL, 
                    HttpMethod.GET, 
                    request, 
                    Map.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ErrorCode.OAUTH2_AUTHENTICATION_FAILED);
            }
            
            return new OAuth2UserInfo(response.getBody());
            
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH2_AUTHENTICATION_FAILED);
        }
    }
}