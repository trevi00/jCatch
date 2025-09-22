package org.jbd.backend.config;

import org.jbd.backend.auth.config.JwtAuthenticationEntryPoint;
import org.jbd.backend.auth.config.JwtAuthenticationFilter;
import org.jbd.backend.auth.config.OAuth2AuthenticationFailureHandler;
import org.jbd.backend.auth.config.OAuth2AuthenticationSuccessHandler;
import org.jbd.backend.auth.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 보안 설정 클래스
 *
 * JWT 토큰 기반 인증, OAuth2 로그인, CORS 설정 등 전체 애플리케이션의 보안 정책을 정의합니다.
 *
 * 주요 기능:
 * - JWT 토큰 기반 무상태 인증
 * - OAuth2 소셜 로그인 (Google 등)
 * - 역할 기반 접근 제어 (GENERAL, COMPANY, ADMIN)
 * - CORS 정책 설정
 * - 비밀번호 암호화 (BCrypt)
 *
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 * @see JwtAuthenticationFilter
 * @see OAuth2AuthenticationSuccessHandler
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /** JWT 인증 실패 시 처리하는 진입점 */
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /** 모든 요청에서 JWT 토큰을 검증하는 필터 */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /** 사용자 정보를 로드하는 서비스 */
    private final UserDetailsService userDetailsService;

    /** OAuth2 사용자 정보를 처리하는 커스텀 서비스 */
    private final CustomOAuth2UserService customOAuth2UserService;

    /** OAuth2 로그인 성공 시 처리 핸들러 */
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    /** OAuth2 로그인 실패 시 처리 핸들러 */
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    
    public SecurityConfig(
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAuthenticationFilter jwtAuthFilter,
            UserDetailsService userDetailsService,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
            OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler
    ) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
    }
    
    /**
     * Spring Security 필터 체인을 구성합니다.
     *
     * 보안 정책의 핵심으로, 인증/인가 규칙, 필터 순서, 예외 처리 등을 정의합니다.
     * JWT 토큰 기반 무상태 인증과 OAuth2 로그인을 동시에 지원합니다.
     *
     * @param http HttpSecurity 설정 객체
     * @return SecurityFilterChain 구성된 보안 필터 체인
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 비활성화 (JWT 사용으로 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 정책: 무상태(STATELESS) - JWT 토큰만 사용
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // HTTP 요청 인가 규칙 설정
                .authorizeHttpRequests(req -> req
                        // OPTIONS 요청은 모두 허용 (CORS preflight 요청)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 인증 관련 엔드포인트는 모두 허용
                        // - /auth/** : 회원가입, 로그인, 토큰 갱신 등
                        // - /admin/login, /admin/verify : 관리자 로그인
                        // - /oauth2/**, /login/oauth2/** : OAuth2 로그인 흐름
                        .requestMatchers(
                                "/auth/**",
                                "/admin/login",
                                "/admin/verify",
                                "/admin/promote",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // 사용자 등록 관련 공개 엔드포인트
                        // - 회원가입, 이메일 인증 등
                        .requestMatchers(
                                "/users/register",
                                "/users/verify-email/**",
                                "/users/verify-company-email/**"
                        ).permitAll()

                        // 모니터링 및 개발 도구 엔드포인트
                        // - Spring Actuator: 애플리케이션 모니터링
                        // - H2 Console: 개발환경 데이터베이스 접근
                        // - 기본 에러 페이지, 파비콘
                        .requestMatchers(
                                "/actuator/**",
                                "/h2-console/**",
                                "/error",
                                "/favicon.ico"
                        ).permitAll()

                        // OAuth2 관련 엔드포인트 (중복 제거 고려)
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // 공개 API - 조회만 허용
                        // - 채용공고, 게시글, 카테고리 조회
                        // - 고객지원 FAQ 조회
                        .requestMatchers(HttpMethod.GET,
                                "/job-postings/**",
                                "/posts/**",
                                "/categories/**",
                                "/api/support/faq/**",
                                "/api/support/categories"
                        ).permitAll()

                        // 개발 편의를 위한 임시 설정
                        // TODO: 프로덕션에서는 제거 필요
                        .requestMatchers(HttpMethod.POST, "/categories/**").permitAll()

                        // 관리자 전용 엔드포인트
                        // - 관리자 대시보드, 시스템 관리 기능
                        .requestMatchers("/dashboard/admin", "/admin/**", "/api/admin/**").hasRole("ADMIN")

                        // 기업 사용자 전용 엔드포인트
                        // - 기업 프로필 관리, 채용공고 등록 등
                        // - 관리자도 접근 가능
                        .requestMatchers("/company/**").hasAnyRole("COMPANY", "ADMIN")

                        // 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 인증 실패 시 예외 처리
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // 인증 제공자 설정 (DAO 기반)
                .authenticationProvider(authenticationProvider())

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                // 모든 요청에서 JWT 토큰을 먼저 검증
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // 사용자 정보 엔드포인트에서 커스텀 서비스 사용
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        // OAuth2 로그인 성공/실패 핸들러 설정
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )

                // H2 Console 사용을 위한 프레임 옵션 비활성화
                // 개발 환경에서만 필요
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        return http.build();
    }
    
    /**
     * CORS(Cross-Origin Resource Sharing) 설정을 구성합니다.
     *
     * 웹 브라우저의 동일 출처 정책(Same-Origin Policy)으로 인해 발생하는 제약을 해결하기 위해
     * 허용할 출처, HTTP 메서드, 헤더 등을 명시적으로 설정합니다.
     *
     * 설정 항목:
     * - 허용 출처: 개발/운영 환경의 프론트엔드 도메인들
     * - 허용 메서드: RESTful API의 모든 HTTP 메서드
     * - 허용 헤더: 클라이언트가 전송할 수 있는 모든 헤더
     * - 자격증명 허용: 쿠키, Authorization 헤더 전송 가능
     * - 노출 헤더: 클라이언트가 접근할 수 있는 응답 헤더
     *
     * @return CorsConfigurationSource CORS 설정이 적용된 구성 소스
     * @see CorsConfiguration
     * @see UrlBasedCorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 출처(Origin) 설정 - 개발 환경 및 프론트엔드 서버들
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",    // React 개발 서버 (기본)
                "http://localhost:3003",    // React 개발 서버 (대체)
                "http://localhost:5173",    // Vite 개발 서버
                "http://localhost:8081"     // 백엔드 서버 (개발용 동일 포트 접근)
        ));

        // 허용할 HTTP 메서드 설정 - RESTful API 전체 메서드 지원
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 요청 헤더 설정 - 모든 헤더 허용
        // 클라이언트가 Content-Type, Authorization 등 임의의 헤더 전송 가능
        configuration.setAllowedHeaders(List.of("*"));

        // 자격증명(Credentials) 허용 설정
        // 쿠키, Authorization 헤더, TLS 클라이언트 인증서 등 전송 가능
        configuration.setAllowCredentials(true);

        // 클라이언트에게 노출할 응답 헤더 설정
        // JavaScript에서 접근 가능한 헤더들을 명시
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // URL 패턴별 CORS 설정 등록
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // 모든 경로에 CORS 설정 적용
        return source;
    }
    
    /**
     * 데이터베이스 기반 인증 제공자를 구성합니다.
     *
     * DAO(Data Access Object) 패턴을 사용하여 사용자 정보를 데이터베이스에서 조회하고,
     * 제공된 자격증명(이메일, 비밀번호)을 검증합니다.
     *
     * 동작 과정:
     * 1. UserDetailsService를 통해 사용자 정보 로드
     * 2. PasswordEncoder를 사용하여 비밀번호 검증
     * 3. 검증 성공 시 Authentication 객체 생성
     * 4. Spring Security의 AuthenticationManager에서 사용
     *
     * @return AuthenticationProvider 구성된 DAO 기반 인증 제공자
     * @see DaoAuthenticationProvider
     * @see UserDetailsService
     * @see PasswordEncoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        // 사용자 정보를 로드할 서비스 설정
        // CustomUserDetailsService에서 이메일을 기반으로 사용자 정보 조회
        authProvider.setUserDetailsService(userDetailsService);

        // 비밀번호 인코더 설정
        // BCrypt 알고리즘을 사용하여 평문 비밀번호와 암호화된 비밀번호 비교
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }
    
    /**
     * Spring Security의 전역 인증 관리자를 구성합니다.
     *
     * AuthenticationManager는 Spring Security의 핵심 인증 컴포넌트로,
     * 다양한 AuthenticationProvider들을 관리하고 인증 요청을 처리합니다.
     *
     * 주요 역할:
     * - 인증 요청을 적절한 AuthenticationProvider에게 위임
     * - 인증 성공 시 Authentication 객체 반환
     * - 인증 실패 시 AuthenticationException 발생
     * - JWT 토큰 생성 시 사용자 인증 검증에 활용
     *
     * @param config Spring Security의 인증 설정 구성
     * @return AuthenticationManager 구성된 인증 관리자
     * @throws Exception 설정 중 발생할 수 있는 예외
     * @see AuthenticationConfiguration
     * @see AuthenticationProvider
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * 비밀번호 암호화를 위한 인코더를 구성합니다.
     *
     * BCrypt 해싱 알고리즘을 사용하여 사용자 비밀번호를 안전하게 암호화합니다.
     * BCrypt는 단방향 해시 함수로, 원본 비밀번호를 복원할 수 없으며
     * 솔트(salt)를 자동으로 생성하여 레인보우 테이블 공격을 방어합니다.
     *
     * 특징:
     * - 적응형 해시 함수: 시간이 지나도 보안 강도 유지 가능
     * - 솔트 자동 생성: 동일한 비밀번호도 다른 해시값 생성
     * - 느린 해싱: 무차별 대입 공격(brute force) 방어
     * - 업계 표준: Spring Security 기본 권장 인코더
     *
     * @return PasswordEncoder BCrypt 비밀번호 인코더
     * @see BCryptPasswordEncoder
     * @see PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}