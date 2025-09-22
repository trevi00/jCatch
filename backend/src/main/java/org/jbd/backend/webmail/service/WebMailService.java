package org.jbd.backend.webmail.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jbd.backend.ai.service.AITranslationService;
import org.jbd.backend.ai.dto.TranslationDto;
import org.jbd.backend.webmail.domain.EmailHistory;
import org.jbd.backend.webmail.dto.SendEmailRequest;
import org.jbd.backend.webmail.dto.SendEmailResponse;
import org.jbd.backend.webmail.repository.EmailHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 웹메일 서비스
 *
 * 이메일 전송 기능을 제공하는 서비스입니다.
 * SendGrid를 기본 이메일 제공업체로 사용하며, AI 번역 서비스와 연동하여 다국어 이메일 발송을 지원합니다.
 *
 * 주요 기능:
 * - 다중 이메일 제공업체 지원 (SendGrid, Gmail 등)
 * - AI 번역 서비스 연동을 통한 다국어 이메일 발송
 * - 이메일 발송 이력 관리 및 추적
 * - HTML 포맷 이메일 지원
 * - 이메일 내용 자동 포매팅 및 브랜딩
 * - 개발/테스트 환경용 Mock 모드 지원
 *
 * 보안 및 설정:
 * - 기본 발신자 정보 관리
 * - API 키 및 인증 정보 보안 처리
 * - 이메일 발송 로깅 및 모니터링
 * - UTF-8 인코딩 지원으로 한글 이메일 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WebMailService {
    
    @Value("${spring.mail.password:}")
    private String sendGridApiKey;
    
    @Value("${spring.profiles.active:local}")
    private String activeProfile;
    
    @Value("${webmail.default-sender.email}")
    private String defaultSenderEmail;
    
    @Value("${webmail.default-sender.name}")
    private String defaultSenderName;
    
    @Value("${webmail.test-mode:false}")
    private boolean testMode;
    
    private final EmailHistoryRepository emailHistoryRepository;
    private final AITranslationService aiTranslationService;
    
    /**
     * 이메일을 발송합니다.
     *
     * 지정된 이메일 제공업체를 통해 이메일을 발송합니다.
     * 다국어 지원을 위해 AI 번역 서비스를 활용하여 내용을 자동 번역할 수 있습니다.
     * 이메일 내용은 HTML 포맷으로 자동 포매팅되며, 브랜드 푸터가 추가됩니다.
     *
     * @param request 이메일 발송 요청 정보 (수신자, 제목, 내용, 번역 옵션 등)
     * @param userId 이메일을 발송하는 사용자 ID
     * @param provider 이메일 제공업체 ('sendgrid', 'gmail' 등)
     * @return SendEmailResponse 이메일 발송 결과 (성공/실패, 메시지 ID, 발송 시간 등)
     */
    public SendEmailResponse sendEmail(SendEmailRequest request, Long userId, String provider) {
        try {
            log.info("이메일 발송 요청 - Provider: {}, To: {}", provider, request.getTo());

            // Provider 검증
            if (provider == null || provider.trim().isEmpty()) {
                provider = "sendgrid"; // 기본값
            }

            // 현재는 SendGrid만 지원
            if (!"sendgrid".equalsIgnoreCase(provider) && !"gmail".equalsIgnoreCase(provider)) {
                log.warn("지원하지 않는 이메일 제공업체: {}. SendGrid로 처리합니다.", provider);
                provider = "sendgrid";
            }

            String contentToSend = request.getContent();
            String originalContent = null;
            String translatedContent = null;
            boolean wasTranslated = false;
            
            // 번역이 필요한 경우 AI 번역 서비스 호출
            if (request.isTranslationNeeded()) {
                log.info("번역 요청: {} -> {}", request.getSourceLanguage(), request.getTargetLanguage());
                
                try {
                    TranslationDto.TranslateResponse translationResponse = 
                        aiTranslationService.translateText(
                            request.getContent(),
                            request.getTargetLanguage(),
                            request.getSourceLanguage(),
                            "email"
                        );
                    
                    if (translationResponse != null && translationResponse.isSuccess() && 
                        translationResponse.getData() != null && 
                        translationResponse.getData().getTranslation() != null &&
                        translationResponse.getData().getTranslation().getTranslatedText() != null &&
                        !translationResponse.getData().getTranslation().getTranslatedText().trim().isEmpty()) {
                        
                        originalContent = request.getContent();
                        translatedContent = translationResponse.getData().getTranslation().getTranslatedText();
                        contentToSend = translatedContent;
                        wasTranslated = true;
                        
                        log.info("번역 성공: {} 글자 -> {} 글자", 
                            originalContent.length(), translatedContent.length());
                    } else {
                        log.warn("번역 실패: {}. 원본 내용으로 발송합니다.", 
                            translationResponse != null ? translationResponse.getMessage() : "번역 응답이 null입니다");
                        contentToSend = request.getContent(); // 원본 내용으로 fallback
                    }
                } catch (Exception e) {
                    log.error("번역 중 오류 발생: {}. 원본 내용으로 발송합니다.", e.getMessage());
                    contentToSend = request.getContent(); // 원본 내용으로 fallback
                }
            }
            
            // from 필드가 있으면 본문에 포함
            if (request.getFrom() != null && !request.getFrom().trim().isEmpty()) {
                contentToSend = "From: " + request.getFrom() + "\n\n" + contentToSend;
            }
            
            // 이메일 발송 (Provider별 처리)
            boolean isEmailSent = false;
            String messageId = "mock-" + System.currentTimeMillis();

            log.info("SendGrid API Key 상태: {}", sendGridApiKey == null ? "null" :
                sendGridApiKey.trim().isEmpty() ? "empty" : "configured (length: " + sendGridApiKey.length() + ")");
            log.info("웹메일 테스트 모드: {}, Provider: {}", testMode, provider);

            if (testMode || sendGridApiKey == null || sendGridApiKey.trim().isEmpty()) {
                // Mock 이메일 발송 (개발/테스트 환경)
                log.info("=== MOCK 이메일 발송 ({}) ===", provider.toUpperCase());
                log.info("From: {} <{}>", defaultSenderName, defaultSenderEmail);
                log.info("To: {}", request.getTo());
                log.info("Subject: {}", request.getSubject());
                log.info("Content: {}", contentToSend);
                log.info("Translation: {}", wasTranslated ? "Yes" : "No");
                if (wasTranslated) {
                    log.info("Original Language: {}", request.getSourceLanguage());
                    log.info("Target Language: {}", request.getTargetLanguage());
                }
                log.info("=====================");

                isEmailSent = true;
            } else {
                // Provider별 실제 이메일 발송
                if ("sendgrid".equalsIgnoreCase(provider)) {
                    isEmailSent = sendEmailViaSendGrid(request, contentToSend);
                    if (isEmailSent) {
                        messageId = "sendgrid-" + System.currentTimeMillis();
                    }
                } else if ("gmail".equalsIgnoreCase(provider)) {
                    // Gmail 발송 로직 (향후 구현)
                    log.warn("Gmail 제공업체는 아직 구현되지 않았습니다. SendGrid로 대체 발송합니다.");
                    isEmailSent = sendEmailViaSendGrid(request, contentToSend);
                    if (isEmailSent) {
                        messageId = "gmail-fallback-" + System.currentTimeMillis();
                    }
                } else {
                    log.error("지원하지 않는 이메일 제공업체입니다: {}", provider);
                    return SendEmailResponse.failure("지원하지 않는 이메일 제공업체입니다: " + provider);
                }

                if (!isEmailSent) {
                    return SendEmailResponse.failure("이메일 발송에 실패했습니다.");
                }
            }
            
            if (isEmailSent) {
                // 발송 성공
                LocalDateTime sentAt = LocalDateTime.now();
                
                // 이메일 이력 저장 (검증된 발신자 정보 사용)
                EmailHistory emailHistory = EmailHistory.builder()
                    .senderEmail(defaultSenderEmail)
                    .senderName(defaultSenderName)
                    .recipientEmail(request.getTo())
                    .subject(request.getSubject())
                    .content(contentToSend)
                    .originalContent(originalContent)
                    .translatedContent(translatedContent)
                    .wasTranslated(wasTranslated)
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .documentType(request.getDocumentType())
                    .sendgridMessageId(messageId)
                    .status(EmailHistory.EmailStatus.SENT)
                    .userId(userId)
                    .provider(provider)
                    .build();
                
                emailHistoryRepository.save(emailHistory);
                
                log.info("이메일 발송 성공: {} -> {}", defaultSenderEmail, request.getTo());
                
                if (wasTranslated) {
                    return SendEmailResponse.success(messageId, sentAt, originalContent, translatedContent);
                } else {
                    return SendEmailResponse.success(messageId, sentAt);
                }
                
            } else {
                log.error("이메일 발송에 실패했습니다");
                return SendEmailResponse.failure("이메일 발송에 실패했습니다");
            }
            
        } catch (Exception e) {
            log.error("이메일 발송 중 예상치 못한 오류 발생", e);
            return SendEmailResponse.failure("이메일 발송 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 사용자의 이메일 발송 이력을 조회합니다.
     *
     * 지정된 사용자가 발송한 모든 이메일의 이력을 최신순으로 페이징 처리하여 조회합니다.
     * 이메일 제목, 수신자, 발송 시간, 번역 여부 등의 정보가 포함됩니다.
     *
     * @param userId 이력을 조회할 사용자 ID
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return Page<EmailHistory> 페이징된 이메일 발송 이력 목록
     */
    @Transactional(readOnly = true)
    public Page<EmailHistory> getSentEmails(Long userId, Pageable pageable) {
        return emailHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * 사용자의 번역된 이메일 이력을 조회합니다.
     *
     * AI 번역 서비스를 통해 번역되어 발송된 이메일들만 필터링하여 조회합니다.
     * 원본 내용과 번역된 내용을 모두 포함하여 번역 내역을 확인할 수 있습니다.
     *
     * @param userId 번역 이메일 이력을 조회할 사용자 ID
     * @return List<EmailHistory> 번역된 이메일 이력 목록 (최신순 정렬)
     */
    @Transactional(readOnly = true)
    public List<EmailHistory> getTranslatedEmails(Long userId) {
        return emailHistoryRepository.findByUserIdAndWasTranslatedTrueOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 사용자의 이메일 발송 총 개수를 조회합니다.
     *
     * 해당 사용자가 지금까지 발송한 이메일의 총 개수를 반환합니다.
     * 대시보드나 통계 화면에서 사용자의 이메일 활동 수준을 보여주는 데 활용됩니다.
     *
     * @param userId 이메일 발송 개수를 조회할 사용자 ID
     * @return long 사용자의 이메일 발송 총 개수
     */
    @Transactional(readOnly = true)
    public long getSentEmailCount(Long userId) {
        return emailHistoryRepository.countByUserId(userId);
    }
    
    /**
     * 이메일 내용을 HTML 포맷으로 포매팅합니다.
     *
     * 일반 텍스트 내용을 HTML 형식으로 변환하고 JBD 브랜드 푸터를 추가합니다.
     * 줄바꿈을 <br> 태그로 변환하여 이메일 클라이언트에서 올바르게 표시되도록 처리합니다.
     *
     * @param content 포매팅할 원본 텍스트 내용
     * @return String HTML 포맷으로 포매팅된 이메일 내용
     */
    private String formatEmailContent(String content) {
        // HTML 형식으로 이메일 내용 포맷팅
        return String.format("""
            <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                %s
            </div>
            <hr style="margin-top: 30px; border: none; border-top: 1px solid #eee;">
            <p style="font-size: 12px; color: #888; text-align: center;">
                이 메일은 JBD 취업 지원 솔루션을 통해 발송되었습니다.
            </p>
            """, content.replace("\n", "<br>"));
    }
    
    /**
     * SendGrid 응답에서 메시지 ID를 추출합니다.
     *
     * SendGrid API 응답 헤더에서 X-Message-Id를 추출하여 메시지 추적을 위한 고유 ID를 얻습니다.
     * 추출에 실패하는 경우 타임스탬프 기반의 대체 ID를 생성합니다.
     *
     * @param response SendGrid API 응답 객체
     * @return String 메시지 추적을 위한 고유 ID
     */
    private String extractMessageId(Response response) {
        // SendGrid Response에서 Message ID 추출
        try {
            String messageId = response.getHeaders().get("X-Message-Id");
            if (messageId != null && !messageId.trim().isEmpty()) {
                return messageId.trim();
            }
        } catch (Exception e) {
            log.debug("헤더에서 Message ID 추출 실패: {}", e.getMessage());
        }

        // 헤더에서 추출 실패 시 타임스탬프 기반 ID 생성
        return "jbd-" + System.currentTimeMillis();
    }

    /**
     * SendGrid API를 통해 이메일을 실제로 발송합니다.
     *
     * SendGrid v3 API를 사용하여 이메일을 발솥합니다.
     * UTF-8 인코딩을 지원하여 한글 내용도 올바르게 발송되며,
     * HTML 포맷을 사용하여 시각적으로 매력적인 이메일을 생성합니다.
     *
     * @param request 이메일 발송 요청 정보
     * @param contentToSend 발송할 이메일 내용 (번역 지원 후)
     * @return boolean 이메일 발송 성공 여부
     */
    private boolean sendEmailViaSendGrid(SendEmailRequest request, String contentToSend) {
        try {
            log.info("SendGrid를 통한 이메일 발송 시작");

            // UTF-8 인코딩을 명시적으로 처리
            Email from = new Email(defaultSenderEmail, defaultSenderName);
            Email to = new Email(request.getTo());

            // 한글 제목과 내용을 위한 UTF-8 인코딩 처리
            String subject = request.getSubject();
            String htmlContent = formatEmailContent(contentToSend);

            // Content를 text/html로 설정 (SendGrid v3 API는 Content-Type에 charset 포함 불가)
            Content content = new Content("text/html", htmlContent);

            Mail mail = new Mail(from, subject, to, content);

            // SendGrid 설정
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request sgRequest = new Request();

            sgRequest.setMethod(Method.POST);
            sgRequest.setEndpoint("mail/send");

            // SendGrid v3 API 헤더 설정 (charset 제거)
            sgRequest.addHeader("Content-Type", "application/json");
            sgRequest.setBody(mail.build());

            log.debug("SendGrid 요청 Body: {}", mail.build());

            Response response = sg.api(sgRequest);

            log.info("SendGrid 응답: Status={}, Body={}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("SendGrid를 통한 이메일 발송 성공");
                return true;
            } else {
                log.error("SendGrid 이메일 발송 실패: Status={}, Body={}",
                    response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (IOException e) {
            log.error("SendGrid API 호출 중 IO 오류 발생", e);
            return false;
        } catch (Exception e) {
            log.error("SendGrid 이메일 발송 중 예상치 못한 오류 발생", e);
            return false;
        }
    }
}