package org.jbd.backend.auth.util;

import org.jbd.backend.auth.service.JwtService;
import org.jbd.backend.common.exception.BusinessException;
import org.jbd.backend.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 인증 관련 유틸리티 클래스
 *
 * JWT 토큰 처리 및 사용자 ID 추출을 위한 공통 메서드를 제공합니다.
 * 모든 컨트롤러에서 일관된 인증 처리가 가능하도록 합니다.
 *
 * @see JwtService
 */
@Component
public class AuthenticationUtils {

    private final JwtService jwtService;

    public AuthenticationUtils(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Authorization 헤더로부터 JWT 토큰을 추출합니다.
     *
     * @param authHeader Authorization 헤더 ("Bearer {token}" 형식)
     * @return 추출된 JWT 토큰
     * @throws BusinessException 헤더 형식이 유효하지 않은 경우
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return authHeader.substring(7);
    }

    /**
     * Authorization 헤더로부터 사용자 ID를 추출합니다.
     *
     * @param authHeader Authorization 헤더 ("Bearer {token}" 형식)
     * @return 사용자 ID
     * @throws BusinessException 헤더나 토큰이 유효하지 않은 경우
     */
    public Long extractUserIdFromHeader(String authHeader) {
        String token = extractTokenFromHeader(authHeader);
        return jwtService.extractUserId(token);
    }

    /**
     * Authorization 헤더로부터 사용자 이메일을 추출합니다.
     *
     * @param authHeader Authorization 헤더 ("Bearer {token}" 형식)
     * @return 사용자 이메일
     * @throws BusinessException 헤더나 토큰이 유효하지 않은 경우
     */
    public String extractEmailFromHeader(String authHeader) {
        String token = extractTokenFromHeader(authHeader);
        return jwtService.extractEmail(token);
    }

    /**
     * Spring Security Authentication 객체로부터 사용자 ID를 추출합니다.
     * 현재는 임시 구현으로 1L을 반환합니다.
     *
     * @param authentication Spring Security Authentication 객체
     * @return 사용자 ID
     * @deprecated 실제 JWT 기반 인증으로 대체 예정
     */
    @Deprecated
    public Long getUserIdFromAuthentication(Authentication authentication) {
        // TODO: 실제 JWT 기반 인증 구현
        // 현재는 테스트를 위한 임시 구현
        if (authentication != null && authentication.getName() != null) {
            return 1L; // 존재하는 테스트 사용자 ID
        }
        return 1L;
    }

    /**
     * 토큰 기반과 Authentication 객체 기반 방식을 통합하여 사용자 ID를 추출합니다.
     * Authorization 헤더가 있으면 토큰에서, 없으면 Authentication 객체에서 추출합니다.
     *
     * @param authHeader Authorization 헤더 (선택사항)
     * @param authentication Spring Security Authentication 객체 (선택사항)
     * @return 사용자 ID
     * @throws BusinessException 둘 다 유효하지 않은 경우
     */
    public Long extractUserId(String authHeader, Authentication authentication) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return extractUserIdFromHeader(authHeader);
        }

        if (authentication != null) {
            return getUserIdFromAuthentication(authentication);
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * Authorization 헤더에서 사용자 타입을 추출합니다.
     *
     * @param authHeader "Bearer {token}" 형식의 Authorization 헤더
     * @return 사용자 타입 (GENERAL, COMPANY, ADMIN)
     * @throws BusinessException 헤더나 토큰이 유효하지 않은 경우
     */
    public String extractUserTypeFromHeader(String authHeader) {
        String token = extractTokenFromHeader(authHeader);
        return jwtService.extractUserType(token);
    }

    /**
     * Authorization 헤더에서 관리자 여부를 추출합니다.
     *
     * @param authHeader "Bearer {token}" 형식의 Authorization 헤더
     * @return 관리자 여부 (true: 관리자, false: 일반 사용자)
     * @throws BusinessException 헤더나 토큰이 유효하지 않은 경우
     */
    public Boolean extractIsAdminFromHeader(String authHeader) {
        String token = extractTokenFromHeader(authHeader);
        return jwtService.extractClaim(token, claims -> claims.get("isAdmin", Boolean.class));
    }
}