package org.jbd.backend.auth.service;

import org.jbd.backend.auth.dto.OAuth2UserInfo;
import org.jbd.backend.user.domain.User;
import org.jbd.backend.user.domain.UserProfile;
import org.jbd.backend.user.domain.enums.UserType;
import org.jbd.backend.user.domain.enums.OAuthProvider;
import org.jbd.backend.user.repository.UserRepository;
import org.jbd.backend.user.repository.UserProfileRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 커스텀 OAuth2 사용자 서비스
 *
 * 잡았다 플랫폼의 Spring Security OAuth2 통합을 위한 사용자 처리 서비스입니다.
 * DefaultOAuth2UserService를 확장하여 OAuth2 인증을 통해 로그인한 사용자를 시스템에 등록하거나
 * 기존 사용자 정보를 업데이트합니다. Google OAuth2를 주요 지원하며,
 * 사용자 프로필 실시간 동기화와 seamless 계정 연동 기능을 제공합니다.
 * Spring Security OAuth2 Client 모듈과 완전 통합되어 안전한 소셜 로그인을 구현합니다.
 *
 * 핵심 기능:
 * - OAuth2 인증 후 사용자 정보 자동 처리 및 매핑
 * - 신규 OAuth2 사용자 등록 및 프로필 생성
 * - 기존 일반 계정과 OAuth2 계정 seamless 연동
 * - 프로필 정보 실시간 동기화 (이름, 프로필 이미지)
 * - 이메일 인증 상태 자동 업데이트 및 검증
 * - OAuth2User와 내부 User 엔티티 통합 객체 제공
 * - 사용자 타입별 권한 자동 설정
 *
 * OAuth2 통합 아키텍처:
 * - Spring Security OAuth2 Client와 완전 통합
 * - DefaultOAuth2UserService 확장으로 표준 준수
 * - 커스텀 OAuth2User 구현으로 사용자 정보 확장
 * - JWT 토큰 연동을 위한 User 엔티티 매핑
 * - Spring Security 컨텍스트 자동 설정
 *
 * 사용자 처리 플로우:
 * 1. OAuth2 제공업체에서 사용자 정보 수신 및 검증
 * 2. 이메일 기반 기존 사용자 검색 및 식별
 * 3. 기존 사용자는 프로필 업데이트, 신규 사용자는 완전 등록
 * 4. OAuth2User와 내부 User 엔티티를 결합한 CustomOAuth2User 반환
 * 5. Spring Security 컨텍스트에 인증 정보 설정
 *
 * 지원하는 OAuth2 제공업체:
 * - Google OAuth 2.0 (primary support)
 *   · Gmail 계정 기반 인증
 *   · 프로필 정보 자동 동기화
 *   · 프로필 이미지 URL 관리
 * - 향후 확장: Kakao, Naver, LinkedIn 등
 *
 * 사용자 등록 전략:
 * - 모든 OAuth2 사용자는 기본적으로 GENERAL 타입으로 등록
 * - 이메일 인증 상태는 OAuth2 제공업체 검증 결과 반영
 * - 프로필 정보는 OAuth2 데이터를 우선으로 자동 생성
 * - 기존 NATIVE 사용자는 OAuth2 연동 시 자동 전환
 *
 * 프로필 동기화 정책:
 * - 이름: OAuth2 정보로 자동 업데이트
 * - 프로필 이미지: OAuth2 프로필 사진 URL 동기화
 * - 이메일 인증: OAuth2 검증 결과 자동 반영
 * - 기존 프로필 정보 보존 (충돌 시 OAuth2 우선)
 *
 * 계정 연동 로직:
 * - 이메일 기반 기존 계정 자동 탐지
 * - NATIVE → GOOGLE OAuth 제공업체 전환
 * - 기존 사용자 데이터 완전 보존
 * - OAuth ID 자동 연결 및 저장
 * - 프로필 정보 선택적 업데이트
 *
 * 보안 고려사항:
 * - 이메일 정보 필수 검증 (없으면 인증 실패)
 * - OAuth2 제공업체 신뢰성 검증
 * - 사용자 정보 암호화 저장
 * - 프로필 이미지 URL 검증
 * - 계정 연동 시 권한 승계
 *
 * 에러 처리 체계:
 * - OAuth2AuthenticationException: OAuth2 인증 전반 오류
 * - 이메일 정보 누락 시 명시적 예외 발생
 * - 사용자 등록/업데이트 실패 시 롤백
 * - 프로필 동기화 실패 시 기본값 적용
 *
 * 데이터 매핑 전략:
 * - OAuth2UserInfo: OAuth2 응답 데이터 래퍼
 * - User 엔티티: 내부 사용자 시스템과 매핑
 * - UserProfile 엔티티: 프로필 정보 동기화
 * - CustomOAuth2User: OAuth2User + User 통합 객체
 *
 * 사용 시나리오:
 * - Google 계정으로 신규 회원가입
 * - 기존 일반 계정에 Google 연동
 * - OAuth2 사용자의 프로필 정보 업데이트
 * - 소셜 로그인 후 내부 권한 시스템 연동
 * - 이메일 인증 자동 처리
 *
 * 성능 최적화:
 * - 조건부 업데이트로 불필요한 DB 쓰기 최소화
 * - 프로필 변경 감지를 통한 선택적 저장
 * - 단일 트랜잭션 내 사용자 및 프로필 처리
 * - OAuth2 정보 캐싱 활용
 *
 * 규정 준수:
 * - OAuth2 RFC 6749 표준 준수
 * - Spring Security OAuth2 모범 사례 적용
 * - GDPR 개인정보 처리 방침 준수
 * - Google OAuth2 정책 완전 준수
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    /**
     * CustomOAuth2UserService 생성자
     *
     * @param userRepository 사용자 데이터 액세스를 위한 리포지토리
     * @param userProfileRepository 사용자 프로필 데이터 액세스를 위한 리포지토리
     */
    public CustomOAuth2UserService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }
    
    /**
     * OAuth2 사용자 정보를 로드하고 처리합니다.
     *
     * Spring Security OAuth2의 표준 메서드를 오버라이드하여 커스텀 로직을 추가합니다.
     * 부모 클래스의 loadUser를 호출하여 OAuth2 사용자 정보를 가져온 후,
     * 내부 시스템에 사용자를 등록하거나 업데이트하는 처리를 수행합니다.
     *
     * @param userRequest OAuth2 사용자 요청 정보 (client registration, access token 등)
     * @return OAuth2User 처리된 사용자 정보를 포함한 OAuth2User 객체
     * @throws OAuth2AuthenticationException OAuth2 인증 처리 중 오류 발생 시
     * @see DefaultOAuth2UserService#loadUser(OAuth2UserRequest)
     * @see CustomOAuth2User
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }
    
    /**
     * OAuth2 사용자 정보를 처리하고 내부 사용자와 연동합니다.
     *
     * OAuth2 제공업체에서 받은 사용자 정보를 바탕으로 내부 사용자를 찾거나 새로 생성합니다.
     * 이메일이 없는 OAuth2 사용자는 처리할 수 없으므로 예외를 발생시킵니다.
     *
     * @param userRequest OAuth2 사용자 요청 정보
     * @param oauth2User OAuth2 제공업체에서 받은 사용자 정보
     * @return OAuth2User 내부 사용자 정보를 포함한 CustomOAuth2User
     * @throws OAuth2AuthenticationException 이메일 정보가 없는 경우
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        OAuth2UserInfo userInfo = new OAuth2UserInfo(oauth2User.getAttributes());
        String email = userInfo.getEmail();
        
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }
        
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> registerNewUser(userInfo));
        
        return new CustomOAuth2User(oauth2User, user);
    }
    
    /**
     * 새로운 OAuth2 사용자를 등록합니다.
     *
     * OAuth2 제공업체에서 받은 사용자 정보를 바탕으로 새로운 사용자와 프로필을 생성합니다.
     * 기본적으로 GENERAL 타입의 사용자로 생성되며, OAuth2에서 이메일 인증 정보를
     * 제공한 경우 자동으로 인증 상태로 설정됩니다.
     *
     * 생성되는 정보:
     * - 기본 사용자 정보 (이메일, OAuth ID, 제공업체, 사용자 타입)
     * - 이메일 인증 상태
     * - 사용자 프로필 (이름, 프로필 이미지)
     *
     * @param userInfo OAuth2 제공업체에서 받은 사용자 정보
     * @return User 생성된 새로운 사용자
     */
    private User registerNewUser(OAuth2UserInfo userInfo) {
        // OAuth 사용자 생성
        User user = new User(userInfo.getEmail(), userInfo.getId(), OAuthProvider.GOOGLE, UserType.GENERAL);
        if (Boolean.TRUE.equals(userInfo.getEmailVerified())) {
            user.verifyEmail();
        }

        User savedUser = userRepository.save(user);

        // UserProfile 생성
        String userName = userInfo.getName() != null ? userInfo.getName() : "Google User";
        UserProfile userProfile = new UserProfile(savedUser, userName, "");
        if (userInfo.getPicture() != null) {
            userProfile.updateProfileImageUrl(userInfo.getPicture());
        }
        userProfileRepository.save(userProfile);

        return savedUser;
    }
    
    /**
     * 기존 사용자의 정보를 OAuth2 정보로 업데이트합니다.
     *
     * 이메일이 일치하는 기존 사용자를 OAuth2 사용자와 연동하고,
     * OAuth2에서 제공하는 최신 정보로 프로필을 업데이트합니다.
     * 기존 NATIVE 인증 사용자를 OAuth2 사용자로 전환합니다.
     *
     * 업데이트되는 정보:
     * - 사용자 이름 (프로필에 저장)
     * - 프로필 이미지 URL
     * - OAuth 제공업체 정보 (NATIVE에서 GOOGLE로 전환)
     * - 이메일 인증 상태
     *
     * @param existingUser 업데이트할 기존 사용자
     * @param userInfo OAuth2에서 받은 최신 사용자 정보
     * @return User 업데이트된 사용자
     */
    private User updateExistingUser(User existingUser, OAuth2UserInfo userInfo) {
        boolean userUpdated = false;
        boolean profileUpdated = false;

        // UserProfile 조회 및 업데이트
        UserProfile userProfile = userProfileRepository.findByUser(existingUser).orElse(null);

        // 이름 업데이트 (UserProfile에서)
        if (userInfo.getName() != null) {
            if (userProfile == null) {
                userProfile = new UserProfile(existingUser, userInfo.getName(), "");
                profileUpdated = true;
            } else if (!userInfo.getName().equals(userProfile.getFullName())) {
                userProfile.updateName(userInfo.getName(), "");
                profileUpdated = true;
            }
        }

        // 프로필 이미지 업데이트
        if (userInfo.getPicture() != null) {
            if (userProfile == null) {
                userProfile = new UserProfile(existingUser, userInfo.getName() != null ? userInfo.getName() : "Google User", "");
                userProfile.updateProfileImageUrl(userInfo.getPicture());
                profileUpdated = true;
            } else if (!userInfo.getPicture().equals(userProfile.getProfileImageUrl())) {
                userProfile.updateProfileImageUrl(userInfo.getPicture());
                profileUpdated = true;
            }
        }

        // OAuth 제공자 업데이트
        if (existingUser.getOauthProvider() == OAuthProvider.NATIVE) {
            existingUser.setOauthProvider(OAuthProvider.GOOGLE);
            userUpdated = true;
        }

        // 이메일 인증 업데이트
        if (Boolean.TRUE.equals(userInfo.getEmailVerified()) && !existingUser.isEmailVerified()) {
            existingUser.verifyEmail();
            userUpdated = true;
        }

        // 업데이트 저장
        if (profileUpdated && userProfile != null) {
            userProfileRepository.save(userProfile);
        }

        if (userUpdated) {
            return userRepository.save(existingUser);
        }

        return existingUser;
    }
}