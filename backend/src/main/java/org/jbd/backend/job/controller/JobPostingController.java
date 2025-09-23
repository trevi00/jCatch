package org.jbd.backend.job.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jbd.backend.auth.util.AuthenticationUtils;
import org.jbd.backend.common.dto.ApiResponse;
import org.jbd.backend.common.dto.PageResponse;
import org.jbd.backend.job.domain.enums.ExperienceLevel;
import org.jbd.backend.job.domain.enums.JobType;
import org.jbd.backend.job.dto.JobPostingCreateDto;
import org.jbd.backend.job.dto.JobPostingResponseDto;
import org.jbd.backend.job.dto.JobPostingSearchDto;
import org.jbd.backend.job.dto.JobPostingStatsDto;
import org.jbd.backend.job.dto.JobPostingUpdateDto;
import org.jbd.backend.job.domain.JobPosting;
import org.jbd.backend.job.service.JobPostingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 채용공고 관리 REST API 컨트롤러
 *
 * 채용공고의 등록, 수정, 삭제, 조회, 검색, 발행, 마감 등 채용공고와 관련된
 * 모든 기능을 제공합니다. 일반 사용자 및 기업 사용자, 관리자의 권한에 따라
 * 다양한 기능에 접근할 수 있으며, 고급 검색과 통계 기능도 포함합니다.
 *
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 * @see JobPostingService
 * @see JobPostingResponseDto
 * @see JobPostingCreateDto
 */
