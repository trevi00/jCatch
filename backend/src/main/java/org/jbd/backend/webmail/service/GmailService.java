package org.jbd.backend.webmail.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jbd.backend.ai.service.AITranslationService;
import org.jbd.backend.ai.dto.TranslationDto;
import org.jbd.backend.webmail.domain.EmailHistory;
import org.jbd.backend.webmail.domain.EmailStatus;
import org.jbd.backend.webmail.dto.SendEmailRequest;
import org.jbd.backend.webmail.dto.SendEmailResponse;
import org.jbd.backend.webmail.repository.EmailHistoryRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Properties;

/**
 * Gmail 서비스
 *
 * Google Gmail API를 통해 이메일을 발송하는 서비스입니다.
 * OAuth2 인증을 통해 사용자의 Gmail 계정으로 이메일을 발송하며, AI 번역 서비스와 연동합니다.
 *
 * 주요 기능:
 * - Google OAuth2 인증을 통한 사용자 Gmail 계정 연동
 * - Gmail API v1을 사용한 이메일 발송
 * - AI 번역 서비스 연동을 통한 다국어 이메일 발송
 * - MIME 메시지 포맷 지원
 * - Base64 인코딩을 통한 이메일 내용 처리
 * - 이메일 발송 이력 추적 및 기록
 *
 * 보안 및 인증:
 * - OAuth2 액세스 토큰 유효성 검증
 * - Google API 클라이언트 보안 연결
 * - 사용자 권한 기반 이메일 발송
 * - 접근 토큰 만료 및 갱신 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GmailService {
    
    private static final String APPLICATION_NAME = "JBD Webmail Service";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final EmailHistoryRepository emailHistoryRepository;
    private final AITranslationService aiTranslationService;
    
    /**
     * Gmail API를 통해 이메일을 발송합니다.
     *
     * 사용자의 Google 계정에 연동된 OAuth2 토큰을 사용하여 Gmail API로 이메일을 발송합니다.
     * AI 번역 서비스와 연동하여 다국어 이메일 발송을 지원하며,
     * MIME 형식으로 이메일을 생성하여 표준 이메일 클라이언트에서 올바르게 표시되도록 합니다.
     *
     * @param request 이메일 발송 요청 정보 (수신자, 제목, 내용, 번역 옵션 등)
     * @param userId 이메일을 발송하는 사용자 ID
     * @param userEmail 발송자의 Gmail 주소 (사용자의 Google 계정)
     * @return SendEmailResponse 이메일 발송 결과 (성공/실패, Gmail 메시지 ID, 발송 시간 등)
     */
    public SendEmailResponse sendEmailViaGmail(SendEmailRequest request, Long userId, String userEmail) {
        try {
            // OAuth2 클라이언트에서 액세스 토큰 가져오기
            OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient("google", userEmail);
            
            if (authorizedClient == null) {
                log.error("사용자 {}의 Google OAuth2 인증 정보를 찾을 수 없습니다", userEmail);
                return SendEmailResponse.failure("Google 계정 연동이 필요합니다. 다시 로그인해주세요.");
            }
            
            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
            if (accessToken == null || accessToken.getTokenValue() == null) {
                log.error("사용자 {}의 액세스 토큰이 유효하지 않습니다", userEmail);
                return SendEmailResponse.failure("Google 인증 토큰이 만료되었습니다. 다시 로그인해주세요.");
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
                        contentToSend = request.getContent();
                    }
                } catch (Exception e) {
                    log.error("번역 중 오류 발생: {}. 원본 내용으로 발송합니다.", e.getMessage());
                    contentToSend = request.getContent();
                }
            }
            
            // Gmail API 클라이언트 생성
            Gmail service = createGmailService(accessToken.getTokenValue());
            
            // 이메일 메시지 생성
            MimeMessage email = createEmail(request.getTo(), userEmail, request.getSubject(), contentToSend);
            Message message = createMessageWithEmail(email);
            
            // Gmail을 통해 이메일 발송
            message = service.users().messages().send("me", message).execute();
            
            if (message != null && message.getId() != null) {
                // 발송 성공
                LocalDateTime sentAt = LocalDateTime.now();
                
                // 이메일 히스토리 저장
                EmailHistory emailHistory = EmailHistory.builder()
                    .senderEmail(userEmail)
                    .senderName("JBD User") // 실제 사용자 이름으로 변경 가능
                    .recipientEmail(request.getTo())
                    .subject(request.getSubject())
                    .content(contentToSend)
                    .originalContent(originalContent)
                    .translatedContent(translatedContent)
                    .wasTranslated(wasTranslated)
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .documentType(request.getDocumentType())
                    .sendgridMessageId(message.getId()) // Gmail 메시지 ID 저장
                    .status(EmailHistory.EmailStatus.SENT)
                    .userId(userId)
                    .build();
                
                emailHistoryRepository.save(emailHistory);
                
                log.info("Gmail을 통한 이메일 발송 성공: {} -> {}", userEmail, request.getTo());
                
                if (wasTranslated) {
                    return SendEmailResponse.success(message.getId(), sentAt, originalContent, translatedContent);
                } else {
                    return SendEmailResponse.success(message.getId(), sentAt);
                }
                
            } else {
                log.error("Gmail API 응답에서 메시지 ID를 찾을 수 없습니다");
                return SendEmailResponse.failure("이메일 발송에 실패했습니다");
            }
            
        } catch (Exception e) {
            log.error("Gmail을 통한 이메일 발송 중 오류 발생", e);
            return SendEmailResponse.failure("이메일 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * Gmail API 서비스 클라이언트를 생성합니다.
     *
     * OAuth2 액세스 토큰을 사용하여 Google Gmail API에 인증된 요청을 보낼 수 있는
     * Gmail 서비스 인스턴스를 생성합니다. Google API 클라이언트 라이브러리를 사용하여
     * 보안 연결을 설정하고 애플리케이션 이름을 등록합니다.
     *
     * @param accessToken OAuth2 액세스 토큰
     * @return Gmail 인증된 Gmail API 서비스 인스턴스
     * @throws GeneralSecurityException 보안 설정 오류
     * @throws IOException 네트워크 연결 오류
     */
    private Gmail createGmailService(String accessToken) throws GeneralSecurityException, IOException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        // OAuth2 액세스 토큰으로 Gmail 서비스 생성
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    
    /**
     * MIME 형식의 이메일 메시지를 생성합니다.
     *
     * JavaMail API를 사용하여 표준 MIME 형식의 이메일 메시지를 생성합니다.
     * 이 메시지는 Gmail API를 통해 발송되며, 모든 이메일 클라이언트에서
     * 올바르게 인식되고 표시됩니다.
     *
     * @param to 수신자 이메일 주소
     * @param from 발송자 이메일 주소
     * @param subject 이메일 제목
     * @param bodyText 이메일 본문 내용
     * @return MimeMessage 생성된 MIME 이메일 메시지
     * @throws MessagingException 이메일 메시지 생성 오류
     */
    private MimeMessage createEmail(String to, String from, String subject, String bodyText)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        
        return email;
    }
    
    /**
     * MIME 메시지를 Gmail API용 메시지로 변환합니다.
     *
     * JavaMail의 MimeMessage를 Gmail API가 요구하는 형식으로 변환합니다.
     * 메시지를 바이트 배열로 직렬화하고 Base64 URL-safe 인코딩을 적용하여
     * Gmail API에서 처리할 수 있는 형태로 만듭니다.
     *
     * @param emailContent MIME 형식의 이메일 메시지
     * @return Message Gmail API용 메시지 객체
     * @throws MessagingException 메시지 처리 오류
     * @throws IOException 입출력 오류
     */
    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
        
        Message message = new Message();
        message.setRaw(encodedEmail);
        
        return message;
    }
}