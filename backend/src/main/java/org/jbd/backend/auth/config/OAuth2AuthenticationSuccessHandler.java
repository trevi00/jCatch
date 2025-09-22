package org.jbd.backend.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jbd.backend.auth.dto.AuthenticationResponse;
import org.jbd.backend.auth.service.JwtService;
import org.jbd.backend.user.domain.User;
import org.jbd.backend.user.domain.UserProfile;
import org.jbd.backend.user.domain.enums.UserType;
import org.jbd.backend.user.domain.enums.OAuthProvider;
import org.jbd.backend.user.dto.UserResponseDto;
import org.jbd.backend.user.repository.UserRepository;
import org.jbd.backend.user.repository.UserProfileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            JwtService jwtService,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauth2Token.getPrincipal();
        
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");
        Boolean emailVerified = oauth2User.getAttribute("email_verified");
        
        // 사용자 조회 또는 생성
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseGet(() -> createNewUser(email, name, picture, emailVerified));

        // 기존 사용자의 경우 프로필 정보 업데이트
        UserProfile userProfile = userProfileRepository.findByUser(user).orElse(null);
        if (userProfile != null) {
            updateUserProfile(userProfile, name, picture);
        } else {
            // 프로필이 없는 경우 새로 생성
            createUserProfile(user, name, picture);
        }

        // 마지막 로그인 시간 업데이트
        user.updateLastLogin();
        userRepository.save(user);
        
        // JWT 토큰 생성
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        // UserProfile 다시 조회해서 최신 정보로 응답 생성
        UserProfile finalProfile = userProfileRepository.findByUser(user).orElse(null);

        // 응답 생성
        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponseDto.from(user, finalProfile))
                .build();

        // 프론트엔드로 리다이렉트 (토큰 포함)
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/auth/callback")
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("user", URLEncoder.encode(objectMapper.writeValueAsString(UserResponseDto.from(user, finalProfile)), StandardCharsets.UTF_8))
                .build().toUriString();
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
    
    private User createNewUser(String email, String name, String picture, Boolean emailVerified) {
        // OAuth 사용자 생성 (패스워드는 OAuth ID 사용)
        User user = new User(email, email, OAuthProvider.GOOGLE, UserType.GENERAL);
        if (emailVerified != null && emailVerified) {
            user.verifyEmail();
        }

        User savedUser = userRepository.save(user);

        // 프로필 생성
        createUserProfile(savedUser, name, picture);

        return savedUser;
    }
    
    private void updateUserProfile(UserProfile userProfile, String name, String picture) {
        boolean updated = false;

        if (name != null && !name.equals(userProfile.getFullName())) {
            userProfile.updateName(name, ""); // firstName으로 설정, lastName은 빈 문자열
            updated = true;
        }

        if (picture != null && !picture.equals(userProfile.getProfileImageUrl())) {
            userProfile.updateProfileImageUrl(picture);
            updated = true;
        }

        if (updated) {
            userProfileRepository.save(userProfile);
        }
    }

    private void createUserProfile(User user, String name, String picture) {
        String firstName = name != null ? name : "Google User";
        UserProfile userProfile = new UserProfile(user, firstName, "");

        if (picture != null) {
            userProfile.updateProfileImageUrl(picture);
        }

        userProfileRepository.save(userProfile);
    }
}