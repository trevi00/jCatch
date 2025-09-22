package org.jbd.backend.webmail.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jbd.backend.auth.service.JwtService;
import org.jbd.backend.common.dto.ApiResponse;
import org.jbd.backend.common.dto.PageResponse;
import org.jbd.backend.user.domain.User;
import org.jbd.backend.webmail.domain.EmailHistory;
import org.jbd.backend.webmail.dto.SendEmailRequest;
import org.jbd.backend.webmail.dto.SendEmailResponse;
import org.jbd.backend.webmail.service.WebMailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 웹메일 서비스 REST API 컨트롤러
 *
 * 잡았다 플랫폼의 웹 기반 메일 송수신 서비스를 제공하는 REST API 컨트롤러입니다.
 * 다양한 이메일 서비스 제공업체(SendGrid, Gmail 등)를 통해 이메일을 송신하고,
 * 송신 내역을 관리하며, AI 번역 기능을 통한 다국어 이메일 지원도 제공합니다.
 *
 * 주요 기능:
 * - 다중 이메일 서비스 제공업체를 통한 이메일 송신
 * - 송신한 이메일 내역 관리 및 조회
 * - AI 번역 기능을 통한 다국어 이메일 작성
 * - 이메일 송신 통계 및 분석 정보
 * - 이메일 템플릿 및 자동 작성 기능
 *
 * 지원 서비스 제공업체:
 * - SendGrid: 전문적인 이메일 마케팅 서비스
 * - Gmail SMTP: 구글 이메일 서비스
 * - 기타 SMTP 서비스: 사용자 정의 이메일 서버
 * - AWS SES: 아마존 이메일 서비스 (예정)
 *
 * 이메일 기능 특징:
 * - HTML 및 텍스트 형식 동시 지원
 * - 첨부파일 업로드 및 전송 기능
 * - 이메일 템플릿 시스템 연동
 * - 예약 송신 및 반복 송신 기능
 * - 이메일 대량 송신 및 배치 처리
 *
 * 사용 사례:
 * - 채용 공고 지원자에게 결과 안내 메일 송신
 * - 기업의 인사담당자와 구직자 간 소통
 * - 면접 일정 안내 및 결과 통보
 * - 다국어 지원을 위한 자동 번역 메일
 * - 마케팅 이메일 및 뉴스레터 발송
 *
 * 보안 및 제한사항:
 * - 인증된 사용자만 이메일 송신 가능
 * - 일일 송신 한도 및 스팩 방지 시스템
 * - 이메일 내용 검열 및 유해 콘텐츠 차단
 * - 개인정보 보호를 위한 데이터 암호화
 *
 * API 엔드포인트:
 * - POST /webmail/send: 이메일 송신
 * - GET /webmail/sent: 송신 내역 조회
 * - GET /webmail/translated: 번역된 이메일 목록
 * - GET /webmail/stats: 이메일 송신 통계
 */
@RestController
@RequestMapping("/webmail")
@RequiredArgsConstructor
@Slf4j
public class WebMailController {
    
    private final WebMailService webMailService;
    private final JwtService jwtService;
    
