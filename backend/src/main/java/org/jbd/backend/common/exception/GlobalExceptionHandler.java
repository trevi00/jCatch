package org.jbd.backend.common.exception;

import org.jbd.backend.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 애플리케이션 전역에서 발생하는 예외를 처리하는 글로벌 예외 핸들러입니다.
 *
 * Spring의 @RestControllerAdvice를 사용하여 모든 컨트롤러에서 발생하는
 * 예외를 중앙 집중식으로 처리하고, 일관된 API 응답 형태로 변환합니다.
 *
 * 처리하는 예외 유형:
 * - BusinessException: 비즈니스 로직 위반 예외
 * - ResourceNotFoundException: 리소스 조회 실패 예외
 * - MethodArgumentNotValidException: 입력값 검증 실패 예외
 * - IllegalArgumentException: 잘못된 인자 전달 예외
 * - Exception: 기타 모든 예상치 못한 예외
 *
 * 응답 형태:
 * - ApiResponse<T>: 성공/실패 상태와 메시지를 포함한 표준 응답
 * - ErrorResponse: 상세한 에러 정보를 포함한 에러 전용 응답
 *
 * 주요 기능:
 * - HTTP 상태 코드 자동 매핑
 * - 에러 코드 기반 구조화된 응답
 * - 입력값 검증 오류 메시지 수집 및 반환
 * - 예외 발생 시간 기록
 * - 보안을 위한 내부 예외 정보 은닉
 *
 * @see BusinessException
 * @see ApiResponse
 * @see ErrorResponse
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BusinessException을 처리하여 구조화된 API 응답을 반환합니다.
     *
     * 비즈니스 로직 위반 시 발생하는 예외를 처리하며,
     * ErrorCode에 정의된 HTTP 상태 코드와 에러 메시지를 사용합니다.
     *
     * @param ex 처리할 BusinessException 객체
     * @return ApiResponse 형태의 에러 응답과 해당 HTTP 상태 코드
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), ex.getErrorCode().getCode());
        return new ResponseEntity<>(response, HttpStatus.valueOf(ex.getStatus()));
    }

    /**
     * ResourceNotFoundException을 처리하여 404 Not Found 응답을 반환합니다.
     *
     * 요청된 리소스를 찾을 수 없을 때 발생하는 예외를 처리합니다.
     *
     * @param ex 처리할 ResourceNotFoundException 객체
     * @return ErrorResponse 형태의 404 에러 응답
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * IllegalArgumentException을 처리하여 400 Bad Request 응답을 반환합니다.
     *
     * 메서드에 잘못된 인자가 전달되었을 때 발생하는 예외를 처리합니다.
     *
     * @param ex 처리할 IllegalArgumentException 객체
     * @return ErrorResponse 형태의 400 에러 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Bean Validation 실패 예외를 처리하여 구조화된 검증 오류 응답을 반환합니다.
     *
     * @Valid 어노테이션을 통한 입력값 검증 실패 시 발생하는 예외를 처리하며,
     * 모든 필드별 검증 오류를 수집하여 클라이언트에게 전달합니다.
     *
     * @param ex 처리할 MethodArgumentNotValidException 객체
     * @return ApiResponse 형태의 검증 오류 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage()));

        String errorMessage = errors.values().stream()
                .findFirst()
                .orElse("Invalid input parameters");

        ApiResponse<Object> response = ApiResponse.error(errorMessage, "VALIDATION_ERROR");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 예상하지 못한 모든 예외를 처리하여 500 Internal Server Error 응답을 반환합니다.
     *
     * 구체적으로 처리되지 않은 모든 예외에 대한 기본 핸들러로,
     * 보안을 위해 내부 예외 정보를 숨기고 일반적인 오류 메시지를 반환합니다.
     *
     * @param ex 처리할 Exception 객체
     * @return ErrorResponse 형태의 500 에러 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 에러 응답을 위한 데이터 전송 객체입니다.
     *
     * 예외 발생 시 클라이언트에게 전달되는 상세한 에러 정보를 포함하며,
     * Builder 패턴을 사용하여 유연한 객체 생성을 지원합니다.
     *
     * 포함 정보:
     * - timestamp: 에러 발생 시간
     * - status: HTTP 상태 코드
     * - error: HTTP 상태 메시지
     * - message: 구체적인 에러 메시지
     * - validationErrors: 입력값 검증 에러 맵 (선택적)
     */
    public static class ErrorResponse {
        /** 에러 발생 시간 */
        private LocalDateTime timestamp;

        /** HTTP 상태 코드 */
        private int status;

        /** HTTP 상태 메시지 */
        private String error;

        /** 구체적인 에러 메시지 */
        private String message;

        /** 입력값 검증 에러 맵 (필드명 -> 에러 메시지) */
        private Map<String, String> validationErrors;

        /**
         * Builder를 통한 ErrorResponse 생성자입니다.
         *
         * @param builder 설정된 Builder 객체
         */
        private ErrorResponse(Builder builder) {
            this.timestamp = builder.timestamp;
            this.status = builder.status;
            this.error = builder.error;
            this.message = builder.message;
            this.validationErrors = builder.validationErrors;
        }

        /**
         * ErrorResponse Builder 인스턴스를 생성합니다.
         *
         * @return 새로운 Builder 인스턴스
         */
        public static Builder builder() {
            return new Builder();
        }

        /** 에러 발생 시간을 반환합니다. */
        public LocalDateTime getTimestamp() { return timestamp; }

        /** HTTP 상태 코드를 반환합니다. */
        public int getStatus() { return status; }

        /** HTTP 상태 메시지를 반환합니다. */
        public String getError() { return error; }

        /** 구체적인 에러 메시지를 반환합니다. */
        public String getMessage() { return message; }

        /** 입력값 검증 에러 맵을 반환합니다. */
        public Map<String, String> getValidationErrors() { return validationErrors; }

        /**
         * ErrorResponse 생성을 위한 Builder 클래스입니다.
         *
         * 메서드 체이닝을 통해 유연하고 가독성 높은 객체 생성을 지원합니다.
         */
        public static class Builder {
            private LocalDateTime timestamp;
            private int status;
            private String error;
            private String message;
            private Map<String, String> validationErrors;

            /**
             * 에러 발생 시간을 설정합니다.
             *
             * @param timestamp 에러 발생 시간
             * @return Builder 인스턴스 (메서드 체이닝용)
             */
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            /**
             * HTTP 상태 코드를 설정합니다.
             *
             * @param status HTTP 상태 코드
             * @return Builder 인스턴스 (메서드 체이닝용)
             */
            public Builder status(int status) {
                this.status = status;
                return this;
            }

            /**
             * HTTP 상태 메시지를 설정합니다.
             *
             * @param error HTTP 상태 메시지
             * @return Builder 인스턴스 (메서드 체이닝용)
             */
            public Builder error(String error) {
                this.error = error;
                return this;
            }

            /**
             * 구체적인 에러 메시지를 설정합니다.
             *
             * @param message 에러 메시지
             * @return Builder 인스턴스 (메서드 체이닝용)
             */
            public Builder message(String message) {
                this.message = message;
                return this;
            }

            /**
             * 입력값 검증 에러 맵을 설정합니다.
             *
             * @param validationErrors 필드별 검증 에러 맵
             * @return Builder 인스턴스 (메서드 체이닝용)
             */
            public Builder validationErrors(Map<String, String> validationErrors) {
                this.validationErrors = validationErrors;
                return this;
            }

            /**
             * 설정된 값들로 ErrorResponse 객체를 생성합니다.
             *
             * @return 구성된 ErrorResponse 객체
             */
            public ErrorResponse build() {
                return new ErrorResponse(this);
            }
        }
    }
}