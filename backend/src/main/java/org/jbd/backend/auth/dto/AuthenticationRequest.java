package org.jbd.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 사용자 인증 요청 DTO
 *
 * 잡았다 플랫폼의 사용자 로그인 요청 시 전송되는 인증 정보를 담는 데이터 전송 객체입니다.
 * 클라이언트에서 서버로 이메일과 비밀번호를 전송할 때 사용되며,
 * Bean Validation을 통해 입력 데이터의 유효성을 검증합니다.
 *
 * 주요 기능:
 * - 이메일 기반 사용자 인증 정보 전송
 * - 입력 데이터 유효성 검증 (이메일 형식, 필수 값)
 * - JSON 직렬화/역직렬화 지원
 * - 안전한 비밀번호 전송 (HTTPS 권장)
 *
 * 보안 고려사항:
 * - 비밀번호는 평문으로 전송되므로 HTTPS 필수
 * - 클라이언트에서 전송 후 즉시 메모리 해제 권장
 * - 로그에 기록하지 않도록 주의
 * - 서버에서 수신 후 즉시 해싱 처리
 *
 * 입력 검증 규칙:
 * - 이메일: 필수 입력, 유효한 이메일 형식
 * - 비밀번호: 필수 입력, 공백 불허
 *
 * 사용 예시:
 * ```json
 * {
 *   "email": "user@example.com",
 *   "password": "securePassword123"
 * }
 * ```
 *
 * 관련 클래스:
 * @see AuthenticationResponse 인증 성공 시 응답 DTO
 * @see org.jbd.backend.auth.controller.AuthController 인증 컨트롤러
 * @see org.jbd.backend.auth.service.AuthenticationService 인증 서비스
 */
public class AuthenticationRequest {
    
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
    
    public AuthenticationRequest() {}
    
    public AuthenticationRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}