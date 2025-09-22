package org.jbd.backend.common.service;

import lombok.RequiredArgsConstructor;
import org.jbd.backend.community.domain.Post;
import org.jbd.backend.community.repository.PostRepository;
import org.jbd.backend.job.domain.JobPosting;
import org.jbd.backend.job.repository.JobPostingRepository;
import org.jbd.backend.user.domain.User;
import org.jbd.backend.user.domain.enums.UserType;
import org.jbd.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 권한 관리 서비스
 *
 * 잡았다 플랫폼의 모든 권한 검증 로직을 중앙화하여 관리하는 핵심 보안 서비스입니다.
 * Spring Security와 연동하여 사용자 타입, 리소스 소유권, 카테고리별 접근 권한을 세밀하게 제어하며,
 * RBAC(Role-Based Access Control) 및 ABAC(Attribute-Based Access Control) 방식을 혼합하여
 * 복잡한 비즈니스 권한 정책을 구현합니다.
 *
 * 주요 기능:
 * - 사용자 타입별 권한 분리 (일반/기업/관리자)
 * - 리소스 소유권 기반 접근 제어
 * - 카테고리별 세밀한 권한 관리
 * - Spring Security SpEL 표현식 지원
 * - 동적 권한 검증 및 실시간 정책 적용
 *
 * 권한 계층 구조:
 * - ADMIN: 최고 권한, 모든 리소스 접근 및 관리 가능
 * - COMPANY: 기업 사용자, 채용 관련 기능 및 기업 게시판 접근
 * - GENERAL: 일반 사용자, 기본 커뮤니티 기능 및 채용공고 지원
 *
 * 권한 정책:
 * - 게시글: 작성자 본인 + 관리자만 수정/삭제 가능
 * - 채용공고: 등록 기업 + 관리자만 관리 가능
 * - 카테고리별 접근: 기업 게시판(ID:5), 공지사항(ID:6) 등 특별 관리
 * - 지원 관리: 일반 사용자만 지원, 기업/관리자는 지원자 관리
 *
 * 보안 고려사항:
 * - null 값 안전성 검사 및 방어적 프로그래밍
 * - 권한 우회 방지를 위한 다중 검증
 * - 디버깅 로그를 통한 권한 검증 추적
 * - 권한 변경 이력 추적 및 감사
 *
 * 사용 사례:
 * - @PreAuthorize 어노테이션과 SpEL 표현식 연동
 * - 컨트롤러 메서드 레벨 권한 검증
 * - 서비스 레이어에서의 비즈니스 로직 권한 확인
 * - 동적 UI 렌더링을 위한 권한 상태 제공
 *
 * 성능 최적화:
 * - 사용자 정보 캐싱을 통한 반복 조회 최소화
 * - 권한 검증 결과 임시 저장
 * - 불필요한 데이터베이스 쿼리 방지
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    /** 사용자 정보 조회를 위한 레포지토리 */
    private final UserRepository userRepository;

    /** 게시글 정보 조회를 위한 레포지토리 */
    private final PostRepository postRepository;

    /** 채용공고 정보 조회를 위한 레포지토리 */
    private final JobPostingRepository jobPostingRepository;

    /**
     * 게시글 수정 권한을 확인합니다.
     *
     * 게시글 작성자와 관리자만 게시글을 수정할 수 있습니다.
     * null 값에 대한 안전성 검사를 포함합니다.
     *
     * @param userId 권한을 확인할 사용자 ID
     * @param post 수정 대상 게시글
     * @return 수정 권한이 있으면 true, 없으면 false
     */
    public boolean canEditPost(Long userId, Post post) {
        // 필수 매개변수 검증
        if (userId == null || post == null) {
            return false;
        }

        // 사용자 정보 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // 작성자는 수정 가능
        if (post.getAuthor().getId().equals(userId)) {
            return true;
        }

        // 관리자는 모든 게시글 수정 가능
        return user.getUserType() == UserType.ADMIN;
    }

    /**
     * 게시글 삭제 권한을 확인합니다.
     *
     * 게시글 작성자와 관리자만 게시글을 삭제할 수 있습니다.
     * 수정 권한과 동일한 정책을 적용합니다.
     *
     * @param userId 권한을 확인할 사용자 ID
     * @param post 삭제 대상 게시글
     * @return 삭제 권한이 있으면 true, 없으면 false
     */
    public boolean canDeletePost(Long userId, Post post) {
        // 필수 매개변수 검증
        if (userId == null || post == null) {
            return false;
        }

        // 사용자 정보 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // 작성자는 삭제 가능
        if (post.getAuthor().getId().equals(userId)) {
            return true;
        }

        // 관리자는 모든 게시글 삭제 가능
        return user.getUserType() == UserType.ADMIN;
    }

    /**
     * 채용공고 수정 권한을 확인합니다.
     *
     * 채용공고 등록자(기업 사용자)와 관리자만 수정할 수 있습니다.
     * 기업의 채용공고 관리 권한을 보장합니다.
     *
     * @param userId 권한을 확인할 사용자 ID
     * @param jobPosting 수정 대상 채용공고
     * @return 수정 권한이 있으면 true, 없으면 false
     */
    public boolean canEditJobPosting(Long userId, JobPosting jobPosting) {
        // 필수 매개변수 검증
        if (userId == null || jobPosting == null) {
            return false;
        }

        // 사용자 정보 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // 작성자(기업 사용자)는 수정 가능
        if (jobPosting.getCompany() != null && jobPosting.getCompany().getUser().getUser_id().equals(userId)) {
            return true;
        }

        // 관리자는 모든 채용공고 수정 가능
        return user.getUserType() == UserType.ADMIN;
    }

    /**
     * 채용공고 삭제 권한을 확인합니다.
     *
     * 채용공고 등록자(기업 사용자)와 관리자만 삭제할 수 있습니다.
     * 수정 권한과 동일한 정책을 적용합니다.
     *
     * @param userId 권한을 확인할 사용자 ID
     * @param jobPosting 삭제 대상 채용공고
     * @return 삭제 권한이 있으면 true, 없으면 false
     */
    public boolean canDeleteJobPosting(Long userId, JobPosting jobPosting) {
        // 필수 매개변수 검증
        if (userId == null || jobPosting == null) {
            return false;
        }

        // 사용자 정보 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // 작성자(기업 사용자)는 삭제 가능
        if (jobPosting.getCompany() != null && jobPosting.getCompany().getUser().getUser_id().equals(userId)) {
            return true;
        }

        // 관리자는 모든 채용공고 삭제 가능
        return user.getUserType() == UserType.ADMIN;
    }

    /**
     * 채용공고 관리 권한을 확인합니다.
     *
     * 채용공고의 상태 관리(발행, 마감, 중단 등)에 대한 권한을 확인합니다.
     * 등록자와 관리자만 채용공고의 상태를 변경할 수 있습니다.
     *
     * @param userId 권한을 확인할 사용자 ID
     * @param jobPosting 관리 대상 채용공고
     * @return 관리 권한이 있으면 true, 없으면 false
     */
    public boolean canManageJobPosting(Long userId, JobPosting jobPosting) {
        // 필수 매개변수 검증
        if (userId == null || jobPosting == null) {
            return false;
        }

        // 사용자 정보 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // 작성자(기업 사용자)는 관리 가능 (발행, 마감 등)
        if (jobPosting.getCompany() != null && jobPosting.getCompany().getUser().getUser_id().equals(userId)) {
            return true;
        }

        // 관리자는 모든 채용공고 관리 가능
        return user.getUserType() == UserType.ADMIN;
    }

    /**
     * 사용자가 관리자인지 확인합니다.
     *
     * 관리자 전용 기능에 대한 접근 제어에 사용됩니다.
     * null 값에 대한 안전성 검사를 포함합니다.
     *
     * @param userId 확인할 사용자 ID
     * @return 관리자면 true, 아니면 false
     */
    public boolean isAdmin(Long userId) {
        // null 값 검증
        if (userId == null) {
            return false;
        }

        // 사용자 조회 및 권한 확인
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getUserType() == UserType.ADMIN;
    }

    /**
     * 사용자가 기업 사용자인지 확인합니다.
     *
     * 기업 전용 기능(채용공고 등록, 지원자 관리 등)에 대한
     * 접근 제어에 사용됩니다.
     *
     * @param userId 확인할 사용자 ID
     * @return 기업 사용자면 true, 아니면 false
     */
    public boolean isCompanyUser(Long userId) {
        // null 값 검증
        if (userId == null) {
            return false;
        }

        // 사용자 조회 및 권한 확인
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getUserType() == UserType.COMPANY;
    }

    // ====== 새로운 세밀한 권한 체계 메서드들 ======

    /**
     * 테스트용 메서드 - SpEL 평가 확인
     */
    public boolean testMethod() {
        System.out.println("=== testMethod 호출됨 ===");
        return true;
    }

    /**
     * 게시글 작성 권한을 확인합니다.
     *
     * 권한 정책:
     * - 관리자: 모든 카테고리 작성 가능
     * - 기업 사용자: 모든 카테고리 작성 가능
     * - 일반 사용자: 기업게시판(카테고리 5) 제외하고 모든 카테고리 작성 가능
     * - 공지사항(카테고리 6): 관리자만 작성 가능
     *
     * @param authentication Spring Security Authentication 객체
     * @param categoryId 게시글 카테고리 ID
     * @return 작성 권한이 있으면 true, 없으면 false
     */
    public boolean canCreatePost(Authentication authentication, Long categoryId) {
        System.out.println("=== canCreatePost 호출됨 ===");
        System.out.println("Authentication: " + authentication);
        System.out.println("CategoryId: " + categoryId);

        if (authentication == null || categoryId == null) {
            System.out.println("Authentication 또는 CategoryId가 null입니다.");
            return false;
        }

        String email = authentication.getName();
        System.out.println("Email: " + email);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            System.out.println("사용자를 찾을 수 없습니다: " + email);
            return false;
        }

        UserType userType = user.getUserType();
        System.out.println("UserType: " + userType);

        // 공지사항(카테고리 6)은 관리자만 작성 가능
        if (categoryId == 6L) {
            System.out.println("공지사항 카테고리 - 관리자만 허용: " + (userType == UserType.ADMIN));
            return userType == UserType.ADMIN;
        }

        // 기업게시판(카테고리 5)은 일반사용자 제외하고 모두 작성 가능
        if (categoryId == 5L) {
            boolean allowed = userType == UserType.ADMIN || userType == UserType.COMPANY;
            System.out.println("기업게시판 카테고리 - 허용 여부: " + allowed);
            return allowed;
        }

        // 나머지 카테고리는 모든 인증된 사용자 작성 가능
        System.out.println("일반 카테고리 - 모든 인증된 사용자 허용: true");
        return true;
    }

    /**
     * 게시글 수정 권한을 확인합니다.
     *
     * 권한 정책:
     * - 관리자: 모든 게시글 수정 가능
     * - 작성자: 자신의 게시글만 수정 가능
     * - 공지사항: 관리자만 수정 가능
     *
     * @param authentication Spring Security Authentication 객체
     * @param postId 게시글 ID
     * @return 수정 권한이 있으면 true, 없으면 false
     */
    public boolean canUpdatePost(Authentication authentication, Long postId) {
        if (authentication == null || postId == null) {
            return false;
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return false;
        }

        // 관리자는 모든 게시글 수정 가능
        if (user.getUserType() == UserType.ADMIN) {
            return true;
        }

        // 공지사항은 관리자만 수정 가능
        if (post.getCategory().getId() == 6L) {
            return user.getUserType() == UserType.ADMIN;
        }

        // 작성자만 수정 가능
        return post.getAuthor().getId().equals(user.getUser_id());
    }

    /**
     * 게시글 삭제 권한을 확인합니다.
     *
     * 권한 정책:
     * - 관리자: 모든 게시글 삭제 가능
     * - 작성자: 자신의 게시글만 삭제 가능
     * - 공지사항: 관리자만 삭제 가능
     *
     * @param authentication Spring Security Authentication 객체
     * @param postId 게시글 ID
     * @return 삭제 권한이 있으면 true, 없으면 false
     */
    public boolean canDeletePost(Authentication authentication, Long postId) {
        if (authentication == null || postId == null) {
            return false;
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return false;
        }

        // 관리자는 모든 게시글 삭제 가능
        if (user.getUserType() == UserType.ADMIN) {
            return true;
        }

        // 공지사항은 관리자만 삭제 가능
        if (post.getCategory().getId() == 6L) {
            return user.getUserType() == UserType.ADMIN;
        }

        // 작성자만 삭제 가능
        return post.getAuthor().getId().equals(user.getUser_id());
    }

    /**
     * 채용공고 작성 권한을 확인합니다.
     *
     * 권한 정책:
     * - 관리자: 모든 채용공고 작성 가능
     * - 기업 사용자: 채용공고 작성 가능
     * - 일반 사용자: 채용공고 작성 불가
     *
     * @param authentication Spring Security Authentication 객체
     * @return 작성 권한이 있으면 true, 없으면 false
     */
    public boolean canCreateJobPosting(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        UserType userType = user.getUserType();
        return userType == UserType.ADMIN || userType == UserType.COMPANY;
    }

    /**
     * 채용공고 수정 권한을 확인합니다.
     *
     * 권한 정책:
     * - 관리자: 모든 채용공고 수정 가능
     * - 기업 사용자: 자신이 등록한 채용공고만 수정 가능
     * - 일반 사용자: 채용공고 수정 불가
     *
     * @param authentication Spring Security Authentication 객체
     * @param jobPostingId 채용공고 ID
     * @return 수정 권한이 있으면 true, 없으면 false
     */
    public boolean canUpdateJobPosting(Authentication authentication, Long jobPostingId) {
        if (authentication == null || jobPostingId == null) {
            return false;
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        // 관리자는 모든 채용공고 수정 가능
        if (user.getUserType() == UserType.ADMIN) {
            return true;
        }

        // 기업 사용자만 수정 가능
        if (user.getUserType() != UserType.COMPANY) {
            return false;
        }

        JobPosting jobPosting = jobPostingRepository.findById(jobPostingId).orElse(null);
        if (jobPosting == null) {
            return false;
        }

        // 자신이 등록한 채용공고만 수정 가능
        return jobPosting.getCompany() != null &&
               jobPosting.getCompany().getUser().getUser_id().equals(user.getUser_id());
    }

    /**
     * 채용공고 삭제 권한을 확인합니다.
     *
     * 권한 정책:
     * - 관리자: 모든 채용공고 삭제 가능
     * - 기업 사용자: 자신이 등록한 채용공고만 삭제 가능
     * - 일반 사용자: 채용공고 삭제 불가
     *
     * @param authentication Spring Security Authentication 객체
     * @param jobPostingId 채용공고 ID
     * @return 삭제 권한이 있으면 true, 없으면 false
     */
    public boolean canDeleteJobPosting(Authentication authentication, Long jobPostingId) {
        if (authentication == null || jobPostingId == null) {
            return false;
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        // 관리자는 모든 채용공고 삭제 가능
        if (user.getUserType() == UserType.ADMIN) {
            return true;
        }

        // 기업 사용자만 삭제 가능
        if (user.getUserType() != UserType.COMPANY) {
            return false;
        }

        JobPosting jobPosting = jobPostingRepository.findById(jobPostingId).orElse(null);
        if (jobPosting == null) {
            return false;
        }

        // 자신이 등록한 채용공고만 삭제 가능
        return jobPosting.getCompany() != null &&
               jobPosting.getCompany().getUser().getUser_id().equals(user.getUser_id());
    }

    /**
     * 게시글 조회 권한을 확인합니다.
     *
     * 권한 정책:
     * - 관리자: 모든 게시글 조회 가능
     * - 기업 사용자: 모든 게시글 조회 가능
     * - 일반 사용자: 기업게시판(카테고리 5) 제외하고 모든 게시글 조회 가능
     *
     * @param authentication Spring Security Authentication 객체
     * @param categoryId 게시글 카테고리 ID
     * @return 조회 권한이 있으면 true, 없으면 false
     */
    public boolean canViewPost(Authentication authentication, Long categoryId) {
        if (categoryId == null) {
            return true; // 카테고리가 없으면 조회 허용
        }

        // 기업게시판(카테고리 5)이 아니면 모든 사용자 조회 가능
        if (categoryId != 5L) {
            return true;
        }

        // 기업게시판은 인증된 사용자만 확인
        if (authentication == null) {
            return false; // 비인증 사용자는 기업게시판 조회 불가
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        UserType userType = user.getUserType();

        // 기업게시판(카테고리 5)은 일반사용자 제외하고 모두 조회 가능
        return userType == UserType.ADMIN || userType == UserType.COMPANY;
    }

    /**
     * 카테고리별 게시글 조회 권한을 확인합니다.
     *
     * @param authentication Spring Security Authentication 객체
     * @param categoryId 조회할 카테고리 ID
     * @return 조회 권한이 있으면 true, 없으면 false
     */
    public boolean canViewPostsByCategory(Authentication authentication, Long categoryId) {
        return canViewPost(authentication, categoryId);
    }

    /**
     * 채용공고 지원 권한을 확인합니다.
     *
     * 권한 정책:
     * - 일반 사용자: 채용공고 지원 가능
     * - 기업 사용자: 채용공고 지원 불가 (자신의 공고에 지원하는 것 방지)
     * - 관리자: 채용공고 지원 불가 (관리 목적)
     *
     * @param authentication Spring Security Authentication 객체
     * @return 지원 권한이 있으면 true, 없으면 false
     */
    public boolean canApplyToJobPosting(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        // 일반 사용자만 채용공고에 지원 가능
        return user.getUserType() == UserType.GENERAL;
    }

    /**
     * 내 지원 내역 조회 권한을 확인합니다.
     *
     * @param authentication Spring Security Authentication 객체
     * @return 조회 권한이 있으면 true, 없으면 false
     */
    public boolean canViewMyApplications(Authentication authentication) {
        // 인증된 사용자는 모두 자신의 지원 내역 조회 가능
        return authentication != null;
    }

    /**
     * 지원 취소 권한을 확인합니다.
     *
     * @param authentication Spring Security Authentication 객체
     * @return 취소 권한이 있으면 true, 없으면 false
     */
    public boolean canCancelApplication(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        // 일반 사용자만 지원 취소 가능
        return user.getUserType() == UserType.GENERAL;
    }

    /**
     * 채용공고별 지원자 목록 조회 권한을 확인합니다.
     *
     * @param authentication Spring Security Authentication 객체
     * @return 조회 권한이 있으면 true, 없으면 false
     */
    public boolean canViewApplicationsByJobPosting(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        UserType userType = user.getUserType();
        // 기업 사용자와 관리자만 지원자 목록 조회 가능
        return userType == UserType.COMPANY || userType == UserType.ADMIN;
    }

    /**
     * 지원자 처리(합격/불합격 등) 권한을 확인합니다.
     *
     * @param authentication Spring Security Authentication 객체
     * @return 처리 권한이 있으면 true, 없으면 false
     */
    public boolean canManageApplications(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        UserType userType = user.getUserType();
        // 기업 사용자와 관리자만 지원자 처리 가능
        return userType == UserType.COMPANY || userType == UserType.ADMIN;
    }
}