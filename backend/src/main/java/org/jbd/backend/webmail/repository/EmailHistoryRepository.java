package org.jbd.backend.webmail.repository;

import org.jbd.backend.webmail.domain.EmailHistory;
import org.jbd.backend.webmail.domain.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Long> {
    
    // 사용자별 발송 이메일 조회
    Page<EmailHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // 사용자별 발송 이메일 조회 (상태별)
    Page<EmailHistory> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, EmailStatus status, Pageable pageable);
    
    // 수신자 이메일로 조회
    List<EmailHistory> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);
    
    // 기간별 발송 이메일 조회
    @Query("SELECT e FROM EmailHistory e WHERE e.userId = :userId AND e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    List<EmailHistory> findByUserIdAndCreatedAtBetween(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    // 번역 사용 이메일만 조회
    List<EmailHistory> findByUserIdAndWasTranslatedTrueOrderByCreatedAtDesc(Long userId);
    
    // 발송 통계
    @Query("SELECT COUNT(e) FROM EmailHistory e WHERE e.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(e) FROM EmailHistory e WHERE e.userId = :userId AND e.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") EmailStatus status);
}