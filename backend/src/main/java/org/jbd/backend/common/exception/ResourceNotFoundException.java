package org.jbd.backend.common.exception;

/**
 * 요청된 리소스를 찾을 수 없을 때 발생하는 예외입니다.
 *
 * 데이터베이스에서 특정 엔티티를 조회했으나 존재하지 않는 경우나,
 * 파일 시스템에서 파일을 찾을 수 없는 경우 등에 사용됩니다.
 *
 * 사용 예시:
 * - 존재하지 않는 사용자 ID로 조회 시
 * - 삭제된 게시글에 접근 시
 * - 업로드되지 않은 파일 다운로드 요청 시
 *
 * 특징:
 * - RuntimeException을 직접 상속하여 간단한 구조 제공
 * - GlobalExceptionHandler에서 404 Not Found로 자동 변환
 * - 리소스명, 필드명, 값을 포함한 구체적인 에러 메시지 생성 지원
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * 사용자 정의 메시지로 예외를 생성합니다.
     *
     * @param message 예외 메시지
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * 리소스 정보를 포함한 구체적인 예외 메시지를 자동 생성합니다.
     *
     * @param resourceName 리소스 이름 (예: "User", "Post", "File")
     * @param fieldName 조회에 사용된 필드명 (예: "id", "email", "filename")
     * @param fieldValue 조회에 사용된 값
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}