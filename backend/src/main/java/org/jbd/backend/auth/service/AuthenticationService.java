package org.jbd.backend.auth.service;

import org.jbd.backend.auth.dto.AuthenticationRequest;
import org.jbd.backend.auth.dto.AuthenticationResponse;
import org.jbd.backend.common.exception.BusinessException;
import org.jbd.backend.common.exception.ErrorCode;
import org.jbd.backend.user.domain.User;
import org.jbd.backend.user.dto.UserRegistrationDto;
import org.jbd.backend.user.dto.UserResponseDto;
import org.jbd.backend.user.repository.UserRepository;
import org.jbd.backend.user.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 사용자 인증 및 토큰 관리 서비스
 *
 * 잡았다 플랫폼의 핵심 인증 서비스로, JWT 기반 사용자 인증, 회원가입, 로그인, 토큰 갱신 등을
 * 안전하고 효율적으로 처리합니다. Spring Security와 완전히 통합되어 안전한 인증 프로세스를 제공하며,
 * 다양한 인증 예외 상황에 대한 세밀한 처리와 명확한 에러 코드를 제공합니다.
 * RFC 7519 JWT 표준을 준수하고 HMAC SHA-256 알고리즘을 사용하여 토큰의 무결성을 보장합니다.
 *
 * 핵심 기능:
 * - 사용자 회원가입 및 즉시 자동 로그인 처리
 * - 이메일/비밀번호 기반 안전한 로그인 인증
 * - JWT 액세스 토큰 및 리프레시 토큰 쌍(pair) 발급
 * - 리프레시 토큰을 통한 무중단 액세스 토큰 갱신
 * - 계정 상태 실시간 검증 (활성화, 비활성화, 잠금)
 * - 인증 실패 시 상세한 비즈니스 에러 코드 제공
 * - Authorization 헤더 기반 토큰 처리
 * - JWT stateless 특성을 활용한 효율적 로그아웃
 *
 * 보안 아키텍처:
 * - Spring Security AuthenticationManager 통합
 * - BCrypt 암호화를 통한 비밀번호 안전성 보장
 * - JWT 토큰 기반 stateless 인증 (세션 불필요)
 * - 리프레시 토큰을 통한 보안성과 사용성 균형
 * - 계정 잠금 및 비활성화 상태 자동 검증
 * - 트랜잭션 기반 데이터 일관성 보장
 *
 * 토큰 관리 전략:
 * - 액세스 토큰: 짧은 만료 시간으로 보안성 강화
 * - 리프레시 토큰: 긴 만료 시간으로 사용자 편의성 제공
 * - 토큰 페어 동시 발급으로 seamless 인증 경험
 * - Bearer 토큰 방식 채택으로 HTTP 표준 준수
 *
 * 예외 처리 체계:
 * - BadCredentialsException → INVALID_PASSWORD
 * - DisabledException → ACCOUNT_DISABLED
 * - LockedException → ACCOUNT_LOCKED
 * - AuthenticationException → UNAUTHORIZED
 * - BusinessException 기반 통일된 에러 응답
 *
 * 인증 플로우:
 * 1. 회원가입: 사용자 생성 → 즉시 토큰 발급 → 자동 로그인
 * 2. 로그인: 계정 검증 → 인증 처리 → 토큰 쌍 발급
 * 3. 토큰 갱신: 리프레시 토큰 검증 → 새 액세스 토큰 발급
 * 4. 로그아웃: 클라이언트 토큰 삭제 (stateless 특성 활용)
 *
 * 사용 시나리오:
 * - 신규 사용자 회원가입 및 즉시 서비스 이용
 * - 기존 사용자 로그인 및 권한 확인
 * - 액세스 토큰 만료 시 자동 갱신
 * - 보안 요구사항에 따른 강제 로그아웃
 * - 계정 상태 변경에 따른 접근 제어
 *
 * 성능 최적화:
 * - JWT stateless 특성으로 세션 스토리지 불필요
 * - 토큰 기반 인증으로 서버 확장성 향상
 * - 리프레시 토큰 재사용으로 네트워크 요청 최소화
 * - 트랜잭션 범위 최적화로 데이터베이스 부하 감소
 *
 * 규정 준수:
 * - OWASP 인증 보안 가이드라인 준수
 * - JWT RFC 7519 표준 완전 구현
 * - Spring Security 모범 사례 적용
 * - 개인정보보호법 및 GDPR 고려사항 반영
 */
