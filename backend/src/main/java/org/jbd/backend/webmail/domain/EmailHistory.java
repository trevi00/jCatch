package org.jbd.backend.webmail.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.jbd.backend.common.entity.BaseEntity;

/**
 * 이메일 발송 히스토리 도메인 엔티티
 *
 * 잡았다 플랫폼의 웹메일 서비스에서 발송된 모든 이메일의 기록을 관리하는 도메인 객체입니다.
 * 사용자가 SendGrid 또는 Gmail을 통해 발송한 이메일의 상세 정보, 번역 데이터, 발송 상태를 포괄적으로 추적하며,
 * 감사(Audit), 사용자 경험 개선, 서비스 품질 관리를 위한 핵심 데이터를 제공합니다.
 *
 * 핵심 기능:
 * - 이메일 발송 기록: 발신자, 수신자, 제목, 내용 완전 추적
 * - AI 번역 통합: 원문과 번역문 동시 저장 및 언어 정보 관리
 * - 다중 제공업체 지원: SendGrid, Gmail 등 이메일 서비스 구분 관리
 * - 발송 상태 추적: 성공, 실패, 대기 상태 실시간 모니터링
 * - 사용자별 히스토리: 개인별 발송 기록 조회 및 통계 제공
 *
 * 번역 기능 통합:
 * - 원문 보존: originalContent를 통한 사용자 입력 원문 저장
 * - 번역문 저장: translatedContent를 통한 AI 번역 결과 보관
 * - 번역 메타데이터: 소스/타겟 언어, 문서 타입, 번역 여부 기록
 * - 품질 추적: 번역 사용 패턴 및 선호 언어 분석 데이터
 *
 * 이메일 제공업체 관리:
 * - SendGrid 통합: sendgridMessageId를 통한 메시지 추적
 * - Gmail API 지원: Gmail 서비스를 통한 발송 기록
 * - 제공업체별 구분: provider 필드로 사용 서비스 식별
 * - 통합 관리: 여러 서비스 간 일관된 기록 형식
 *
 * 발송 상태 관리 (EmailStatus):
 * - SENT: 발송 완료 (이메일 서버에서 정상 처리)
 * - FAILED: 발송 실패 (네트워크 오류, 인증 실패, 형식 오류 등)
 * - PENDING: 발송 대기 (큐에서 처리 대기 중)
 *
 * 데이터 보관 및 관리:
 * - 완전한 이메일 복사본: 제목과 내용 전체 저장
 * - 발신자 정보: 이메일 주소와 표시 이름 분리 저장
 * - 수신자 추적: 대상 이메일 주소 정확한 기록
 * - 시간 정보: BaseEntity를 통한 생성/수정 시각 자동 관리
 *
 * 사용자 경험 향상:
 * - 발송 히스토리 조회: 사용자별 이메일 발송 기록 열람
 * - 재발송 기능: 이전 이메일 템플릿 재사용
 * - 발송 통계: 개인별 이메일 발송 빈도 및 패턴 분석
 * - 오류 추적: 발송 실패 원인 분석 및 개선 방안 제시
 *
 * 보안 및 프라이버시:
 * - 개인정보 보호: 민감한 내용 접근 제어 및 암호화
 * - 사용자별 격리: userId를 통한 개인 데이터 분리
 * - 데이터 보존 정책: 일정 기간 후 자동 삭제 또는 아카이브
 * - 접근 로그: 히스토리 조회 기록 감사
 *
 * 비즈니스 분석:
 * - 이메일 사용 통계: 발송량, 성공률, 인기 언어 쌍 분석
 * - 번역 서비스 효과: 번역 사용 빈도 및 사용자 만족도
 * - 제공업체 성능: SendGrid vs Gmail 성능 비교 분석
 * - 사용자 행동 패턴: 이메일 발송 시간, 빈도, 내용 유형 분석
 *
 * 시스템 모니터링:
 * - 발송 성공률: 전체 및 제공업체별 성공률 모니터링
 * - 에러 패턴: 실패 원인별 분류 및 트렌드 분석
 * - 성능 지표: 발송 지연 시간, 처리량 등 성능 메트릭
 * - 알림 시스템: 임계값 초과 시 관리자 알림
 *
 * 규정 준수:
 * - GDPR 준수: 개인 데이터 보호 및 삭제 권리 보장
 * - CAN-SPAM Act: 스팸 방지 규정 준수
 * - 데이터 보존: 법적 요구사항에 따른 데이터 보관 기간
 * - 감사 추적: 완전한 감사 로그 제공
 *
 * 성능 최적화:
 * - 인덱싱: 사용자별, 날짜별, 상태별 효율적 검색
 * - 페이징: 대량 히스토리 데이터 효율적 조회
 * - 아카이빙: 오래된 데이터 별도 저장소 이관
 * - 캐싱: 자주 조회되는 통계 데이터 캐시 활용
 *
 * 장애 대응:
 * - 백업 및 복구: 중요한 이메일 히스토리 백업
 * - 데이터 무결성: 이메일 내용 변조 방지 검증
 * - 복구 절차: 데이터 손실 시 복구 프로세스
 * - 모니터링: 실시간 데이터 품질 검증
 *
 * 사용 시나리오:
 * - 취업 지원 이메일: 감사 메일, 문의 메일 발송 기록
 * - 다국어 소통: 번역된 이메일 발송 및 추적
 * - 비즈니스 통신: 기업 간 공식 이메일 교환
 * - 고객 지원: 사용자 문의 응답 이메일 관리
 * - 마케팅 커뮤니케이션: 알림 및 안내 이메일 발송
 *
 * 관련 클래스:
 * @see org.jbd.backend.webmail.service.WebMailService 웹메일 서비스
 * @see org.jbd.backend.webmail.dto.SendEmailRequest 이메일 발송 요청
 * @see org.jbd.backend.webmail.dto.SendEmailResponse 이메일 발송 응답
 * @see EmailStatus 이메일 발송 상태 열거형
 * @see BaseEntity 공통 엔티티 기능
 */
@Entity
@Table(name = "email_history")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailHistory extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "sender_email", nullable = false)
    private String senderEmail;
    
    @Column(name = "sender_name")
    private String senderName;
    
    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;
    
    @Column(name = "subject", nullable = false, length = 200)
    private String subject;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;
    
    @Column(name = "translated_content", columnDefinition = "TEXT")
    private String translatedContent;
    
    @Builder.Default
    @Column(name = "was_translated")
    private boolean wasTranslated = false;
    
    @Column(name = "source_language", length = 10)
    private String sourceLanguage;
    
    @Column(name = "target_language", length = 10)
    private String targetLanguage;
    
    @Column(name = "document_type", length = 50)
    private String documentType;
    
    @Column(name = "sendgrid_message_id")
    private String sendgridMessageId;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status = EmailStatus.SENT;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder.Default
    @Column(name = "provider", length = 20)
    private String provider = "sendgrid";

    public enum EmailStatus {
        SENT, FAILED, PENDING
    }
}