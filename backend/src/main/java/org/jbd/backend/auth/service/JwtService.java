package org.jbd.backend.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.jbd.backend.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT(JSON Web Token) 토큰 관리 서비스
 *
 * 잡았다 플랫폼의 인증 및 권한 관리를 위한 JWT 토큰의 생성, 파싱, 검증을 담당하는 핵심 서비스입니다.
 * RFC 7519 표준을 따르며, HMAC SHA-256 알고리즘을 사용하여 토큰의 무결성과 인증을 보장합니다.
 * 액세스 토큰과 리프레시 토큰을 구분하여 관리하며, 사용자 정보와 권한 정보를 안전하게 토큰에 포함시킵니다.
 *
 * 주요 기능:
 * - JWT 액세스 토큰 생성 및 검증 (24시간 유효)
 * - JWT 리프레시 토큰 생성 및 관리 (7일 유효)
 * - 사용자 정보 및 권한 정보 토큰 임베딩
 * - 토큰 서명 및 무결성 검증
 * - 커스텀 클레임을 통한 확장 가능한 토큰 구조
 * - 관리자 전용 토큰 생성 및 관리
 *
 * 토큰 구조 (Header.Payload.Signature):
 * - Header: 알고리즘 정보 (HS256)
 * - Payload: 사용자 정보 및 메타데이터 (Claims)
 * - Signature: HMAC SHA-256 기반 디지털 서명
 *
 * 포함되는 표준 클레임:
 * - sub (subject): 사용자 이메일 (고유 식별자)
 * - iat (issued at): 토큰 발급 시간
 * - exp (expiration): 토큰 만료 시간
 *
 * 포함되는 커스텀 클레임:
 * - userId: 시스템 사용자 ID
 * - userType: 사용자 분류 (GENERAL, COMPANY, ADMIN)
 * - emailVerified: 이메일 인증 완료 여부
 * - companyEmailVerified: 기업 이메일 인증 완료 여부
 * - isAdmin: 관리자 권한 보유 여부
 *
 * 보안 고려사항:
 * - 환경 변수를 통한 비밀 키 관리
 * - 최소 256비트 키 길이 보장
 * - 토큰 만료 시간 엄격 관리
 * - 서명 검증을 통한 변조 방지
 * - 안전한 키 생성 및 관리 프로세스
 *
 * 토큰 생명주기:
 * 1. 로그인 시 액세스 토큰 + 리프레시 토큰 발급
 * 2. API 요청 시 액세스 토큰으로 인증
 * 3. 액세스 토큰 만료 시 리프레시 토큰으로 갱신
 * 4. 리프레시 토큰 만료 시 재로그인 필요
 *
 * 사용 사례:
 * - RESTful API 인증 및 권한 검증
 * - 무상태(Stateless) 세션 관리
 * - 마이크로서비스 간 인증 정보 전달
 * - 모바일 앱 및 SPA 인증 처리
 * - OAuth2 및 소셜 로그인 연동
 */
@Slf4j
@Service
public class JwtService {
    
    /** JWT 서명에 사용되는 비밀 키 (환경변수로 설정) */
    @Value("${JWT_SECRET:your-super-secret-jwt-key-should-be-at-least-256-bits-long-for-hs256}")
    private String secretKey;

    /** JWT 액세스 토큰의 만료 시간 (초 단위, 기본 24시간) */
    @Value("${JWT_EXPIRATION:86400}")
    private long jwtExpiration;

    /** 리프레시 토큰의 만료 시간 (7일, 밀리초 단위) */
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 days
    
    /**
     * 토큰에서 사용자 이름(이메일)을 추출합니다.
     * JWT의 subject 클레임에서 사용자 이메일을 반환합니다.
     *
     * @param token JWT 토큰 문자열
     * @return String 사용자 이메일 주소
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * 토큰에서 특정 클레임을 추출합니다.
     * 제네릭 메소드로 다양한 타입의 클레임을 추출할 수 있습니다.
     *
     * @param <T> 추출할 클래임의 타입
     * @param token JWT 토큰 문자열
     * @param claimsResolver 클레임에서 원하는 값을 추출하는 함수
     * @return T 추출된 클레임 값
     * @see Claims
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 사용자를 위한 기본 액세스 토큰을 생성합니다.
     * 추가 클레임 없이 기본 사용자 정보만으로 토큰을 생성합니다.
     *
     * @param user 토큰을 생성할 사용자
     * @return String 생성된 JWT 액세스 토큰
     * @see User
     */
    public String generateToken(User user) {
        return generateToken(new HashMap<>(), user);
    }
    
    /**
     * 추가 클레임을 포함한 액세스 토큰을 생성합니다.
     * 사용자 기본 정보 외에 추가적인 정보를 토큰에 포함할 수 있습니다.
     *
     * @param extraClaims 추가로 포함할 클레임 맵
     * @param user 토큰을 생성할 사용자
     * @return String 생성된 JWT 액세스 토큰
     * @see User
     */
    public String generateToken(Map<String, Object> extraClaims, User user) {
        return buildToken(extraClaims, user, jwtExpiration * 1000);
    }
    
