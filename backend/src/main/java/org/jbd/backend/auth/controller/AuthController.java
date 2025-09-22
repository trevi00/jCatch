package org.jbd.backend.auth.controller;

import jakarta.validation.Valid;
import org.jbd.backend.auth.dto.AuthenticationRequest;
import org.jbd.backend.auth.dto.AuthenticationResponse;
import org.jbd.backend.auth.service.AuthenticationService;
import org.jbd.backend.common.dto.ApiResponse;
import org.jbd.backend.user.dto.UserRegistrationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 인증 관련 REST API 컨트롤러
 *
 * 잡았다 플랫폼의 사용자 인증 및 권한 관리를 담당하는 REST API 컨트롤러입니다.
 * 회원가입, 로그인, 토큰 갱신, 로그아웃, 이메일 인증 등의 핵심 인증 기능을 제공합니다.
 * JWT 토큰 기반의 보안 인증을 사용하며, OAuth2 소셜 로그인과도 통합 연동됩니다.
 *
 * 주요 기능:
 * - 사용자 회원가입 및 자동 로그인 처리
 * - 이메일/비밀번호 기반 로그인 인증
 * - JWT 액세스 토큰 및 리프레시 토큰 발급
 * - 토큰 갱신 및 로그아웃 처리
 * - 이메일 인증 링크 발송 및 인증 처리
 * - 사용자 타입별 차별화된 인증 프로세스
 *
 * 보안 고려사항:
 * - 비밀번호 암호화 및 HTTPS 통신 강제
 * - JWT 토큰 만료 시간 및 리프레시 정책 관리
 * - Rate Limiting으로 무차별 대입 공격 방지
 * - CSRF 및 XSS 공격 방어
 * - 로그인 시도 로깅 및 감시
 *
 * API 엔드포인트:
 * - POST /auth/register: 사용자 회원가입
 * - POST /auth/login: 사용자 로그인
 * - POST /auth/refresh: 토큰 갱신
 * - POST /auth/logout: 사용자 로그아웃
 * - POST /auth/verify-email: 이메일 인증 처리
 * - POST /auth/resend-verification: 인증 이메일 재전송
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "인증 관리", description = "사용자 인증 및 권한 관리 API")
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * AuthController 생성자
     *
     * 인증 서비스에 대한 의존성을 주입받아 컨트롤러를 초기화합니다.
     *
     * @param authenticationService 인증 및 사용자 관리 비즈니스 로직을 처리하는 서비스
     */
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    /**
     * 새로운 사용자를 시스템에 등록하고 자동 로그인 처리를 수행합니다.
     *
     * 사용자가 제공한 회원가입 정보를 검증하고 새로운 계정을 생성합니다.
     * 회원가입과 동시에 자동으로 로그인 처리되어 JWT 토큰을 즉시 발급하므로,
     * 사용자는 별도의 로그인 과정 없이 바로 서비스를 이용할 수 있습니다.
     *
     * 처리 과정:
     * 1. 입력 데이터 유효성 검증 (이메일 형식, 비밀번호 강도 등)
     * 2. 이메일 중복 확인 및 사용 가능 여부 검증
     * 3. 비밀번호 해시화 및 보안 처리
     * 4. 사용자 계정 생성 및 데이터베이스 저장
     * 5. JWT 토큰 발급 및 인증 응답 생성
     * 6. 이메일 인증 링크 발송 (백그라운드 처리)
     *
     * 사용자 타입별 처리:
     * - GENERAL: 일반 사용자(구직자) 계정 생성
     * - COMPANY: 기업 사용자 계정 생성 (추후 기업 정보 등록 필요)
     * - ADMIN: 관리자 계정 (별도 승인 프로세스 필요)
     *
     * 보안 및 검증 규칙:
     * - 이메일 주소는 전체 시스템에서 유일해야 함
     * - 비밀번호는 최소 8자 이상, 100자 이하
     * - 사용자 이름은 2자 이상, 50자 이하
     * - XSS 및 SQL 인젝션 방지를 위한 입력값 검증
     *
     * @param request 회원가입 요청 데이터 객체
     *                - email: 사용자 이메일 주소 (로그인 ID, 필수, 유일성 검증)
     *                - password: 사용자 비밀번호 (필수, 8-100자)
     *                - name: 사용자 이름 (필수, 2-50자)
     *                - userType: 사용자 분류 (GENERAL/COMPANY, 기본값: GENERAL)
     *                - phoneNumber: 연락처 전화번호 (선택사항)
     * @return 회원가입 성공 시 JWT 토큰과 사용자 정보가 포함된 API 응답
     *         - success: 처리 성공 여부 (true)
     *         - message: "회원가입이 완료되었습니다." 메시지
     *         - data: 인증 토큰 및 사용자 기본 정보
     * @throws IllegalArgumentException 이미 존재하는 이메일로 가입 시도 시
     * @throws ValidationException 입력 데이터 유효성 검증 실패 시
     * @throws SecurityException 비밀번호 보안 요구사항 미충족 시
     * @apiNote POST /api/auth/register 엔드포인트로 접근
     *          Content-Type: application/json 헤더 필수
     *          회원가입 성공 시 자동으로 로그인 상태가 되므로 추가 로그인 불필요
     */
    @PostMapping("/register")
    @Operation(
        summary = "사용자 회원가입",
        description = "새로운 사용자를 등록하고 자동으로 로그인 처리하여 JWT 토큰을 반환합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "회원가입 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "message": "회원가입이 완료되었습니다.",
                      "data": {
                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "userType": "GENERAL",
                        "expiresIn": 86400000
                      }
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "message": "이미 존재하는 이메일입니다.",
                      "errorCode": "DUPLICATE_EMAIL"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(
            @Parameter(
                description = "회원가입 요청 데이터",
                required = true,
                example = """
                {
                  "email": "user@example.com",
                  "password": "password123",
                  "name": "홍길동",
                  "userType": "GENERAL"
                }
                """
            )
            @Valid @RequestBody UserRegistrationDto request
    ) {
        AuthenticationResponse response = authenticationService.register(request);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    /**
     * 사용자 인증 정보를 검증하고 로그인 처리를 수행합니다.
     *
     * 사용자가 제공한 이메일과 비밀번호를 검증하여 시스템 접근 권한을 부여하고,
     * 성공적인 인증 시 JWT 액세스 토큰과 리프레시 토큰을 발급합니다.
     * 발급된 토큰을 통해 사용자는 보호된 API 엔드포인트에 접근할 수 있습니다.
     *
     * 인증 처리 과정:
     * 1. 입력된 이메일 주소로 사용자 계정 조회
     * 2. 계정 상태 확인 (활성화 여부, 잠금 상태 등)
     * 3. 비밀번호 해시 검증 및 일치 여부 확인
     * 4. 로그인 성공 시 사용자 세션 정보 업데이트
     * 5. JWT 액세스 토큰 및 리프레시 토큰 생성
     * 6. 마지막 로그인 시간 기록 및 보안 로그 생성
     *
     * 보안 검증 항목:
     * - 이메일 주소 존재 여부 및 형식 검증
     * - 비밀번호 일치 여부 확인 (해시 비교)
     * - 계정 활성화 상태 및 잠금 여부 확인
     * - 로그인 시도 횟수 제한 (무차별 대입 공격 방지)
     * - IP 주소 기반 접근 제한 (선택적)
     *
     * 사용자 타입별 인증 처리:
     * - GENERAL: 일반 사용자 로그인, 구직 관련 기능 접근
     * - COMPANY: 기업 사용자 로그인, 채용 관련 기능 접근
     * - ADMIN: 관리자 로그인, 시스템 관리 기능 접근
     *
     * 토큰 발급 정책:
     * - 액세스 토큰 유효기간: 24시간
     * - 리프레시 토큰 유효기간: 7일
     * - 토큰에는 사용자 ID, 이메일, 사용자 타입 정보 포함
     *
     * @param request 로그인 인증 요청 데이터 객체
     *                - email: 등록된 사용자 이메일 주소 (필수)
     *                - password: 사용자 계정 비밀번호 (필수)
     * @return 로그인 성공 시 JWT 토큰과 사용자 인증 정보가 포함된 API 응답
     *         - success: 인증 처리 성공 여부 (true)
     *         - message: "로그인이 완료되었습니다." 메시지
     *         - data: JWT 토큰, 리프레시 토큰, 사용자 타입, 토큰 만료 시간
     * @throws AuthenticationException 이메일 또는 비밀번호가 올바르지 않을 시
     * @throws AccountLockedException 계정이 잠겨있거나 비활성화된 경우
     * @throws TooManyAttemptsException 로그인 시도 횟수가 제한을 초과한 경우
     * @apiNote POST /api/auth/login 엔드포인트로 접근
     *          Content-Type: application/json 헤더 필수
     *          성공 시 발급된 토큰을 Authorization 헤더에 Bearer 토큰으로 사용
     */
    @PostMapping("/login")
    @Operation(
        summary = "사용자 로그인",
        description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "message": "로그인이 완료되었습니다.",
                      "data": {
                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "userType": "GENERAL",
                        "expiresIn": 86400000
                      }
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
                      "errorCode": "INVALID_CREDENTIALS"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(
            @Parameter(
                description = "로그인 요청 데이터",
                required = true,
                example = """
                {
                  "email": "user@example.com",
                  "password": "password123"
                }
                """
            )
            @Valid @RequestBody AuthenticationRequest request
    ) {
        AuthenticationResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
    }

    /**
     * 리프레시 토큰을 사용하여 만료된 액세스 토큰을 갱신합니다.
     *
     * 클라이언트가 보유한 유효한 리프레시 토큰을 검증하고,
     * 새로운 액세스 토큰과 리프레시 토큰을 발급하여 사용자의 세션을 연장합니다.
     * 이를 통해 사용자는 재로그인 없이 지속적으로 서비스를 이용할 수 있습니다.
     *
     * 토큰 갱신 처리 과정:
     * 1. Authorization 헤더에서 리프레시 토큰 추출
     * 2. 리프레시 토큰의 유효성 및 만료 여부 검증
     * 3. 토큰에 포함된 사용자 정보로 계정 상태 확인
     * 4. 새로운 액세스 토큰 및 리프레시 토큰 생성
     * 5. 기존 리프레시 토큰 무효화 (보안 강화)
     * 6. 갱신된 토큰 정보를 클라이언트에 응답
     *
     * 보안 검증 항목:
     * - 리프레시 토큰 서명 및 구조 유효성 검증
     * - 토큰 만료 시간 및 발급 시간 확인
     * - 토큰에 포함된 사용자 계정 활성화 상태 검증
     * - 토큰이 이미 사용되거나 무효화되지 않았는지 확인
     * - 토큰 재사용 공격(Token Replay Attack) 방지
     *
     * 토큰 갱신 정책:
     * - 새로운 액세스 토큰 유효기간: 24시간
     * - 새로운 리프레시 토큰 유효기간: 7일
     * - 기존 리프레시 토큰은 즉시 무효화
     * - 갱신 시 사용자의 최신 권한 및 상태 정보 반영
     *
     * 오류 처리 시나리오:
     * - 리프레시 토큰이 만료된 경우: 재로그인 필요
     * - 토큰이 변조되거나 유효하지 않은 경우: 인증 오류
     * - 사용자 계정이 비활성화된 경우: 접근 거부
     * - 토큰이 이미 사용된 경우: 보안 위반으로 간주
     *
     * @param authHeader HTTP Authorization 헤더 값
     *                   "Bearer {refresh_token}" 형식으로 전송되어야 함
     *                   리프레시 토큰은 로그인 시 발급받은 유효한 토큰이어야 함
     * @return 토큰 갱신 성공 시 새로운 JWT 토큰 정보가 포함된 API 응답
     *         - success: 토큰 갱신 성공 여부 (true)
     *         - message: "토큰이 갱신되었습니다." 메시지
     *         - data: 새로운 액세스 토큰, 리프레시 토큰, 사용자 타입, 만료 시간
     * @throws InvalidTokenException 리프레시 토큰이 유효하지 않거나 변조된 경우
     * @throws ExpiredTokenException 리프레시 토큰이 만료된 경우
     * @throws SecurityException 토큰 재사용 시도나 보안 위반이 감지된 경우
     * @throws AccountDisabledException 토큰의 사용자 계정이 비활성화된 경우
     * @apiNote POST /api/auth/refresh-token 엔드포인트로 접근
     *          Authorization: Bearer {refresh_token} 헤더 필수
     *          갱신된 토큰으로 기존 토큰을 교체하여 사용해야 함
     */
    @PostMapping("/refresh-token")
    @Operation(
        summary = "토큰 갱신",
        description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토큰 갱신 성공",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "message": "토큰이 갱신되었습니다.",
                      "data": {
                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "userType": "GENERAL",
                        "expiresIn": 86400000
                      }
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "유효하지 않은 Refresh Token"
        )
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @Parameter(
                description = "Bearer {refresh_token} 형식의 Authorization 헤더",
                required = true,
                example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader("Authorization") String authHeader
    ) {
        AuthenticationResponse response = authenticationService.refreshTokenFromHeader(authHeader);
        return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", response));
    }

    /**
     * 사용자 로그아웃을 처리하고 관련 토큰을 안전하게 무효화합니다.
     *
     * 사용자의 현재 세션을 종료하고, 보유하고 있는 JWT 토큰을 무효화하여
     * 향후 해당 토큰을 통한 API 접근을 차단합니다.
     * 클라이언트 측에서도 토큰을 삭제하여 완전한 로그아웃을 보장합니다.
     *
     * 로그아웃 처리 과정:
     * 1. Authorization 헤더에서 액세스 토큰 추출 (선택적)
     * 2. 토큰이 제공된 경우 토큰 유효성 검증
     * 3. 토큰을 블랙리스트에 추가하여 무효화
     * 4. 관련된 리프레시 토큰도 함께 무효화
     * 5. 사용자 세션 정보 정리 및 로그아웃 로그 기록
     * 6. 성공 응답을 클라이언트에 전송
     *
     * 보안 처리 항목:
     * - 액세스 토큰 및 리프레시 토큰 즉시 무효화
     * - 토큰 블랙리스트 등록으로 재사용 방지
     * - 세션 관련 임시 데이터 및 캐시 정리
     * - 로그아웃 시간 및 IP 주소 로깅
     * - 다중 디바이스 로그인 시 특정 세션만 종료
     *
     * 토큰 무효화 정책:
     * - 제공된 액세스 토큰을 블랙리스트에 추가
     * - 해당 사용자의 모든 리프레시 토큰 무효화 (선택적)
     * - 토큰 만료 시간까지 블랙리스트 유지
     * - 무효화된 토큰으로 API 접근 시 401 Unauthorized 응답
     *
     * 클라이언트 권장사항:
     * - 로그아웃 성공 후 로컬 저장소의 모든 토큰 삭제
     * - 사용자를 로그인 페이지로 리다이렉트
     * - 자동 로그인 설정이 있는 경우 해제
     * - 민감한 사용자 데이터 캐시 정리
     *
     * @param authHeader HTTP Authorization 헤더 값 (선택적)
     *                   "Bearer {access_token}" 형식으로 제공
     *                   토큰이 제공되지 않아도 로그아웃 처리는 성공적으로 완료됨
     *                   제공된 경우 해당 토큰을 무효화하여 보안 강화
     * @return 로그아웃 처리 완료를 알리는 API 응답
     *         - success: 로그아웃 처리 성공 여부 (항상 true)
     *         - message: "로그아웃이 완료되었습니다." 메시지
     *         - data: null (로그아웃 응답에는 추가 데이터 없음)
     * @apiNote POST /api/auth/logout 엔드포인트로 접근
     *          Authorization 헤더는 선택사항이지만 제공 시 보안 강화
     *          로그아웃 후 클라이언트에서 모든 인증 정보 삭제 필요
     *          네트워크 오류 시에도 클라이언트 측 토큰 삭제 권장
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authenticationService.logout(token);
        }

        return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다."));
    }
}