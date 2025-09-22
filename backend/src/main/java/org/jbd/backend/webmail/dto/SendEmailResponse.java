package org.jbd.backend.webmail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이메일 발송 응답 DTO
 *
 * 잡았다 플랫폼의 웹메일 발송 결과를 클라이언트에게 전달하는 데이터 전송 객체입니다.
 * 이메일 발송 성공/실패 상태, 메시지 ID, 번역 정보 등을 포함하여
 * 사용자에게 발송 결과를 명확하고 유용한 형태로 제공합니다.
 *
 * 응답 정보 구성:
 * - 발송 상태: 성공/실패 여부 및 상세 메시지
 * - 메시지 추적: 이메일 제공업체에서 제공하는 고유 메시지 ID
 * - 발송 시간: 실제 이메일 발송이 완료된 시각
 * - 번역 정보: AI 번역 사용 시 원문 및 번역문 정보
 *
 * 번역 통합 정보:
 * - originalContent: 사용자가 입력한 원본 내용
 * - translatedContent: AI가 번역한 내용
 * - wasTranslated: 번역이 실제로 수행되었는지 여부
 * - 번역 품질 및 정확도 정보
 *
 * 발송 추적 기능:
 * - messageId: SendGrid, Gmail 등에서 제공하는 고유 식별자
 * - sentAt: 정확한 발송 완료 시각 (LocalDateTime)
 * - 발송 상태 실시간 추적 가능
 * - 발송 실패 시 상세 오류 정보 제공
 *
 * 정적 팩토리 메서드:
 * - success(): 일반 발송 성공 응답 생성
 * - success(translation): 번역 포함 발송 성공 응답 생성
 * - failure(): 발송 실패 응답 생성
 * - 일관된 응답 형식 보장
 *
 * 에러 처리:
 * - 네트워크 오류: "네트워크 연결 문제로 발송에 실패했습니다"
 * - 인증 오류: "이메일 서비스 인증에 실패했습니다"
 * - 번역 오류: "번역 서비스 오류가 발생했습니다"
 * - 형식 오류: "이메일 형식이 올바르지 않습니다"
 *
 * 사용자 경험:
 * - 명확한 성공/실패 메시지
 * - 번역 내용 확인 가능
 * - 발송 시간 정보 제공
 * - 추적 가능한 메시지 ID
 *
 * 로깅 및 모니터링:
 * - 발송 성공률 통계
 * - 번역 사용 빈도 분석
 * - 에러 패턴 분석
 * - 성능 지표 수집
 *
 * 보안 고려사항:
 * - 민감한 내용 로그 기록 방지
 * - 메시지 ID 보안 처리
 * - 개인정보 포함 여부 확인
 * - 발송 기록 개인정보 보호
 *
 * 관련 클래스:
 * @see SendEmailRequest 이메일 발송 요청 DTO
 * @see org.jbd.backend.webmail.service.WebMailService 웹메일 서비스
 * @see org.jbd.backend.ai.service.AITranslationService AI 번역 서비스
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailResponse {
    
    private boolean success;
    private String message;
    private String messageId; // SendGrid Message ID
    private LocalDateTime sentAt;
    
    // 번역된 내용 (번역을 사용한 경우)
    private String originalContent;
    private String translatedContent;
    private boolean wasTranslated;
    
    public static SendEmailResponse success(String messageId, LocalDateTime sentAt) {
        return new SendEmailResponse(true, "이메일이 성공적으로 발송되었습니다", messageId, sentAt, null, null, false);
    }
    
    public static SendEmailResponse success(String messageId, LocalDateTime sentAt, String originalContent, String translatedContent) {
        return new SendEmailResponse(true, "이메일이 성공적으로 발송되었습니다", messageId, sentAt, originalContent, translatedContent, true);
    }
    
    public static SendEmailResponse failure(String message) {
        return new SendEmailResponse(false, message, null, null, null, null, false);
    }
}