    /**
     * 사용자를 위한 리프레시 토큰을 생성합니다.
     * 액세스 토큰이 만료되었을 때 새로운 액세스 토큰을 발급받는 데 사용합니다.
     * 리프레시 토큰은 7일간 유효합니다.
     *
     * @param user 리프레시 토큰을 생성할 사용자
     * @return String 생성된 JWT 리프레시 토큰
     * @see User
     */
    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user, REFRESH_TOKEN_EXPIRATION);
    }
    
    /**
     * JWT 토큰을 구축하는 핵심 메서드입니다.
     *
     * JWT의 구조 (Header.Payload.Signature)에 따라 토큰을 생성합니다:
     * - Header: 알고리즘 정보 (HS256)
     * - Payload: 사용자 정보와 토큰 메타데이터 (클레임)
     * - Signature: HMAC SHA-256으로 생성된 서명
     *
     * 포함되는 클레임:
     * - sub (subject): 사용자 이메일
     * - iat (issued at): 토큰 발급 시간
     * - exp (expiration): 토큰 만료 시간
     * - userId: 사용자 ID (커스텀 클레임)
     * - userType: 사용자 타입 (GENERAL, COMPANY, ADMIN)
     * - emailVerified: 이메일 인증 여부
     * - companyEmailVerified: 기업 이메일 인증 여부
     *
     * @param extraClaims 추가로 포함할 커스텀 클레임
     * @param user 토큰 대상 사용자
     * @param expiration 토큰 만료 시간 (밀리초)
     * @return String 완전히 구성된 JWT 토큰
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            User user,
            long expiration
    ) {
        // 기존 추가 클레임을 복사하고 사용자 정보 클레임 추가
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("userId", user.getId());
        claims.put("userType", user.getUserType().name());
        claims.put("emailVerified", user.getEmailVerified());
        claims.put("companyEmailVerified", user.getCompanyEmailVerified());
        claims.put("isAdmin", user.isAdmin());

        // JWT 빌더를 사용한 토큰 생성
        return Jwts
                .builder()
                .setClaims(claims)                                              // 페이로드에 클레임 설정
                .setSubject(user.getEmail())                                   // subject 클레임 (사용자 식별자)
                .setIssuedAt(new Date(System.currentTimeMillis()))            // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 만료 시간
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)           // HMAC SHA-256으로 서명
                .compact();                                                    // 최종 토큰 문자열 생성
    }
    
    /**
     * JWT 토큰의 유효성을 검증합니다.
     *
     * 검증 과정:
     * 1. 토큰 서명 검증 (내부적으로 extractUsername에서 수행)
     * 2. 토큰 만료 시간 확인
     * 3. 사용자 일치 확인 (일반 사용자의 경우)
     *
     * 관리자 토큰의 경우 user 매개변수가 null일 수 있으며,
     * 이 경우 만료 시간만 확인합니다.
     *
     * @param token 검증할 JWT 토큰
     * @param user 토큰과 매칭할 사용자 (관리자 토큰의 경우 null 가능)
     * @return boolean 토큰이 유효하면 true, 아니면 false
     */
    public boolean isTokenValid(String token, User user) {
        if (user == null) {
            // 관리자 토큰의 경우: 만료 시간만 확인
            return !isTokenExpired(token);
        }

        // 일반 사용자 토큰의 경우: 사용자 일치 + 만료 시간 확인
        final String username = extractUsername(token);
        return (username.equals(user.getEmail())) && !isTokenExpired(token);
    }

    /**
     * 관리자용 특별 토큰을 생성합니다.
     *
     * 일반 사용자 토큰과 달리 User 엔티티 없이 username만으로 토큰을 생성합니다.
     * 관리자 인증 시스템에서 사용되며, 추가 클레임을 통해 관리자 권한 정보를 포함할 수 있습니다.
     *
     * @param username 관리자 사용자명 (이메일)
     * @param extraClaims 관리자 권한 등 추가 정보
     * @return String 생성된 관리자 JWT 토큰
     */
    public String generateAdminToken(String username, Map<String, Object> extraClaims) {
        // 추가 클레임을 포함한 새로운 클레임 맵 생성
        Map<String, Object> claims = new HashMap<>(extraClaims);

        // 관리자 토큰 생성 (User 엔티티 정보 제외)
        return Jwts
                .builder()
                .setClaims(claims)                                              // 관리자 권한 클레임 설정
                .setSubject(username)                                          // 관리자 식별자
                .setIssuedAt(new Date(System.currentTimeMillis()))            // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration * 1000)) // 만료 시간
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)           // 동일한 서명 키 사용
                .compact();                                                    // 토큰 문자열 생성
    }
    
    /**
     * 토큰의 만료 여부를 확인합니다.
     *
     * 토큰의 'exp' (expiration) 클레임을 현재 시간과 비교하여 만료 여부를 결정합니다.
     * 만료된 토큰은 보안상 위험하므로 사용할 수 없습니다.
     *
     * @param token 확인할 JWT 토큰
     * @return boolean 만료되었으면 true, 아직 유효하면 false
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * 토큰에서 만료 시간을 추출합니다.
     *
     * JWT의 'exp' 클레임에서 만료 시간 정보를 가져옵니다.
     * 이 정보는 Unix 타임스탬프 형태로 저장되어 있습니다.
     *
     * @param token JWT 토큰
     * @return Date 토큰 만료 시간
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * JWT 토큰에서 모든 클레임을 추출합니다.
     *
     * JWT 파싱 과정:
     * 1. 토큰을 Header.Payload.Signature로 분리
     * 2. 서명 키로 Signature 검증
     * 3. 검증 성공 시 Payload에서 Claims 추출
     *
     * 서명 검증이 실패하면 예외가 발생합니다.
     *
     * @param token 파싱할 JWT 토큰
     * @return Claims 토큰의 모든 클레임 정보
     * @throws io.jsonwebtoken.JwtException 토큰이 유효하지 않은 경우
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()                  // JWT 파서 빌더 생성
                .setSigningKey(getSignInKey())   // 서명 검증용 키 설정
                .build()                          // 파서 빌드
                .parseClaimsJws(token)           // JWS (서명된 JWT) 파싱
                .getBody();                       // 페이로드 (Claims) 추출
    }
    
    /**
     * JWT 서명에 사용할 HMAC 키를 생성합니다.
     *
     * 복잡한 키 변환 과정:
     * 1. 문자열 secretKey를 byte 배열로 변환
     * 2. Base64로 인코딩하여 안전한 형태로 변환
     * 3. 다시 Base64 디코딩하여 바이트 배열 복원
     * 4. HMAC SHA-256용 키 객체 생성
     *
     * 이 과정은 키의 길이와 형식을 HMAC SHA-256 요구사항에 맞게 조정합니다.
     * HMAC SHA-256은 최소 256비트(32바이트) 키를 필요로 합니다.
     *
     * @return Key HMAC SHA-256 서명용 키 객체
     */
    private Key getSignInKey() {
        // 1. 문자열 -> 바이트 배열 -> Base64 인코딩 -> Base64 디코딩
        byte[] keyBytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(secretKey.getBytes()));

        // 2. HMAC SHA-256용 키 객체 생성
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 토큰에서 사용자 ID를 추출합니다.
     *
     * 커스텀 클레임 'userId'에서 사용자 ID를 가져옵니다.
     * 안전한 타입 변환을 위해 Number 타입 검사를 수행합니다.
     *
     * @param token JWT 토큰
     * @return Long 사용자 ID
     * @throws IllegalArgumentException 토큰에 유효한 userId가 없는 경우
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get("userId");

        // 안전한 타입 변환: JSON에서 숫자는 다양한 Number 타입으로 올 수 있음
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        throw new IllegalArgumentException("Invalid userId in token");
    }
    
    /**
     * 토큰에서 사용자 타입을 추출합니다.
     *
     * 커스텀 클레임 'userType'에서 사용자 타입 정보를 가져옵니다.
     * 반환되는 값은 UserType enum의 문자열 형태입니다.
     *
     * @param token JWT 토큰
     * @return String 사용자 타입 (GENERAL, COMPANY, ADMIN)
     */
    public String extractUserType(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userType", String.class);
    }
    
    /**
     * 토큰에서 이메일 인증 상태를 추출합니다.
     *
     * 커스텀 클레임 'emailVerified'에서 이메일 인증 여부를 가져옵니다.
     * 이 정보는 사용자가 이메일 인증을 완료했는지 확인하는 데 사용됩니다.
     *
     * @param token JWT 토큰
     * @return Boolean 이메일 인증 여부 (true: 인증완료, false: 미인증)
     */
    public Boolean extractEmailVerified(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("emailVerified", Boolean.class);
    }
    
    /**
     * 토큰에서 기업 이메일 인증 상태를 추출합니다.
     *
     * 커스텀 클레임 'companyEmailVerified'에서 기업 이메일 인증 여부를 가져옵니다.
     * 기업 사용자의 경우 대결제 등 민감한 작업에 사용됩니다.
     *
     * @param token JWT 토큰
     * @return Boolean 기업 이메일 인증 여부 (true: 인증완료, false: 미인증)
     */
    public Boolean extractCompanyEmailVerified(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("companyEmailVerified", Boolean.class);
    }
    
    /**
     * 토큰에서 사용자 이메일을 추출합니다.
     *
     * JWT의 subject 클레임에 저장된 사용자 이메일을 반환합니다.
     * extractUsername과 동일한 기능을 수행하지만 명시적인 메서드명으로 제공합니다.
     *
     * @param token JWT 토큰
     * @return String 사용자 이메일 주소
     * @see #extractUsername(String)
     */
    public String extractEmail(String token) {
        return extractUsername(token);
    }
}