    /**
     * 이메일을 송신합니다.
     *
     * 지정된 이메일 서비스 제공업체를 통해 이메일을 송신하는 API입니다.
     * HTML 및 텍스트 형식을 동시에 지원하며, 첨부파일 전송도 가능합니다.
     *
     * @param request 이메일 송신 요청 데이터
     *                - to: 수신자 이메일 (필수)
     *                - subject: 이메일 제목 (필수)
     *                - content: 이메일 내용 (필수)
     *                - isHtml: HTML 형식 여부 (선택)
     * @param provider 이메일 서비스 제공업체 (기본값: sendgrid)
     * @param authHeader 사용자 인증 토큰
     * @param userDetails 인증된 사용자 정보
     * @return ResponseEntity<ApiResponse<SendEmailResponse>> 송신 결과
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendEmailResponse>> sendEmail(
            @Valid @RequestBody SendEmailRequest request,
            @RequestParam(value = "provider", defaultValue = "sendgrid") String provider,
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String token = authHeader.substring(7);
            Long userId = jwtService.extractUserId(token);
            
            // 현재 로그인한 사용자 정보 설정
            String currentUserEmail = userDetails.getUsername();
            // 실제로는 User 엔티티에서 이름을 가져와야 하지만, 임시로 이메일에서 추출
            String currentUserName = currentUserEmail.split("@")[0];
            
            request.setSenderEmail(currentUserEmail);
            request.setSenderName(currentUserName);
            
            // 이메일 발송 (제공업체별)
            log.info("{}를 사용하여 이메일 발송: {} -> {}", provider, currentUserEmail, request.getTo());
            SendEmailResponse response = webMailService.sendEmail(request, userId, provider);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(
                    ApiResponse.success("이메일이 성공적으로 발송되었습니다", response)
                );
            } else {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(response.getMessage())
                );
            }
            
        } catch (Exception e) {
            log.error("이메일 발송 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("이메일 발송 중 시스템 오류가 발생했습니다")
            );
        }
    }
    
    /**
     * 송신한 이메일 내역을 조회합니다.
     *
     * 사용자가 송신한 모든 이메일의 내역을 페이지네이션으로 조회하는 API입니다.
     * 송신 시간, 수신자, 제목, 송신 결과 등의 정보를 포함합니다.
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @param authHeader 사용자 인증 토큰
     * @param userDetails 인증된 사용자 정보
     * @return ResponseEntity<ApiResponse<PageResponse<EmailHistory>>> 송신 내역 목록
     */
    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<PageResponse<EmailHistory>>> getSentEmails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String token = authHeader.substring(7);
            Long userId = jwtService.extractUserId(token);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<EmailHistory> sentEmails = webMailService.getSentEmails(userId, pageable);
            
            return ResponseEntity.ok(
                ApiResponse.success("보낸편지함 조회 성공", new PageResponse<>(sentEmails))
            );
            
        } catch (Exception e) {
            log.error("보낸편지함 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("보낸편지함 조회 중 오류가 발생했습니다")
            );
        }
    }
    
    /**
     * AI 번역 기능을 사용한 이메일 목록을 조회합니다.
     *
     * 사용자가 AI 번역 기능을 활용하여 송신한 이메일들의 목록을 조회하는 API입니다.
     * 원본 언어와 번역 언어 정보도 함께 제공됩니다.
     *
     * @param authHeader 사용자 인증 토큰
     * @param userDetails 인증된 사용자 정보
     * @return ResponseEntity<ApiResponse<List<EmailHistory>>> 번역된 이메일 목록
     */
    @GetMapping("/translated")
    public ResponseEntity<ApiResponse<List<EmailHistory>>> getTranslatedEmails(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String token = authHeader.substring(7);
            Long userId = jwtService.extractUserId(token);
            
            List<EmailHistory> translatedEmails = webMailService.getTranslatedEmails(userId);
            
            return ResponseEntity.ok(
                ApiResponse.success("번역된 이메일 조회 성공", translatedEmails)
            );
            
        } catch (Exception e) {
            log.error("번역된 이메일 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("번역된 이메일 조회 중 오류가 발생했습니다")
            );
        }
    }
    
    /**
     * 이메일 송신 통계 정보를 조회합니다.
     *
     * 사용자의 이메일 송신 현황을 수치로 제공하는 API입니다.
     * 전체 송신 수, 번역된 이메일 수, 일반 이메일 수 등을 포함합니다.
     *
     * @param authHeader 사용자 인증 토큰
     * @param userDetails 인증된 사용자 정보
     * @return ResponseEntity<ApiResponse<EmailStatsResponse>> 이메일 통계 정보
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<EmailStatsResponse>> getEmailStats(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String token = authHeader.substring(7);
            Long userId = jwtService.extractUserId(token);
            
            long totalSentCount = webMailService.getSentEmailCount(userId);
            long translatedCount = webMailService.getTranslatedEmails(userId).size();
            
            EmailStatsResponse stats = EmailStatsResponse.builder()
                .totalSentCount(totalSentCount)
                .translatedCount(translatedCount)
                .regularCount(totalSentCount - translatedCount)
                .build();
            
            return ResponseEntity.ok(
                ApiResponse.success("이메일 통계 조회 성공", stats)
            );
            
        } catch (Exception e) {
            log.error("이메일 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("이메일 통계 조회 중 오류가 발생했습니다")
            );
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class EmailStatsResponse {
        private long totalSentCount;    // 총 발송 이메일 수
        private long translatedCount;   // 번역된 이메일 수
        private long regularCount;      // 일반 이메일 수
    }
}