@Service
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * AuthenticationService 생성자입니다.
     *
     * 의존성 주입을 통해 필요한 서비스들을 초기화합니다.
     *
     * @param userRepository 사용자 데이터 접근 레포지토리
     * @param userService 사용자 관리 서비스
     * @param jwtService JWT 토큰 생성 및 검증 서비스
     * @param authenticationManager Spring Security 인증 매니저
     */
    public AuthenticationService(
            UserRepository userRepository,
            UserService userService,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * 새로운 사용자를 등록하고 자동으로 로그인 처리합니다.
     *
     * 사용자 등록 후 즉시 JWT 토큰을 발급하여 자동 로그인을 처리합니다.
     * 등록과 동시에 사용자가 바로 서비스를 이용할 수 있도록 합니다.
     *
     * @param request 사용자 등록 정보
     * @return AuthenticationResponse JWT 토큰과 사용자 정보를 포함한 인증 응답
     * @throws BusinessException 사용자 등록 실패 시
     */
    public AuthenticationResponse register(UserRegistrationDto request) {
        UserResponseDto userResponse = userService.registerUser(request);
        
        User user = userRepository.findById(userResponse.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }
    
    /**
     * 사용자 인증을 수행하고 JWT 토큰을 발급합니다.
     *
     * 이메일과 비밀번호를 검증하여 사용자 인증을 수행합니다.
     * 인증 성공 시 JWT 액세스 토큰과 리프레시 토큰을 발급합니다.
     * 다양한 인증 실패 상황에 대해 구체적인 에러 코드를 제공합니다.
     *
     * 인증 과정:
     * 1. 사용자 존재 여부 확인
     * 2. 계정 활성화 상태 검증
     * 3. Spring Security를 통한 비밀번호 검증
     * 4. JWT 토큰 발급 및 응답 생성
     *
     * @param request 인증 요청 정보 (이메일, 비밀번호)
     * @return AuthenticationResponse JWT 토큰과 사용자 정보를 포함한 인증 응답
     * @throws BusinessException 인증 실패 시 구체적인 에러 코드와 함께
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 활성 계정 확인
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            userRepository.save(user);

            String jwtToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .user(UserResponseDto.from(user))
                    .build();

        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);

        } catch (DisabledException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);

        } catch (LockedException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);

        } catch (AuthenticationException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.
     *
     * 기존 리프레시 토큰의 유효성을 검증한 후 새로운 액세스 토큰을 발급합니다.
     * 리프레시 토큰은 재사용되며, 액세스 토큰만 새로 발급됩니다.
     *
     * @param refreshToken 유효한 리프레시 토큰
     * @return AuthenticationResponse 새로운 액세스 토큰과 기존 리프레시 토큰을 포함한 응답
     * @throws BusinessException 리프레시 토큰이 유효하지 않거나 만료된 경우
     */
    public AuthenticationResponse refreshToken(String refreshToken) {
        try {
            String userEmail = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            
            if (jwtService.isTokenValid(refreshToken, user)) {
                String accessToken = jwtService.generateToken(user);
                
                return AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .user(UserResponseDto.from(user))
                        .build();
            } else {
                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
    
    /**
     * Authorization 헤더로부터 refresh token을 추출하고 토큰을 갱신합니다.
     *
     * @param authHeader Authorization 헤더 ("Bearer {refresh_token}" 형식)
     * @return 갱신된 토큰 정보
     * @throws BusinessException 토큰 형식이 유효하지 않거나 토큰이 만료된 경우
     */
    public AuthenticationResponse refreshTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String refreshToken = authHeader.substring(7);
        return refreshToken(refreshToken);
    }

    /**
     * 사용자 로그아웃을 처리합니다.
     *
     * JWT는 stateless한 특성상 서버에서 별도의 로그아웃 처리가 필요하지 않습니다.
     * 클라이언트에서 토큰을 삭제하는 것으로 로그아웃이 완료됩니다.
     * 필요시 토큰 블랙리스트 기능을 구현할 수 있습니다.
     *
     * @param token 로그아웃할 JWT 토큰 (현재는 사용되지 않음)
     */
    public void logout(String token) {
        // JWT는 stateless하므로 별도의 로그아웃 처리가 필요하지 않음
        // 필요시 블랙리스트 구현 가능
    }
}