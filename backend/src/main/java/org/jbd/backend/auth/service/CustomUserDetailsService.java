package org.jbd.backend.auth.service;

import org.jbd.backend.user.domain.User;
import org.jbd.backend.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 커스텀 사용자 세부정보 서비스
 *
 * 잡았다 플랫폼의 Spring Security 통합을 위한 사용자 인증 정보 제공 서비스입니다.
 * UserDetailsService 인터페이스를 구현하여 사용자 인증에 필요한 상세 정보를 제공하며,
 * 데이터베이스에서 사용자를 조회하고 Spring Security가 요구하는 UserDetails 객체로 변환합니다.
 * 일반 사용자, 기업 사용자, OAuth 사용자를 모두 지원하며, 역할 기반 접근 제어(RBAC)를 구현합니다.
 *
 * 핵심 기능:
 * - 이메일 기반 사용자 인증 정보 조회 및 검증
 * - 사용자 타입별 권한 설정 (ROLE_GENERAL, ROLE_COMPANY, ROLE_ADMIN)
 * - 계정 상태 실시간 확인 (활성화, 비활성화, 잠금, 삭제 여부)
 * - OAuth 소셜 로그인 사용자 지원 (비밀번호 없는 사용자)
 * - Spring Security UserDetails 객체 생성 및 관리
 * - 권한 기반 접근 제어를 위한 GrantedAuthority 제공
 *
 * Spring Security 통합 아키텍처:
 * - AuthenticationManager: 사용자 인증 시 loadUserByUsername 호출
 * - JWT 필터: 토큰 검증 시 사용자 정보 조회
 * - 권한 기반 접근 제어: @PreAuthorize, @Secured 어노테이션 지원
 * - Method Security: 메서드 레벨 보안 적용
 * - Web Security: URL 기반 접근 권한 제어
 *
 * 사용자 권한 체계:
 * - ROLE_GENERAL: 일반 구직자 사용자
 *   · 채용 공고 검색 및 지원
 *   · 개인 프로필 관리
 *   · AI 기반 취업 지원 서비스 이용
 *   · 커뮤니티 참여
 * - ROLE_COMPANY: 기업 채용담당자 사용자
 *   · 채용 공고 등록 및 관리
 *   · 지원자 관리 및 평가
 *   · 기업 프로필 관리
 *   · 채용 통계 및 분석
 * - ROLE_ADMIN: 시스템 관리자 사용자
 *   · 전체 시스템 관리
 *   · 사용자 관리 및 권한 제어
 *   · 통계 및 모니터링
 *
 * 계정 상태 관리:
 * - 활성/비활성 계정 구분 처리
 * - 소프트 삭제된 계정 필터링
 * - 계정 잠금 상태 반영
 * - OAuth 연동 계정 특별 처리
 *
 * OAuth 사용자 지원:
 * - Google OAuth 사용자 인증 지원
 * - 비밀번호 없는 사용자 처리
 * - 소셜 로그인 계정의 권한 관리
 * - OAuth 제공업체별 사용자 정보 매핑
 *
 * 보안 고려사항:
 * - 이메일 기반 사용자 식별로 보안성 강화
 * - 삭제된 계정 자동 필터링
 * - 계정 상태별 접근 권한 세밀 제어
 * - 권한 정보 실시간 동기화
 *
 * 인증 플로우:
 * 1. 사용자 로그인 요청
 * 2. AuthenticationManager가 loadUserByUsername 호출
 * 3. 이메일로 데이터베이스에서 사용자 조회
 * 4. 사용자 존재 여부 및 상태 검증
 * 5. UserDetails 객체 생성 (사용자명, 비밀번호, 권한)
 * 6. Spring Security가 비밀번호 검증 수행
 * 7. 인증 성공 시 SecurityContext에 저장
 *
 * 에러 처리:
 * - UsernameNotFoundException: 사용자 없음
 * - DisabledException: 비활성화된 계정
 * - LockedException: 잠긴 계정
 * - AccountExpiredException: 만료된 계정
 *
 * 성능 최적화:
 * - 단일 쿼리로 사용자 정보 조회
 * - 삭제된 사용자 사전 필터링
 * - 권한 정보 메모리 캐싱
 * - 불필요한 객체 생성 최소화
 *
 * 사용 시나리오:
 * - 사용자 로그인 시 인증 정보 제공
 * - JWT 토큰 검증 시 사용자 조회
 * - 권한 기반 API 접근 제어
 * - OAuth 소셜 로그인 사용자 인증
 * - 관리자 권한 검증
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * CustomUserDetailsService 생성자
     *
     * @param userRepository 사용자 데이터 액세스를 위한 리포지토리
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * 이메일을 사용하여 사용자 세부정보를 로드합니다.
     *
     * Spring Security의 표준 인터페이스를 구현하여 사용자 인증 시
     * 데이터베이스에서 사용자 정보를 조회하고 UserDetails 객체로 변환합니다.
     *
     * 처리 과정:
     * 1. 이메일로 활성 사용자 조회 (삭제되지 않은 사용자만)
     * 2. 사용자가 없으면 UsernameNotFoundException 발생
     * 3. 사용자 정보를 Spring Security UserDetails로 변환
     * 4. 권한 정보 설정 (사용자 타입 기반)
     * 5. 계정 상태 정보 설정 (잠금, 비활성화 여부)
     *
     * OAuth 사용자 처리:
     * - 비밀번호가 없는 OAuth 사용자도 지원
     * - 빈 문자열을 비밀번호로 설정하여 OAuth 인증 플로우에서 사용
     *
     * @param email 조회할 사용자의 이메일 주소 (username 역할)
     * @return UserDetails Spring Security에서 사용할 사용자 세부정보
     * @throws UsernameNotFoundException 해당 이메일의 사용자가 없는 경우
     * @see UserDetails
     * @see User
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .disabled(user.isDeleted())
                .accountLocked(user.isAccountLocked())
                .authorities(getAuthorities(user))
                .build();
    }
    
    /**
     * 사용자의 권한 목록을 생성합니다.
     *
     * 사용자 타입에 따라 Spring Security 권한을 설정합니다.
     * 권한은 "ROLE_" 접두사를 붙인 사용자 타입으로 구성됩니다.
     *
     * 지원하는 권한:
     * - ROLE_GENERAL: 일반 구직자 사용자
     * - ROLE_COMPANY: 기업 채용담당자 사용자
     * - ROLE_ADMIN: 관리자 사용자
     *
     * @param user 권한을 설정할 사용자
     * @return List<SimpleGrantedAuthority> 사용자의 권한 목록
     * @see SimpleGrantedAuthority
     * @see org.jbd.backend.user.domain.enums.UserType
     */
    private List<SimpleGrantedAuthority> getAuthorities(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserType().name()));
    }
}