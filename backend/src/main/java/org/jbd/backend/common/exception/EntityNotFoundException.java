package org.jbd.backend.common.exception;

/**
 * JPA 엔티티를 찾을 수 없을 때 발생하는 예외입니다.
 *
 * 데이터베이스에서 특정 엔티티 조회 시 해당 엔티티가 존재하지 않거나,
 * 삭제된 엔티티에 접근하려 할 때 사용됩니다.
 *
 * 사용 예시:
 * - JPA Repository의 findById() 결과가 empty일 때
 * - 연관관계 매핑에서 참조하는 엔티티가 없을 때
 * - 소프트 삭제된 엔티티에 접근 시
 *
 * 특징:
 * - RuntimeException을 직접 상속하여 간단한 구조 제공
 * - ResourceNotFoundException과 유사하지만 JPA 엔티티 전용
 * - 원인 예외(cause)를 포함할 수 있어 디버깅에 유용
 */
public class EntityNotFoundException extends RuntimeException {

    /**
     * 사용자 정의 메시지로 예외를 생성합니다.
     *
     * @param message 예외 메시지
     */
    public EntityNotFoundException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인 예외를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 메시지
     * @param cause 이 예외의 원인이 된 예외
     */
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}