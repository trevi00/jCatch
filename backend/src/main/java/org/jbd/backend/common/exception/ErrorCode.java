package org.jbd.backend.common.exception;

/**
 * 애플리케이션 전역에서 사용되는 에러 코드 정의 enum입니다.
 *
 * 체계적인 에러 관리를 위해 도메인별로 코드를 구분하고,
 * 각 에러마다 고유 코드, 한글 메시지, HTTP 상태 코드를 정의합니다.
 *
 * 에러 코드 체계:
 * - 1000번대: 공통 에러 (인증, 권한, 유효성 검증 등)
 * - 2000번대: 회원 관련 에러
 * - 3000번대: 인증/인가 관련 에러
 * - 4000번대: 채용공고 관련 에러
 * - 5000번대: AI 서비스 관련 에러
 * - 6000번대: 파일 업로드 관련 에러
 * - 7000번대: 외부 API 관련 에러
 * - 8000번대: 프로필 관련 에러
 * - 8500번대: 회사 관련 에러
 * - 9000번대: 비즈니스 로직 관련 에러
 *
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 * @see BusinessException
 * @see GlobalExceptionHandler
 */
public enum ErrorCode {
    
    // 공통 에러 (1000번대)
    INTERNAL_SERVER_ERROR("COMMON_001", "내부 서버 오류가 발생했습니다.", 500),
    INVALID_REQUEST("COMMON_002", "잘못된 요청입니다.", 400),
    UNAUTHORIZED("COMMON_003", "인증이 필요합니다.", 401),
    FORBIDDEN("COMMON_004", "접근 권한이 없습니다.", 403),
    NOT_FOUND("COMMON_005", "요청한 리소스를 찾을 수 없습니다.", 404),
    METHOD_NOT_ALLOWED("COMMON_006", "허용되지 않는 HTTP 메서드입니다.", 405),
    VALIDATION_ERROR("COMMON_007", "입력값 검증에 실패했습니다.", 400),
    
    // 회원 관련 에러 (2000번대)
    USER_NOT_FOUND("USER_001", "사용자를 찾을 수 없습니다.", 404),
    USER_ALREADY_EXISTS("USER_002", "이미 존재하는 사용자입니다.", 409),
    INVALID_EMAIL_FORMAT("USER_003", "이메일 형식이 올바르지 않습니다.", 400),
    INVALID_PASSWORD("USER_004", "비밀번호가 올바르지 않습니다.", 400),
    ACCOUNT_DISABLED("USER_005", "비활성화된 계정입니다.", 403),
    ACCOUNT_LOCKED("USER_006", "잠긴 계정입니다.", 423),
    EMAIL_ALREADY_VERIFIED("USER_007", "이미 인증된 이메일입니다.", 409),
    EMAIL_VERIFICATION_FAILED("USER_008", "이메일 인증에 실패했습니다.", 400),
    USER_PROFILE_NOT_FOUND("USER_009", "사용자 프로필을 찾을 수 없습니다.", 404),
    
    // 인증/인가 관련 에러 (3000번대)
    INVALID_TOKEN("AUTH_001", "유효하지 않은 토큰입니다.", 401),
    EXPIRED_TOKEN("AUTH_002", "만료된 토큰입니다.", 401),
    TOKEN_NOT_FOUND("AUTH_003", "토큰이 존재하지 않습니다.", 401),
    INVALID_REFRESH_TOKEN("AUTH_004", "유효하지 않은 리프레시 토큰입니다.", 401),
    OAUTH_AUTHENTICATION_FAILED("AUTH_005", "OAuth 인증에 실패했습니다.", 401),
    OAUTH2_AUTHENTICATION_FAILED("AUTH_006", "OAuth2 인증에 실패했습니다.", 401),
    INVALID_OAUTH2_TOKEN("AUTH_007", "유효하지 않은 OAuth2 토큰입니다.", 401),
    EXPIRED_OAUTH2_TOKEN("AUTH_008", "만료된 OAuth2 토큰입니다.", 401),
    INVALID_OAUTH2_USER_INFO("AUTH_009", "유효하지 않은 OAuth2 사용자 정보입니다.", 400),
    
