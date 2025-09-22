package org.jbd.backend.common.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 JPA 엔티티의 기본 클래스입니다.
 *
 * 공통적으로 필요한 감사(Audit) 필드들과 소프트 삭제 기능을 제공합니다.
 * 모든 도메인 엔티티는 이 클래스를 상속받아 일관된 데이터 관리가 가능합니다.
 *
 * 제공 기능:
 * - 생성일시/수정일시 자동 관리 (JPA Auditing 활용)
 * - 소프트 삭제 기능 (물리적 삭제 대신 논리적 삭제)
 * - 삭제된 데이터 복구 기능
 * - 삭제 상태 확인 메서드
 *
 * 사용 패턴:
 * - 모든 엔티티는 BaseEntity를 상속
 * - 삭제 시 delete() 메서드 호출로 소프트 삭제 수행
 * - 실제 조회 시 isDeleted = false 조건 추가 필요
 *
 * 주의사항:
 * - @MappedSuperclass로 테이블이 생성되지 않음
 * - AuditingEntityListener가 생성일시/수정일시 자동 관리
 * - 물리적 삭제는 특별한 경우에만 사용 권장
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    /** 엔티티 생성 일시 (JPA Auditing으로 자동 설정) */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 엔티티 최종 수정 일시 (JPA Auditing으로 자동 갱신) */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 소프트 삭제 일시 (삭제되지 않은 경우 null) */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 삭제 여부 플래그 (기본값: false) */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 기본 생성자입니다.
     * JPA 프레임워크에서 엔티티 인스턴스 생성 시 사용됩니다.
     */
    protected BaseEntity() {}

    /**
     * 엔티티를 소프트 삭제합니다.
     *
     * 물리적으로 데이터를 삭제하지 않고 삭제 마크만 설정하여,
     * 필요시 복구가 가능하도록 합니다.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.isDeleted = true;
    }

    /**
     * 소프트 삭제된 엔티티를 복구합니다.
     *
     * 삭제 마크를 해제하고 삭제 일시를 초기화하여
     * 정상 상태로 되돌립니다.
     */
    public void restore() {
        this.deletedAt = null;
        this.isDeleted = false;
    }

    /**
     * 엔티티의 삭제 상태를 확인합니다.
     *
     * @return 삭제된 경우 true, 정상 상태인 경우 false
     */
    public boolean isDeleted() {
        return isDeleted != null && isDeleted;
    }
    
    /**
     * 엔티티 생성 일시를 반환합니다.
     *
     * @return 생성 일시
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 엔티티 최종 수정 일시를 반환합니다.
     *
     * @return 최종 수정 일시
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 소프트 삭제 일시를 반환합니다.
     *
     * @return 삭제 일시 (삭제되지 않은 경우 null)
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 삭제 여부 플래그를 반환합니다.
     *
     * @return 삭제 여부 (true: 삭제됨, false: 정상)
     */
    public Boolean getIsDeleted() {
        return isDeleted;
    }

    /**
     * 엔티티 생성 일시를 설정합니다.
     * JPA 프레임워크에서만 사용됩니다.
     *
     * @param createdAt 생성 일시
     */
    protected void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 엔티티 최종 수정 일시를 설정합니다.
     * JPA 프레임워크에서만 사용됩니다.
     *
     * @param updatedAt 최종 수정 일시
     */
    protected void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 소프트 삭제 일시를 설정합니다.
     * 일반적으로 delete() 메서드 사용을 권장합니다.
     *
     * @param deletedAt 삭제 일시
     */
    protected void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 삭제 여부 플래그를 설정합니다.
     * 일반적으로 delete() 또는 restore() 메서드 사용을 권장합니다.
     *
     * @param isDeleted 삭제 여부
     */
    protected void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}