@RestController
@RequestMapping("/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

    /** 채용공고 비즈니스 로직을 처리하는 서비스 */
    private final JobPostingService jobPostingService;

    /** 인증 관련 유틸리티 */
    private final AuthenticationUtils authenticationUtils;

    /**
     * 새로운 채용공고를 등록합니다.
     * 기업 사용자만 채용공고를 등록할 수 있습니다.
     *
     * @param token Authorization 헤더 ("Bearer {token}" 형식)
     * @param dto 채용공고 생성 데이터
     *            - title: 채용공고 제목 (필수)
     *            - companyName: 회사명 (필수)
     *            - location: 근무지 (필수)
     *            - jobType: 직무 유형 (FULL_TIME, PART_TIME, CONTRACT 등)
     *            - experienceLevel: 경력 수준 (ENTRY, MID, SENIOR 등)
     *            - description: 상세 설명 (필수)
     *            - minSalary: 최소 연봉
     *            - maxSalary: 최대 연봉
     * @return ResponseEntity<ApiResponse<JobPostingResponseDto>> 등록된 채용공고 정보
     * @apiNote POST /job-postings
     * @see JobPostingCreateDto
     * @see JobPostingResponseDto
     */
    @PostMapping
    @PreAuthorize("isAuthenticated() and @permissionService.canCreateJobPosting(authentication)")
    public ResponseEntity<ApiResponse<JobPostingResponseDto>> createJobPosting(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody JobPostingCreateDto dto) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);

        JobPostingResponseDto response = jobPostingService.createJobPostingWithDto(
                userId, dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("채용공고가 등록되었습니다.", response));
    }

    /**
     * 특정 채용공고의 상세 정보를 조회합니다.
     * 조회 시 조회수가 자동으로 증가합니다.
     *
     * @param id 채용공고 ID
     * @return ResponseEntity<ApiResponse<JobPostingResponseDto>> 채용공고 상세 정보
     * @apiNote GET /job-postings/{id}
     * @see JobPostingResponseDto
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobPostingResponseDto>> getJobPosting(@PathVariable Long id) {
        JobPostingResponseDto response = jobPostingService.getJobPostingWithDto(id);
        return ResponseEntity.ok(ApiResponse.success("채용공고 조회 성공", response));
    }

    /**
     * 채용공고를 수정합니다.
     * 작성자 또는 관리자만 수정할 수 있습니다.
     *
     * @param token Authorization 헤더 ("Bearer {token}" 형식)
     * @param id 수정할 채용공고 ID
     * @param dto 채용공고 수정 데이터
     *            - title: 수정할 제목 (선택)
     *            - location: 수정할 근무지 (선택)
     *            - jobType: 수정할 직무 유형 (선택)
     *            - experienceLevel: 수정할 경력 수준 (선택)
     *            - description: 수정할 상세 설명 (선택)
     *            - minSalary, maxSalary: 수정할 급여 범위 (선택)
     * @return ResponseEntity<ApiResponse<JobPostingResponseDto>> 수정된 채용공고 정보
     * @apiNote PUT /job-postings/{id}
     * @see JobPostingUpdateDto
     * @see JobPostingResponseDto
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @permissionService.canUpdateJobPosting(authentication, #id)")
    public ResponseEntity<ApiResponse<JobPostingResponseDto>> updateJobPosting(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody JobPostingUpdateDto dto) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);

        JobPostingResponseDto response = jobPostingService.updateJobPostingWithPermissionCheck(userId, id, dto);

        return ResponseEntity.ok(ApiResponse.success("채용공고가 수정되었습니다.", response));
    }

    /**
     * 채용공고를 발행합니다.
     * 초기 등록된 채용공고를 공개 상태로 전환하여 사용자들이 볼 수 있도록 합니다.
     * 작성자 또는 관리자만 발행할 수 있습니다.
     *
     * @param token Authorization 헤더 ("Bearer {token}" 형식)
     * @param id 발행할 채용공고 ID
     * @param deadlineDate 채용 마감 일자 (지정한 날짜까지 지원 가능)
     * @return ResponseEntity<ApiResponse<JobPostingResponseDto>> 발행된 채용공고 정보
     * @apiNote POST /job-postings/{id}/publish
     * @see JobPostingResponseDto
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("isAuthenticated() and @permissionService.canUpdateJobPosting(authentication, #id)")
    public ResponseEntity<ApiResponse<JobPostingResponseDto>> publishJobPosting(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestParam LocalDate deadlineDate) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);

        JobPostingResponseDto response = jobPostingService.publishJobPostingWithPermissionCheck(userId, id, deadlineDate);
        return ResponseEntity.ok(ApiResponse.success("채용공고가 발행되었습니다.", response));
    }

    /**
     * 채용공고를 조기 마감합니다.
     * 예정된 마감 일자 이전에 채용을 완료한 경우 수동으로 마감처리합니다.
     * 작성자 또는 관리자만 마감할 수 있습니다.
     *
     * @param token Authorization 헤더 ("Bearer {token}" 형식)
     * @param id 마감할 채용공고 ID
     * @return ResponseEntity<ApiResponse<JobPostingResponseDto>> 마감된 채용공고 정보
     * @apiNote POST /job-postings/{id}/close
     * @see JobPostingResponseDto
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("isAuthenticated() and @permissionService.canUpdateJobPosting(authentication, #id)")
    public ResponseEntity<ApiResponse<JobPostingResponseDto>> closeJobPosting(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);

        JobPostingResponseDto response = jobPostingService.closeJobPostingWithPermissionCheck(userId, id);
        return ResponseEntity.ok(ApiResponse.success("채용공고가 마감되었습니다.", response));
    }

    /**
     * 공개된 채용공고 목록을 조회합니다.
     * 발행 상태인 채용공고들만 조회되며, 최신 등록순으로 정렬됩니다.
     *
     * @param pageable 페이지네이션 정보 (기본: 20개씩, 생성일 내림차순)
     * @return ResponseEntity<ApiResponse<PageResponse<JobPostingResponseDto>>> 채용공고 목록 (페이지네이션 적용)
     * @apiNote GET /job-postings
     * @see PageResponse
     * @see JobPostingResponseDto
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<JobPostingResponseDto>>> getPublishedJobPostings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<JobPosting> jobPostings = jobPostingService.getPublishedJobPostings(pageable);
        Page<JobPostingResponseDto> responseDtos = jobPostings.map(JobPostingResponseDto::from);

        return ResponseEntity.ok(ApiResponse.success("채용공고 목록 조회 성공", new PageResponse<>(responseDtos)));
    }

    /**
     * 기본 조건으로 채용공고를 검색합니다.
     * 근무지, 직무 유형, 경력 수준 등의 기본 필터로 검색할 수 있습니다.
     *
     * @param location 근무지 필터 (선택사항)
     * @param jobType 직무 유형 필터 (FULL_TIME, PART_TIME, CONTRACT 등, 선택사항)
     * @param experienceLevel 경력 수준 필터 (ENTRY, MID, SENIOR 등, 선택사항)
     * @param pageable 페이지네이션 정보
     * @return ResponseEntity<ApiResponse<PageResponse<JobPostingResponseDto>>> 검색된 채용공고 목록
     * @apiNote GET /job-postings/search
     * @see JobType
     * @see ExperienceLevel
     * @see PageResponse
     * @see JobPostingResponseDto
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<JobPostingResponseDto>>> searchJobPostings(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<JobPostingResponseDto> responseData = jobPostingService.searchJobPostingsWithDto(location, jobType, experienceLevel, pageable);
        return ResponseEntity.ok(ApiResponse.success("채용공고 검색 성공", responseData));
    }

    /**
     * 고급 조건으로 채용공고를 검색합니다.
     * 제목, 근무지, 직무 유형, 경력 수준, 급여 범위 등을 조합하여 상세한 검색이 가능합니다.
     *
     * @param title 채용공고 제목 검색어 (선택사항)
     * @param location 근무지 필터 (선택사항)
     * @param jobType 직무 유형 필터 (선택사항)
     * @param experienceLevel 경력 수준 필터 (선택사항)
     * @param minSalary 최소 급여 필터 (선택사항)
     * @param maxSalary 최대 급여 필터 (선택사항)
     * @param pageable 페이지네이션 정보
     * @return ResponseEntity<ApiResponse<PageResponse<JobPostingResponseDto>>> 고급 검색된 채용공고 목록
     * @apiNote GET /job-postings/search/advanced
     * @see JobType
     * @see ExperienceLevel
     * @see PageResponse
     * @see JobPostingResponseDto
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<ApiResponse<PageResponse<JobPostingResponseDto>>> searchJobPostingsAdvanced(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @RequestParam(required = false) Integer minSalary,
            @RequestParam(required = false) Integer maxSalary,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<JobPostingResponseDto> responseData = jobPostingService.searchJobPostingsAdvancedWithDto(
                title, location, jobType, experienceLevel, minSalary, maxSalary, pageable);
        return ResponseEntity.ok(ApiResponse.success("고급 채용공고 검색 성공", responseData));
    }

    /**
     * 키워드로 채용공고를 검색합니다.
     * 제목, 설명, 회사명 등에서 키워드를 포함하는 채용공고를 찾습니다.
     *
     * @param keyword 검색할 키워드 (필수)
     * @return ResponseEntity<ApiResponse<List<JobPostingResponseDto>>> 키워드 검색 결과 목록
     * @apiNote GET /job-postings/keyword
     * @see JobPostingResponseDto
     */
    @GetMapping("/keyword")
    public ResponseEntity<ApiResponse<List<JobPostingResponseDto>>> searchByKeyword(@RequestParam String keyword) {
        List<JobPostingResponseDto> responseDtos = jobPostingService.searchJobPostingsByKeywordWithDto(keyword);
        return ResponseEntity.ok(ApiResponse.success("키워드 검색 성공", responseDtos));
    }

    /**
     * 내가 등록한 채용공고 목록을 조회합니다.
     * 인증된 기업 사용자의 모든 채용공고(발행/미발행 포함)를 조회합니다.
     *
     * @param token Authorization 헤더 ("Bearer {token}" 형식)
     * @return ResponseEntity<ApiResponse<List<JobPostingResponseDto>>> 내 채용공고 목록
     * @apiNote GET /job-postings/my-postings
     * @see JobPostingResponseDto
     */
    @GetMapping("/my-postings")
    public ResponseEntity<ApiResponse<List<JobPostingResponseDto>>> getMyJobPostings(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);

        List<JobPostingResponseDto> responseDtos = jobPostingService.getMyJobPostingsWithDto(userId);
        return ResponseEntity.ok(ApiResponse.success("내 채용공고 목록 조회 성공", responseDtos));
    }

    /**
     * 채용공고를 삭제합니다.
     * 작성자 또는 관리자만 삭제할 수 있습니다. 삭제된 채용공고는 복구할 수 없습니다.
     *
     * @param token Authorization 헤더 ("Bearer {token}" 형식)
     * @param id 삭제할 채용공고 ID
     * @return ResponseEntity<ApiResponse<Void>> 삭제 완료 응답
     * @apiNote DELETE /job-postings/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @permissionService.canDeleteJobPosting(authentication, #id)")
    public ResponseEntity<ApiResponse<Void>> deleteJobPosting(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);

        jobPostingService.deleteJobPostingWithPermissionCheck(userId, id);
        return ResponseEntity.ok(ApiResponse.success("채용공고가 삭제되었습니다."));
    }

    // ====== JobAtda 통합 신규 엔드포인트들 ======

    /**
     * 고급 검색 API (POST - 복잡한 검색 조건)
     */
    @PostMapping("/search/advanced")
    public ResponseEntity<ApiResponse<PageResponse<JobPostingResponseDto>>> searchJobPostingsAdvanced(
            @Valid @RequestBody JobPostingSearchDto searchDto,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<JobPostingResponseDto> responseData = jobPostingService.searchJobPostingsWithSpecificationAndDto(searchDto, pageable);
        return ResponseEntity.ok(ApiResponse.success("고급 검색 성공", responseData));
    }

    /**
     * 간편 검색 API (GET - 키워드 + 기본 필터)
     */
    @GetMapping("/search/simple")
    public ResponseEntity<ApiResponse<PageResponse<JobPostingResponseDto>>> searchJobPostingsSimple(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // Service 메서드에서 직접 DTO 변환까지 처리하도록 변경
        Page<JobPostingResponseDto> responseDtos = jobPostingService.searchJobPostingsSimpleWithDto(
                keyword, location, jobType, experienceLevel, pageable);

        return ResponseEntity.ok(ApiResponse.success("간편 검색 성공", new PageResponse<>(responseDtos)));
    }

    /**
     * 마감 임박 채용공고 조회
     */
    @GetMapping("/deadline-approaching")
    public ResponseEntity<ApiResponse<List<JobPostingResponseDto>>> getDeadlineApproachingJobPostings(
            @RequestParam(defaultValue = "7") int days) {

        List<JobPostingResponseDto> responseDtos = jobPostingService.getDeadlineApproachingJobPostingsWithDto(days);
        return ResponseEntity.ok(ApiResponse.success(
                days + "일 이내 마감 임박 채용공고 조회 성공", responseDtos));
    }

    /**
     * 내 채용공고 중 마감 임박한 것들 조회
     */
    @GetMapping("/my-postings/deadline-approaching")
    public ResponseEntity<ApiResponse<List<JobPostingResponseDto>>> getMyDeadlineApproachingJobPostings(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "7") int days) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);

        List<JobPostingResponseDto> responseDtos = jobPostingService.getMyDeadlineApproachingJobPostingsWithDto(userId, days);
        return ResponseEntity.ok(ApiResponse.success(
                "내 채용공고 중 " + days + "일 이내 마감 임박 공고 조회 성공", responseDtos));
    }

    /**
     * 채용공고 통계 조회
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<JobPostingStatsDto>> getJobPostingStats(@PathVariable Long id) {
        JobPostingStatsDto stats = jobPostingService.getJobPostingStats(id);
        return ResponseEntity.ok(ApiResponse.success("채용공고 통계 조회 성공", stats));
    }

    /**
     * 내 채용공고 통계 목록 조회
     */
    @GetMapping("/my-postings/stats")
    public ResponseEntity<ApiResponse<List<JobPostingStatsDto>>> getMyJobPostingStats(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);

        List<JobPostingStatsDto> stats = jobPostingService.getMyJobPostingStatsWithDto(userId);
        return ResponseEntity.ok(ApiResponse.success("내 채용공고 통계 목록 조회 성공", stats));
    }

    /**
     * 전체 채용공고 통계 (관리자용)
     */
    @GetMapping("/admin/stats")
    public ResponseEntity<ApiResponse<Object>> getOverallJobPostingStatistics(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);
        String userTypeFromToken = authenticationUtils.extractUserTypeFromHeader(authHeader);
        Boolean isAdmin = authenticationUtils.extractIsAdminFromHeader(authHeader);

        Object stats = jobPostingService.getOverallJobPostingStatisticsWithPermissionCheck(userTypeFromToken, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("전체 채용공고 통계 조회 성공", stats));
    }
}