    // 채용공고 관련 에러 (4000번대)
    JOB_POSTING_NOT_FOUND("JOB_001", "채용공고를 찾을 수 없습니다.", 404),
    JOB_POSTING_ACCESS_DENIED("JOB_002", "채용공고에 대한 접근 권한이 없습니다.", 403),
    JOB_POSTING_ALREADY_CLOSED("JOB_003", "이미 마감된 채용공고입니다.", 409),
    
    // AI 서비스 관련 에러 (5000번대)
    AI_SERVICE_UNAVAILABLE("AI_001", "AI 서비스를 이용할 수 없습니다.", 503),
    AI_SERVICE_TIMEOUT("AI_002", "AI 서비스 요청이 시간 초과되었습니다.", 504),
    INVALID_AI_REQUEST("AI_003", "잘못된 AI 요청입니다.", 400),
    
    // 파일 업로드 관련 에러 (6000번대)
    FILE_UPLOAD_FAILED("FILE_001", "파일 업로드에 실패했습니다.", 500),
    FILE_SIZE_EXCEEDED("FILE_002", "파일 크기가 제한을 초과했습니다.", 413),
    INVALID_FILE_TYPE("FILE_003", "지원하지 않는 파일 형식입니다.", 400),
    FILE_NOT_FOUND("FILE_004", "파일을 찾을 수 없습니다.", 404),
    
    // 외부 API 관련 에러 (7000번대)
    EXTERNAL_API_ERROR("API_001", "외부 API 호출 중 오류가 발생했습니다.", 502),
    EMAIL_SEND_FAILED("API_002", "이메일 전송에 실패했습니다.", 500),
    
    // 프로필 관련 에러 (8000번대)
    SKILL_ALREADY_EXISTS("SKILL_001", "이미 존재하는 스킬입니다.", 409),
    SKILL_NOT_FOUND("SKILL_002", "스킬을 찾을 수 없습니다.", 404),
    EDUCATION_NOT_FOUND("EDU_001", "교육 이력을 찾을 수 없습니다.", 404),
    CERTIFICATION_NOT_FOUND("CERT_001", "자격증을 찾을 수 없습니다.", 404),
    PORTFOLIO_NOT_FOUND("PORT_001", "포트폴리오를 찾을 수 없습니다.", 404),
    EXPERIENCE_NOT_FOUND("EXP_001", "경력을 찾을 수 없습니다.", 404),
    
    // 회사 관련 에러 (8500번대)
    COMPANY_NOT_FOUND("COMPANY_001", "회사 정보를 찾을 수 없습니다.", 404),
    COMPANY_PROFILE_ALREADY_EXISTS("COMPANY_002", "이미 기업 프로필이 존재합니다.", 409),
    COMPANY_ACCESS_DENIED("COMPANY_003", "기업 사용자만 접근할 수 있습니다.", 403),
    DUPLICATE_BUSINESS_NUMBER("COMPANY_004", "이미 사용 중인 사업자번호입니다.", 409),
    
    // 비즈니스 로직 관련 에러 (9000번대)
    INVALID_APPLICATION_STATUS("BIZ_001", "잘못된 지원 상태입니다.", 400),
    DUPLICATE_APPLICATION("BIZ_002", "이미 지원한 채용공고입니다.", 409),
    CERTIFICATE_REQUEST_LIMIT_EXCEEDED("BIZ_003", "증명서 요청 한도를 초과했습니다.", 429);
    
    /** 에러 코드 (예: "USER_001") */
    private final String code;

    /** 에러 메시지 (한글) */
    private final String message;

    /** HTTP 상태 코드 */
    private final int status;

    /**
     * ErrorCode 생성자
     *
     * @param code 에러 코드
     * @param message 에러 메시지
     * @param status HTTP 상태 코드
     */
    ErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
    
    /**
     * 에러 코드를 반환합니다.
     *
     * @return 에러 코드 (예: "USER_001")
     */
    public String getCode() {
        return code;
    }

    /**
     * 에러 메시지를 반환합니다.
     *
     * @return 에러 메시지 (한글)
     */
    public String getMessage() {
        return message;
    }

    /**
     * HTTP 상태 코드를 반환합니다.
     *
     * @return HTTP 상태 코드
     */
    public int getStatus() {
        return status;
    }
}