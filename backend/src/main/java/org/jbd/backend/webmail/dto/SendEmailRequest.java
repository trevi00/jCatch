package org.jbd.backend.webmail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이메일 발송 요청 DTO
 *
 * 잡았다 플랫폼의 웹메일 발송 기능에서 사용자가 이메일을 보낼 때 필요한 모든 정보를 담는 데이터 전송 객체입니다.
 * SendGrid 및 Gmail API를 통한 이메일 발송을 지원하며, AI 번역 기능과 통합되어 다국어 이메일 발송이 가능합니다.
 * 취업 관련 이메일 (지원서, 감사 메일, 문의 등)을 전문적으로 작성하고 발송할 수 있도록 설계되었습니다.
 *
 * 핵심 기능:
 * - 기본 이메일 정보: 수신자, 제목, 내용
 * - AI 번역 통합: 자동 언어 번역 및 문서 타입별 최적화
 * - 발신자 정보 관리: 현재 로그인 사용자 정보 자동 설정
 * - 유연한 From 필드: 선택적 발신자 정보 커스터마이징
 * - 다중 이메일 제공업체 지원: SendGrid, Gmail 등
 *
 * 이메일 발송 프로세스:
 * 1. 사용자 입력 데이터 수집 및 검증
 * 2. 번역 필요 시 AI 번역 서비스 호출
 * 3. 선택된 이메일 제공업체를 통한 발송
 * 4. 발송 결과 로깅 및 사용자 피드백
 *
 * AI 번역 기능:
 * - 자동 언어 감지 및 번역
 * - 문서 타입별 최적화 (이메일, 이력서, 자기소개서 등)
 * - 소스/타겟 언어 명시적 지정 가능
 * - 번역 품질 보장을 위한 컨텍스트 인식
 *
 * 입력 검증 규칙:
 * - 수신자 이메일: 필수, 유효한 이메일 형식
 * - 제목: 필수, 최대 200자 제한
 * - 내용: 필수, 최대 10,000자 제한
 * - 발신자 정보: 시스템에서 자동 설정
 *
 * 번역 설정:
 * - translationNeeded: 번역 필요 여부 (기본값: false)
 * - sourceLanguage: 원문 언어 (기본값: "ko")
 * - targetLanguage: 번역 대상 언어 (기본값: "en")
 * - documentType: 문서 타입 (기본값: "email")
 *
 * 보안 고려사항:
 * - HTML 태그 및 스크립트 삽입 방지
 * - 이메일 주소 유효성 검증
 * - 스팸 방지를 위한 발송 빈도 제한
 * - 개인정보 보호를 위한 로깅 정책
 *
 * 사용 시나리오:
 * - 취업 지원 감사 메일 발송
 * - 채용담당자에게 문의 메일 발송
 * - 이력서 및 포트폴리오 첨부 메일
 * - 면접 확인 및 감사 메일
 * - 다국어 기업 대상 영문 메일
 *
 * 성능 최적화:
 * - 비동기 이메일 발송 처리
 * - 번역 결과 캐싱
 * - 대용량 첨부파일 최적화
 * - 발송 실패 시 재시도 로직
 *
 * 관련 클래스:
 * @see SendEmailResponse 이메일 발송 응답 DTO
 * @see org.jbd.backend.webmail.service.WebMailService 웹메일 서비스
 * @see org.jbd.backend.ai.service.AITranslationService AI 번역 서비스
 * @see org.jbd.backend.webmail.controller.WebMailController 웹메일 컨트롤러
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {
    
    @NotBlank(message = "수신자 이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String to;
    
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String subject;
    
    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 10000, message = "내용은 10,000자를 초과할 수 없습니다")
    private String content;
    
    // 번역 관련 필드
    private boolean translationNeeded = false;
    private String sourceLanguage = "ko";
    private String targetLanguage = "en";
    private String documentType = "email";
    
    // 발신자 정보 (현재 로그인한 사용자의 이메일)
    private String senderEmail;
    private String senderName;
    
    // From 필드 (선택사항 - 입력 시 본문에 포함)
    private String from;
}