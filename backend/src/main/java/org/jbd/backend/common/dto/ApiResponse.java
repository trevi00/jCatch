package org.jbd.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * API 응답을 표준화하는 공통 응답 클래스입니다.
 * 모든 REST API 엔드포인트에서 일관된 응답 형식을 제공하기 위해 사용됩니다.
 * 성공/실패 상태, 메시지, 데이터, 에러 코드, 타임스탬프를 포함합니다.
 *
 * @param <T> 응답 데이터의 타입
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** 요청 성공 여부를 나타내는 플래그 */
    private boolean success;

    /** 응답에 대한 설명 메시지 */
    private String message;

    /** 실제 응답 데이터 */
    private T data;

    /** 에러 발생 시 에러 코드 */
    private String errorCode;

    /** 응답 생성 시간 */
    private LocalDateTime timestamp;
    
    /**
     * ApiResponse의 기본 생성자입니다.
     * 현재 시간으로 타임스탬프를 초기화합니다.
     */
    private ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 성공 응답을 생성합니다.
     * 데이터만 포함하고 메시지는 포함하지 않습니다.
     *
     * @param <T> 응답 데이터의 타입
     * @param data 응답에 포함할 데이터
     * @return 성공 상태의 ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        return response;
    }
    
    /**
     * 성공 응답을 생성합니다.
     * 메시지와 데이터를 모두 포함합니다.
     *
     * @param <T> 응답 데이터의 타입
     * @param message 성공 메시지
     * @param data 응답에 포함할 데이터
     * @return 성공 상태의 ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        response.data = data;
        return response;
    }
    
    /**
     * 성공 응답을 생성합니다.
     * 메시지만 포함하고 데이터는 포함하지 않습니다.
     *
     * @param message 성공 메시지
     * @return 성공 상태의 ApiResponse 객체 (데이터 없음)
     */
    public static ApiResponse<Void> success(String message) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        return response;
    }
    
    /**
     * 에러 응답을 생성합니다.
     * 에러 메시지만 포함하고 에러 코드는 포함하지 않습니다.
     *
     * @param <T> 응답 데이터의 타입
     * @param message 에러 메시지
     * @return 실패 상태의 ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        return response;
    }
    
    /**
     * 에러 응답을 생성합니다.
     * 에러 메시지와 에러 코드를 모두 포함합니다.
     *
     * @param <T> 응답 데이터의 타입
     * @param message 에러 메시지
     * @param errorCode 에러 코드
     * @return 실패 상태의 ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.errorCode = errorCode;
        return response;
    }
    
    /**
     * 요청 성공 여부를 반환합니다.
     *
     * @return 성공 시 true, 실패 시 false
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 응답 메시지를 반환합니다.
     *
     * @return 성공 또는 에러 메시지
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 응답 데이터를 반환합니다.
     *
     * @return 응답에 포함된 실제 데이터
     */
    public T getData() {
        return data;
    }
    
    /**
     * 에러 코드를 반환합니다.
     *
     * @return 에러 발생 시 에러 코드, 성공 시 null
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 응답 생성 시간을 반환합니다.
     *
     * @return 응답이 생성된 시간
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}