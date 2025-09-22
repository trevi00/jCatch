package org.jbd.backend.common.exception;

/**
 * 비즈니스 로직 수행 중 발생하는 예외를 처리하는 클래스입니다.
 *
 * 애플리케이션의 비즈니스 규칙 위반이나 도메인 로직 처리 중 발생하는
 * 예상 가능한 예외 상황들을 표현합니다.
 *
 * 사용 예시:
 * - 사용자가 이미 지원한 채용공고에 다시 지원하려는 경우
 * - 권한이 없는 리소스에 접근하려는 경우
 * - 비즈니스 규칙에 맞지 않는 데이터 입력 시
 * - 도메인 엔티티의 상태 변경이 불가능한 경우
 *
 * 특징:
 * - CustomException을 상속하여 일관된 예외 처리 구조 제공
 * - ErrorCode를 통해 구체적인 에러 정보 전달
 * - GlobalExceptionHandler에서 자동으로 적절한 HTTP 응답으로 변환
 * - 클라이언트에게 명확한 오류 정보 제공
 *
 * @see CustomException
 * @see ErrorCode
 * @see GlobalExceptionHandler
 */
public class BusinessException extends CustomException {

    /**
     * ErrorCode만을 사용하여 비즈니스 예외를 생성합니다.
     *
     * @param errorCode 비즈니스 로직 위반을 나타내는 에러 코드
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 커스텀 메시지로 비즈니스 예외를 생성합니다.
     *
     * @param errorCode 비즈니스 로직 위반을 나타내는 에러 코드
     * @param message 사용자 정의 예외 메시지
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * ErrorCode, 메시지, 추가 데이터와 함께 비즈니스 예외를 생성합니다.
     *
     * @param errorCode 비즈니스 로직 위반을 나타내는 에러 코드
     * @param message 사용자 정의 예외 메시지
     * @param data 예외와 함께 전달할 추가 데이터 객체
     */
    public BusinessException(ErrorCode errorCode, String message, Object data) {
        super(errorCode, message, data);
    }
}