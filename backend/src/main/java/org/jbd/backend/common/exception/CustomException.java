package org.jbd.backend.common.exception;

/**
 * 애플리케이션 전역에서 사용되는 커스텀 예외의 최상위 클래스입니다.
 *
 * RuntimeException을 상속하여 체크되지 않는 예외로 구현되었으며,
 * 모든 비즈니스 예외의 기본 구조를 제공합니다.
 *
 * 주요 기능:
 * - ErrorCode 기반의 구조화된 예외 처리
 * - 예외 발생 시 추가 데이터 전달 지원
 * - HTTP 상태 코드 및 에러 코드 자동 매핑
 * - 다양한 생성자를 통한 유연한 예외 생성
 *
 * 사용 패턴:
 * - 직접 사용보다는 BusinessException 등의 구체적인 예외 클래스 상속용
 * - 예외 발생 시 ErrorCode를 통한 일관된 에러 정보 제공
 * - 선택적으로 추가 데이터 객체를 포함하여 상세한 에러 정보 전달
 *
 * @see ErrorCode
 * @see BusinessException
 * @see GlobalExceptionHandler
 */
public class CustomException extends RuntimeException {
    
    /** 예외와 연관된 에러 코드 (HTTP 상태 코드, 메시지 포함) */
    private final ErrorCode errorCode;

    /** 예외 발생 시 함께 전달할 추가 데이터 객체 (선택적) */
    private final Object data;

    /**
     * ErrorCode만을 사용하여 예외를 생성합니다.
     *
     * @param errorCode 예외 유형을 정의하는 에러 코드
     */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = null;
    }

    /**
     * ErrorCode와 커스텀 메시지로 예외를 생성합니다.
     *
     * @param errorCode 예외 유형을 정의하는 에러 코드
     * @param message 사용자 정의 예외 메시지
     */
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.data = null;
    }

    /**
     * ErrorCode, 메시지, 추가 데이터와 함께 예외를 생성합니다.
     *
     * @param errorCode 예외 유형을 정의하는 에러 코드
     * @param message 사용자 정의 예외 메시지
     * @param data 예외와 함께 전달할 추가 데이터 객체
     */
    public CustomException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    /**
     * ErrorCode, 메시지, 원인 예외와 함께 예외를 생성합니다.
     *
     * @param errorCode 예외 유형을 정의하는 에러 코드
     * @param message 사용자 정의 예외 메시지
     * @param cause 이 예외의 원인이 된 예외
     */
    public CustomException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.data = null;
    }

    /**
     * ErrorCode와 원인 예외로 예외를 생성합니다.
     *
     * @param errorCode 예외 유형을 정의하는 에러 코드
     * @param cause 이 예외의 원인이 된 예외
     */
    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.data = null;
    }
    
    /**
     * 이 예외와 연관된 ErrorCode를 반환합니다.
     *
     * @return 예외의 ErrorCode 객체
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 예외와 함께 전달된 추가 데이터 객체를 반환합니다.
     *
     * @return 추가 데이터 객체 (없는 경우 null)
     */
    public Object getData() {
        return data;
    }

    /**
     * ErrorCode에서 정의된 HTTP 상태 코드를 반환합니다.
     *
     * @return HTTP 상태 코드 (예: 400, 404, 500)
     */
    public int getStatus() {
        return errorCode.getStatus();
    }

    /**
     * ErrorCode에서 정의된 에러 코드 문자열을 반환합니다.
     *
     * @return 에러 코드 문자열 (예: "USER_001", "AUTH_002")
     */
    public String getCode() {
        return errorCode.getCode();
    }
}