package org.jbd.backend.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.jbd.backend.common.exception.BusinessException;
import org.jbd.backend.common.exception.ErrorCode;
import org.jbd.backend.common.service.PermissionService;
import org.jbd.backend.job.domain.JobApplication;
import org.jbd.backend.job.domain.JobPosting;
import org.jbd.backend.job.domain.enums.ExperienceLevel;
import org.jbd.backend.job.domain.enums.JobType;
import org.jbd.backend.job.dto.JobPostingCreateDto;
import org.jbd.backend.job.dto.JobPostingResponseDto;
import org.jbd.backend.job.dto.JobPostingSearchDto;
import org.jbd.backend.job.dto.JobPostingStatsDto;
import org.jbd.backend.job.dto.JobPostingUpdateDto;
import org.jbd.backend.common.dto.PageResponse;
import org.jbd.backend.job.repository.JobApplicationRepository;
import org.jbd.backend.job.repository.JobPostingRepository;
import org.jbd.backend.job.specification.JobPostingSpecification;
import org.jbd.backend.user.domain.User;
import org.jbd.backend.user.repository.UserRepository;
import org.jbd.backend.company.domain.Company;
import org.jbd.backend.company.repository.CompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 채용공고 서비스
 *
 * 채용공고의 생성, 조회, 수정, 삭제 및 검색 기능을 제공하는 서비스입니다.
 * 기업의 채용 프로세스 전반을 관리하고 구직자에게 맞춤형 채용정보를 제공합니다.
 *
 * 주요 기능:
 * - 채용공고 CRUD 작업 (생성, 조회, 수정, 삭제)
 * - 고도화된 검색 기능 (지역, 직무유형, 경력, 연봉 등)
 * - 채용공고 상태 관리 (초안, 발행, 마감, 만료)
 * - 지원자 통계 및 분석 데이터 제공
 * - 마감 임박 채용공고 모니터링
 * - 조회수 및 인기도 추적
 *
 * 검색 기능:
 * - 기본 검색: 지역, 직무유형, 경력수준별 필터링
 * - 고급 검색: 키워드, 연봉범위, 자격요건 등을 이용한 상세 검색
 * - 전문검색: JPA Specification을 활용한 복합 조건 검색
 *
 * 비즈니스 규칙:
 * - 오직 기업 사용자만 채용공고 작성 가능
 * - 채용공고 작성자 또는 관리자만 수정/삭제 가능
 * - 채용공고 상태 전환 규칙 (초안 → 발행 → 마감)
 * - 마감일 자동 처리 및 알림
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PermissionService permissionService;

    /**
     * 채용공고를 생성합니다.
     *
     * 기업 사용자가 새로운 채용공고를 작성합니다.
     * 기업 권한 확인 및 기업 프로필 연동을 통해 채용공고를 등록합니다.
     *
     * @param userId 채용공고를 작성할 사용자 ID (기업 사용자)
     * @param title 채용공고 제목
     * @param companyName 회사명
     * @param location 근무지역
     * @param jobType 근무형태 (FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP)
     * @param experienceLevel 경력수준 (ENTRY, JUNIOR, MID_LEVEL, SENIOR, EXECUTIVE)
     * @param description 채용공고 상세 설명
     * @param minSalary 최소 연봉 (만원)
     * @param maxSalary 최대 연봉 (만원)
     * @return JobPosting 생성된 채용공고
     * @throws IllegalArgumentException 사용자나 기업 프로필을 찾을 수 없거나 기업 사용자가 아닌 경우
     */
    @Transactional
    public JobPosting createJobPosting(Long userId, String title, String companyName, String location,
                                       JobType jobType, ExperienceLevel experienceLevel,
                                       String description, Integer minSalary, Integer maxSalary) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 사용자가 기업 유형인지 확인
        if (!user.isCompanyUser()) {
            throw new IllegalArgumentException("기업 사용자만 채용공고를 작성할 수 있습니다.");
        }

        // 사용자와 연결된 Company 조회
        Company company = companyRepository.findByUser_userId(userId)
                .orElseThrow(() -> new IllegalArgumentException("기업 프로필이 없습니다. 먼저 기업 프로필을 생성해주세요."));

        JobPosting jobPosting = new JobPosting(company, title, location,
                JobPosting.JobType.valueOf(jobType.name()),
                JobPosting.ExperienceLevel.valueOf(experienceLevel.name()), true);
        jobPosting.updateContent(description, null, null, null);
        jobPosting.updateSalaryInfo(
                minSalary != null ? java.math.BigDecimal.valueOf(minSalary) : null,
                maxSalary != null ? java.math.BigDecimal.valueOf(maxSalary) : null,
                "KRW");

        return jobPostingRepository.save(jobPosting);
    }

    /**
     * 채용공고를 발행합니다.
     *
     * 초안 상태의 채용공고를 정식으로 발행하여 공개합니다.
     * 마감일을 설정하고 지원자가 지원할 수 있는 상태로 만듭니다.
     *
     * @param jobPostingId 발행할 채용공고 ID
     * @param deadlineDate 지원 마감일
     * @return JobPosting 발행된 채용공고
     * @throws IllegalArgumentException 채용공고를 찾을 수 없는 경우
     */
    @Transactional
    public JobPosting publishJobPosting(Long jobPostingId, LocalDate deadlineDate) {
        JobPosting jobPosting = getJobPosting(jobPostingId);
        jobPosting.setDeadline_date(deadlineDate);
        jobPosting.publish();
        return jobPostingRepository.save(jobPosting);
    }

    /**
     * 채용공고를 조회합니다.
     *
     * ID로 채용공고를 조회하여 상세 정보를 반환합니다.
     * 내부에서 사용하는 기본 조회 메서드입니다.
     *
     * @param jobPostingId 조회할 채용공고 ID
     * @return JobPosting 채용공고 정보
     * @throws IllegalArgumentException 채용공고를 찾을 수 없는 경우
     */
    public JobPosting getJobPosting(Long jobPostingId) {
        return jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new IllegalArgumentException("채용공고를 찾을 수 없습니다."));
    }

    /**
     * 발행된 채용공고 목록을 조회합니다.
     *
     * ACTIVE 상태의 모든 채용공고를 페이징 처리하여 조회합니다.
     * 메인 채용공고 목록 페이지에서 사용됩니다.
     *
     * @param pageable 페이징 정보
     * @return Page<JobPosting> 발행된 채용공고 목록
     */
    public Page<JobPosting> getPublishedJobPostings(Pageable pageable) {
        return jobPostingRepository.findByStatus(JobPosting.JobStatus.ACTIVE, pageable);
    }

    /**
     * 지역별 채용공고를 검색합니다.
     *
     * @param location 검색할 지역명
     * @return List<JobPosting> 해당 지역의 채용공고 목록
     */
    public List<JobPosting> searchJobPostingsByLocation(String location) {
        return jobPostingRepository.findByLocation(location);
    }

    /**
     * 근무형태별 채용공고를 검색합니다.
     *
     * @param jobType 검색할 직무유형
     * @return List<JobPosting> 해당 직무유형의 채용공고 목록
     */
    public List<JobPosting> searchJobPostingsByJobType(JobPosting.JobType jobType) {
        return jobPostingRepository.findByJob_type(jobType);
    }

    /**
     * 경력수준별 채용공고를 검색합니다.
     *
     * @param experienceLevel 검색할 경력수준
     * @return List<JobPosting> 해당 경력수준의 채용공고 목록
     */
    public List<JobPosting> searchJobPostingsByExperienceLevel(JobPosting.ExperienceLevel experienceLevel) {
        return jobPostingRepository.findByExperience_level(experienceLevel);
    }

    /**
     * 키워드로 채용공고를 검색합니다.
     *
     * 제목이나 설명에 키워드가 포함된 채용공고를 검색합니다.
     *
     * @param keyword 검색 키워드
     * @return List<JobPosting> 키워드가 포함된 채용공고 목록
     */
    public List<JobPosting> searchJobPostingsByKeyword(String keyword) {
        return jobPostingRepository.findByTitleContainingOrDescriptionContaining(keyword);
    }

    /**
     * 채용공고의 조회수를 증가시킵니다.
     *
     * 채용공고 상세 조회 시 자동으로 조회수를 1 증가시킵니다.
     *
     * @param jobPostingId 조회수를 증가시킬 채용공고 ID
     * @throws IllegalArgumentException 채용공고를 찾을 수 없는 경우
     */
    @Transactional
    public void incrementViewCount(Long jobPostingId) {
        JobPosting jobPosting = getJobPosting(jobPostingId);
        jobPosting.incrementViewCount();
        jobPostingRepository.save(jobPosting);
    }

    /**
     * 기업 사용자의 채용공고 목록을 조회합니다.
     *
     * 특정 기업 사용자가 작성한 모든 채용공고를 조회합니다.
     *
     * @param userId 채용공고를 조회할 기업 사용자 ID
     * @return List<JobPosting> 해당 기업의 채용공고 목록
     * @throws IllegalArgumentException 기업 프로필을 찾을 수 없는 경우
     */
    public List<JobPosting> getJobPostingsByCompanyUser(Long userId) {
        // 사용자와 연결된 Company 조회
        Company company = companyRepository.findByUser_userId(userId)
                .orElseThrow(() -> new IllegalArgumentException("기업 프로필이 없습니다."));

        return jobPostingRepository.findByCompany(company);
    }

    @Transactional
    public JobPosting updateJobPosting(Long jobPostingId, String title, String description,
                                       Integer minSalary, Integer maxSalary) {
        JobPosting jobPosting = getJobPosting(jobPostingId);
        jobPosting.updateBasicInfo(title, description, jobPosting.getRequirements(),
                jobPosting.getPreferred_qualifications(), jobPosting.getLocation(),
                jobPosting.getJob_type(), jobPosting.getExperience_level());
        jobPosting.updateContent(description, jobPosting.getQualifications(), jobPosting.getRequiredSkills(), jobPosting.getBenefits());
        jobPosting.updateSalaryInfo(
                minSalary != null ? java.math.BigDecimal.valueOf(minSalary) : jobPosting.getSalary_min(),
                maxSalary != null ? java.math.BigDecimal.valueOf(maxSalary) : jobPosting.getSalary_max(),
                "KRW");
        return jobPostingRepository.save(jobPosting);
    }

    @Transactional
    public JobPosting updateJobPosting(Long jobPostingId, org.jbd.backend.job.dto.JobPostingUpdateDto dto) {
        JobPosting jobPosting = getJobPosting(jobPostingId);

        // 기본 정보 업데이트 (null이 아닌 필드만)
        if (dto.getTitle() != null || dto.getCompanyName() != null || dto.getLocation() != null ||
                dto.getJobType() != null || dto.getDepartment() != null || dto.getField() != null ||
                dto.getExperienceLevel() != null) {
            jobPosting.updateBasicInfo(
                    dto.getTitle() != null ? dto.getTitle() : jobPosting.getTitle(),
                    dto.getDescription() != null ? dto.getDescription() : jobPosting.getDescription(),
                    jobPosting.getRequirements(), // requirements는 별도 DTO 필드가 없으면 기존값 유지
                    jobPosting.getPreferred_qualifications(), // 기존값 유지
                    dto.getLocation() != null ? dto.getLocation() : jobPosting.getLocation(),
                    dto.getJobType() != null ? JobPosting.JobType.valueOf(dto.getJobType().name()) : jobPosting.getJob_type(),
                    dto.getExperienceLevel() != null ? JobPosting.ExperienceLevel.valueOf(dto.getExperienceLevel().name()) : jobPosting.getExperience_level()
            );
        }

        // 내용 정보 업데이트
        if (dto.getDescription() != null || dto.getQualifications() != null ||
                dto.getRequiredSkills() != null || dto.getBenefits() != null) {
            jobPosting.updateContent(
                    dto.getDescription() != null ? dto.getDescription() : jobPosting.getDescription(),
                    dto.getQualifications() != null ? dto.getQualifications() : jobPosting.getQualifications(),
                    dto.getRequiredSkills() != null ? dto.getRequiredSkills() : jobPosting.getRequiredSkills(),
                    dto.getBenefits() != null ? dto.getBenefits() : jobPosting.getBenefits()
            );
        }

        // 급여 정보 업데이트
        if (dto.getMinSalary() != null || dto.getMaxSalary() != null || dto.getSalaryNegotiable() != null) {
            jobPosting.updateSalaryInfo(
                    dto.getMinSalary() != null ? java.math.BigDecimal.valueOf(dto.getMinSalary()) : jobPosting.getSalary_min(),
                    dto.getMaxSalary() != null ? java.math.BigDecimal.valueOf(dto.getMaxSalary()) : jobPosting.getSalary_max(),
                    "KRW"
            );
        }

        // 근무 조건 업데이트
        if (dto.getWorkingHours() != null || dto.getIsRemotePossible() != null) {
            jobPosting.updateWorkingConditions(
                    dto.getWorkingHours() != null ? dto.getWorkingHours() : jobPosting.getWorkingHours(),
                    dto.getIsRemotePossible() != null ? dto.getIsRemotePossible() : jobPosting.getIsRemotePossible()
            );
        }

        // 연락처 정보 업데이트
        if (dto.getContactEmail() != null || dto.getContactPhone() != null) {
            jobPosting.updateContactInfo(
                    dto.getContactEmail() != null ? dto.getContactEmail() : jobPosting.getContactEmail(),
                    dto.getContactPhone() != null ? dto.getContactPhone() : jobPosting.getContactPhone()
            );
        }

        return jobPostingRepository.save(jobPosting);
    }

    @Transactional
    public JobPosting closeJobPosting(Long jobPostingId) {
        JobPosting jobPosting = getJobPosting(jobPostingId);
        jobPosting.close();
        return jobPostingRepository.save(jobPosting);
    }

    public List<JobPosting> getExpiredJobPostings() {
        return jobPostingRepository.findByStatusAndDeadlineDateBefore(JobPosting.JobStatus.ACTIVE, LocalDate.now());
    }

    // 기본 필터 검색 (위치, 근무 형태, 경력 수준 기준으로 검색)
    public Page<JobPosting> searchJobPostings(String location, JobType jobType, ExperienceLevel experienceLevel, Pageable pageable) {
        return jobPostingRepository.findByFilters(
                JobPosting.JobStatus.ACTIVE,
                location,
                jobType != null ? JobPosting.JobType.valueOf(jobType.name()) : null,
                experienceLevel != null ? JobPosting.ExperienceLevel.valueOf(experienceLevel.name()) : null,
                null,  // title
                null,  // minSalary
                null,  // maxSalary
                pageable
        );
    }

    // 향상된 검색 메서드 추가 (제목, 연봉 범위 포함)
    public Page<JobPosting> searchJobPostingsAdvanced(String title, String location, JobType jobType,
                                                      ExperienceLevel experienceLevel, Integer minSalary,
                                                      Integer maxSalary, Pageable pageable) {
        return jobPostingRepository.findByFilters(
                JobPosting.JobStatus.ACTIVE,
                location,
                jobType != null ? JobPosting.JobType.valueOf(jobType.name()) : null,
                experienceLevel != null ? JobPosting.ExperienceLevel.valueOf(experienceLevel.name()) : null,
                title,
                minSalary,
                maxSalary,
                pageable
        );
    }


    public long getJobPostingCountByCompany(Company company) {
        return jobPostingRepository.countByCompany(company);
    }

    @Transactional
    public void deleteJobPosting(Long jobPostingId) {
        JobPosting jobPosting = getJobPosting(jobPostingId);

        // 먼저 연관된 지원서들을 삭제
        List<JobApplication> applications = jobApplicationRepository.findByJobPosting(jobPosting);
        if (!applications.isEmpty()) {
            jobApplicationRepository.deleteAll(applications);
        }

        // 그 다음 채용공고 삭제
        jobPostingRepository.delete(jobPosting);
    }

    // ====== JobAtda 통합 기능들 ======

    /**
     * 고급 검색 기능을 제공합니다.
     *
     * JPA Specification을 활용하여 복합 조건의 채용공고 검색을 수행합니다.
     * 다양한 검색 필터를 조합하여 정밀한 검색 결과를 제공합니다.
     *
     * @param searchDto 검색 조건을 담은 DTO
     * @param pageable 페이징 정보
     * @return Page<JobPosting> 검색된 채용공고 목록
     */
    public Page<JobPosting> searchJobPostingsWithSpecification(JobPostingSearchDto searchDto, Pageable pageable) {
        log.info("고급 검색 기준: {}", searchDto);

        Specification<JobPosting> spec = JobPostingSpecification.withSearchCriteria(searchDto);
        return jobPostingRepository.findAll(spec, pageable);
    }

    /**
     * 간편 검색 기능을 제공합니다.
     *
     * 키워드와 기본 필터(지역, 직무유형, 경력수준)를 이용한 간단한 검색을 수행합니다.
     * 일반 사용자가 쉽게 사용할 수 있는 검색 기능입니다.
     *
     * @param keyword 검색 키워드 (제목에서 검색)
     * @param location 근무지 필터
     * @param jobType 직무유형 필터
     * @param experienceLevel 경력수준 필터
     * @param pageable 페이징 정보
     * @return Page<JobPosting> 검색된 채용공고 목록
     */
    @Transactional(readOnly = true)
    public Page<JobPosting> searchJobPostingsSimple(String keyword, String location, JobType jobType,
                                                    ExperienceLevel experienceLevel, Pageable pageable) {
        log.info("단순 검색 - 키원드: {}, location: {}, jobType: {}, experienceLevel: {}",
                keyword, location, jobType, experienceLevel);

        // @EntityGraph가 적용된 Repository 메서드를 직접 사용
        Page<JobPosting> result = jobPostingRepository.findByFilters(
                JobPosting.JobStatus.ACTIVE,
                location,
                jobType != null ? JobPosting.JobType.valueOf(jobType.name()) : null,
                experienceLevel != null ? JobPosting.ExperienceLevel.valueOf(experienceLevel.name()) : null,
                keyword,  // title로 키워드 전달
                null,     // minSalary
                null,     // maxSalary
                pageable
        );

        // 디버깅: Company가 로드되었는지 확인
        result.getContent().forEach(jp -> {
            log.debug("JobPosting ID: {}, Company 초기화 완료: {}",
                    jp.getId(),
                    jp.getCompany() != null ? Hibernate.isInitialized(jp.getCompany()) : "null");
        });

        return result;
    }

    /**
     * 간편 검색 기능 - DTO 포함 버전
     */
    @Transactional(readOnly = true)
    public Page<JobPostingResponseDto> searchJobPostingsSimpleWithDto(String keyword, String location, JobType jobType,
                                                                      ExperienceLevel experienceLevel, Pageable pageable) {
        log.info("DTO를 통한 단순 검색 - keyword: {}, location: {}, jobType: {}, experienceLevel: {}",
                keyword, location, jobType, experienceLevel);

        // @EntityGraph가 적용된 Repository 메서드를 직접 사용
        Page<JobPosting> result = jobPostingRepository.findByFilters(
                JobPosting.JobStatus.ACTIVE,
                location,
                jobType != null ? JobPosting.JobType.valueOf(jobType.name()) : null,
                experienceLevel != null ? JobPosting.ExperienceLevel.valueOf(experienceLevel.name()) : null,
                keyword,  // title로 키워드 전달
                null,     // minSalary
                null,     // maxSalary
                pageable
        );

        // 트랜잭션 내에서 DTO 변환
        return result.map(JobPostingResponseDto::from);
    }

    /**
     * 마감 임박 채용공고를 조회합니다.
     *
     * 지정된 일수 내에 마감되는 채용공고를 조회합니다.
     * 마감 임박 알림이나 긴급 지원 안내에 활용됩니다.
     *
     * @param days 마감까지 남은 일수 (1-30일)
     * @return List<JobPosting> 마감 임박 채용공고 목록
     * @throws IllegalArgumentException 일수가 유효 범위를 벗어난 경우
     */
    public List<JobPosting> getDeadlineApproachingJobPostings(int days) {
        log.info("{}일 내 마감 예정인 채용공고 조회 중", days);

        if (days <= 0 || days > 30) {
            throw new IllegalArgumentException("일수는 1일에서 30일 사이여야 합니다.");
        }

        LocalDate now = LocalDate.now();
        LocalDate deadline = now.plusDays(days);

        return jobPostingRepository.findDeadlineApproachingJobPostings(now, deadline);
    }

    /**
     * 기업별 마감 임박 채용공고 조회
     */
    public List<JobPosting> getDeadlineApproachingJobPostingsByCompany(User companyUser, int days) {
        log.info("회사: {}의 {}일 이내 마감 예정 채용공고 조회 중",
                companyUser.getId(), days);

        if (days <= 0 || days > 30) {
            throw new IllegalArgumentException("일수는 1일에서 30일 사이여야 합니다.");
        }

        LocalDate now = LocalDate.now();
        LocalDate deadline = now.plusDays(days);

        Company company = companyRepository.findByUser(companyUser)
                .orElseThrow(() -> new IllegalArgumentException("기업 정보를 찾을 수 없습니다."));
        return jobPostingRepository.findDeadlineApproachingJobPostingsByCompany(company, now, deadline);
    }

    /**
     * 채용공고의 상세 통계를 조회합니다.
     *
     * 특정 채용공고의 조회수, 지원자 수, 전환율 등의 상세 통계 정보를 제공합니다.
     * 기업의 채용 성과 분석에 활용됩니다.
     *
     * @param jobPostingId 통계를 조회할 채용공고 ID
     * @return JobPostingStatsDto 채용공고 통계 정보
     * @throws IllegalArgumentException 채용공고를 찾을 수 없는 경우
     */
    public JobPostingStatsDto getJobPostingStats(Long jobPostingId) {
        log.info("채용공고 ID: {} 통계 정보 조회 중", jobPostingId);

        JobPosting jobPosting = getJobPosting(jobPostingId);

        return convertToStatsDto(jobPosting);
    }

    /**
     * 기업별 채용공고 통계 목록 조회
     */
    public List<JobPostingStatsDto> getJobPostingStatsByCompany(User companyUser) {
        log.info("회사: {} 채용공고 통계 정보 조회 중", companyUser.getId());

        List<JobPosting> jobPostings = jobPostingRepository.findByCompanyUser(companyUser);

        return jobPostings.stream()
                .map(this::convertToStatsDto)
                .collect(Collectors.toList());
    }

    /**
     * 전체 채용공고 통계 (관리자용)
     */
    public Object getOverallJobPostingStatistics() {
        log.info("전체 채용공고 통계 정보 조회 중");

        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        Object[] stats = jobPostingRepository.findJobPostingStatistics(weekAgo);

        return Map.of(
                "totalJobPostings", stats[0],
                "activeJobPostings", stats[1],
                "closedJobPostings", stats[2],
                "thisWeekJobPostings", stats[3]
        );
    }

    /**
     * JobPosting 엔티티를 JobPostingStatsDto로 변환합니다.
     *
     * 채용공고 엔티티의 데이터를 통계 DTO로 변환하여 분석 데이터를 제공합니다.
     * 마감일까지의 남은 일수, 조회수 대비 지원자 비율 등을 계산합니다.
     *
     * @param jobPosting 변환할 채용공고 엔티티
     * @return JobPostingStatsDto 통계 정보가 담긴 DTO
     */
    private JobPostingStatsDto convertToStatsDto(JobPosting jobPosting) {
        Long daysUntilDeadline = null;
        Boolean isDeadlineApproaching = false;
        Boolean isExpired = false;

        if (jobPosting.getDeadlineDate() != null) {
            LocalDate now = LocalDate.now();
            daysUntilDeadline = ChronoUnit.DAYS.between(now, jobPosting.getDeadlineDate());
            isDeadlineApproaching = daysUntilDeadline <= 7 && daysUntilDeadline >= 0;
            isExpired = daysUntilDeadline < 0;
        }

        Double viewToApplicationRatio = null;
        if (jobPosting.getView_count() != null && jobPosting.getView_count() > 0) {
            viewToApplicationRatio = (double) jobPosting.getApplication_count() / jobPosting.getView_count() * 100;
        }

        return JobPostingStatsDto.builder()
                .jobId(jobPosting.getId())
                .title(jobPosting.getTitle())
                .companyName(jobPosting.getCompanyName())
                .viewCount(jobPosting.getView_count() != null ? Long.valueOf(jobPosting.getView_count()) : 0L)
                .applicationCount(jobPosting.getApplication_count() != null ? Long.valueOf(jobPosting.getApplication_count()) : 0L)
                .status(org.jbd.backend.job.domain.enums.JobStatus.valueOf(jobPosting.getStatus().name()))
                .publishedAt(jobPosting.getPublishedAt())
                .deadlineDate(jobPosting.getDeadlineDate())
                .createdAt(jobPosting.getCreatedAt())
                .updatedAt(jobPosting.getUpdatedAt())
                .daysUntilDeadline(daysUntilDeadline)
                .viewToApplicationRatio(viewToApplicationRatio)
                .isDeadlineApproaching(isDeadlineApproaching)
                .isExpired(isExpired)
                .isActive(jobPosting.getStatus() == JobPosting.JobStatus.ACTIVE)
                .build();
    }

    // ====== 클린 아키텍처를 위한 새로운 메서드들 ======

    /**
     * 꺌비한 API를 위한 채용공고 생성 메서드입니다.
     *
     * DTO를 활용하여 채용공고를 생성하고 권한 검증을 수행합니다.
     * 비즈니스 로직 및 예외 처리가 포함된 고수준 API입니다.
     *
     * @param userId 채용공고를 생성할 사용자 ID
     * @param dto 채용공고 생성 요청 DTO
     * @return JobPostingResponseDto 생성된 채용공고 응답 DTO
     * @throws BusinessException 사용자나 기업을 찾을 수 없는 경우
     */
    @Transactional
    public JobPostingResponseDto createJobPostingWithDto(Long userId, JobPostingCreateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 사용자와 연결된 Company 조회
        Company company = companyRepository.findByUser_userId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND, "기업 프로필이 없습니다."));

        JobPosting jobPosting = new JobPosting(company, dto.getTitle(),
                dto.getLocation(),
                JobPosting.JobType.valueOf(dto.getJobType().name()),
                JobPosting.ExperienceLevel.valueOf(dto.getExperienceLevel().name()), true);
        jobPosting.updateContent(dto.getDescription(), null, null, null);

        if (dto.getMinSalary() != null || dto.getMaxSalary() != null) {
            jobPosting.updateSalaryInfo(
                    dto.getMinSalary() != null ? java.math.BigDecimal.valueOf(dto.getMinSalary()) : java.math.BigDecimal.ZERO,
                    dto.getMaxSalary() != null ? java.math.BigDecimal.valueOf(dto.getMaxSalary()) : java.math.BigDecimal.ZERO,
                    "KRW"
            );
        }

        JobPosting saved = jobPostingRepository.save(jobPosting);
        return JobPostingResponseDto.from(saved);
    }

    /**
     * 조회수 증가와 함께 채용공고를 조회합니다.
     *
     * 채용공고 상세 조회 시 자동으로 조회수를 1 증가시키고 DTO로 변환하여 반환합니다.
     * 고수준 API에서 사용되는 메서드입니다.
     *
     * @param id 조회할 채용공고 ID
     * @return JobPostingResponseDto 채용공고 상세 정보 DTO
     * @throws IllegalArgumentException 채용공고를 찾을 수 없는 경우
     */
    @Transactional
    public JobPostingResponseDto getJobPostingWithDto(Long id) {
        JobPosting jobPosting = getJobPosting(id);
        incrementViewCount(id);
        return JobPostingResponseDto.from(jobPosting);
    }

    /**
     * 권한 검증과 함께 채용공고를 수정합니다.
     *
     * 사용자의 채용공고 수정 권한을 검증한 후 업데이트를 수행합니다.
     * 작성자 또는 관리자만 수정이 가능합니다.
     *
     * @param userId 수정을 요청하는 사용자 ID
     * @param jobPostingId 수정할 채용공고 ID
     * @param dto 채용공고 수정 요청 DTO
     * @return JobPostingResponseDto 수정된 채용공고 응답 DTO
     * @throws BusinessException 권한이 없거나 채용공고를 찾을 수 없는 경우
     */
    @Transactional
    public JobPostingResponseDto updateJobPostingWithPermissionCheck(Long userId, Long jobPostingId, JobPostingUpdateDto dto) {
        JobPosting jobPosting = getJobPosting(jobPostingId);

        // 권한 확인
        if (!permissionService.canEditJobPosting(userId, jobPosting)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "수정 권한이 없습니다. 작성자 또는 관리자만 수정할 수 있습니다.");
        }

        JobPosting updated = updateJobPosting(jobPostingId, dto);
        return JobPostingResponseDto.from(updated);
    }

    /**
     * 권한 검증과 함께 채용공고 발행
     */
    @Transactional
    public JobPostingResponseDto publishJobPostingWithPermissionCheck(Long userId, Long jobPostingId, LocalDate deadlineDate) {
        JobPosting jobPosting = getJobPosting(jobPostingId);

        if (!permissionService.canManageJobPosting(userId, jobPosting)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "발행 권한이 없습니다. 작성자 또는 관리자만 발행할 수 있습니다.");
        }

        JobPosting published = publishJobPosting(jobPostingId, deadlineDate);
        return JobPostingResponseDto.from(published);
    }

    /**
     * 권한 검증과 함께 채용공고 마감
     */
    @Transactional
    public JobPostingResponseDto closeJobPostingWithPermissionCheck(Long userId, Long jobPostingId) {
        JobPosting jobPosting = getJobPosting(jobPostingId);

        if (!permissionService.canManageJobPosting(userId, jobPosting)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "마감 권한이 없습니다. 작성자 또는 관리자만 마감할 수 있습니다.");
        }

        JobPosting closed = closeJobPosting(jobPostingId);
        return JobPostingResponseDto.from(closed);
    }

    /**
     * 권한 검증과 함께 채용공고를 삭제합니다.
     *
     * 사용자의 채용공고 삭제 권한을 검증한 후 삭제를 수행합니다.
     * 작성자 또는 관리자만 삭제가 가능하며, 연관된 지원서도 함께 삭제됩니다.
     *
     * @param userId 삭제를 요청하는 사용자 ID
     * @param jobPostingId 삭제할 채용공고 ID
     * @throws BusinessException 권한이 없거나 채용공고를 찾을 수 없는 경우
     */
    @Transactional
    public void deleteJobPostingWithPermissionCheck(Long userId, Long jobPostingId) {
        JobPosting jobPosting = getJobPosting(jobPostingId);

        if (!permissionService.canDeleteJobPosting(userId, jobPosting)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "삭제 권한이 없습니다. 작성자 또는 관리자만 삭제할 수 있습니다.");
        }

        deleteJobPosting(jobPostingId);
    }

    /**
     * 사용자 조회와 내 채용공고 목록 반환
     */
    public List<JobPostingResponseDto> getMyJobPostingsWithDto(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<JobPosting> jobPostings = getJobPostingsByCompanyUser(userId);
        return jobPostings.stream()
                .map(JobPostingResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 관리자 권한 검증과 함께 전체 통계 조회
     */
    public Object getOverallJobPostingStatisticsWithPermissionCheck(String userType, Boolean isAdmin) {
        if (!"ADMIN".equals(userType) || !Boolean.TRUE.equals(isAdmin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자만 접근할 수 있습니다.");
        }

        return getOverallJobPostingStatistics();
    }

    /**
     * 내 마감 임박 채용공고 조회 (검증 포함)
     */
    public List<JobPostingResponseDto> getMyDeadlineApproachingJobPostingsWithDto(Long userId, int days) {
        if (days <= 0 || days > 30) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "일수는 1일에서 30일 사이여야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<JobPosting> jobPostings = getDeadlineApproachingJobPostingsByCompany(user, days);
        return jobPostings.stream()
                .map(JobPostingResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 내 채용공고 통계 목록 조회
     */
    public List<JobPostingStatsDto> getMyJobPostingStatsWithDto(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return getJobPostingStatsByCompany(user);
    }

    /**
     * 마감 임박 채용공고 조회 (검증 포함)
     */
    public List<JobPostingResponseDto> getDeadlineApproachingJobPostingsWithDto(int days) {
        if (days <= 0 || days > 30) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "일수는 1일에서 30일 사이여야 합니다.");
        }

        List<JobPosting> jobPostings = getDeadlineApproachingJobPostings(days);
        return jobPostings.stream()
                .map(JobPostingResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 키워드 검색 결과 DTO 변환
     */
    public List<JobPostingResponseDto> searchJobPostingsByKeywordWithDto(String keyword) {
        List<JobPosting> jobPostings = searchJobPostingsByKeyword(keyword);
        return jobPostings.stream()
                .map(JobPostingResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 기본 검색 결과 DTO 변환
     */
    public PageResponse<JobPostingResponseDto> searchJobPostingsWithDto(
            String location, JobType jobType, ExperienceLevel experienceLevel, Pageable pageable) {
        Page<JobPosting> jobPostings = searchJobPostings(location, jobType, experienceLevel, pageable);
        Page<JobPostingResponseDto> responseDtos = jobPostings.map(JobPostingResponseDto::from);
        return new PageResponse<>(responseDtos);
    }

    /**
     * 고급 검색 결과 DTO 변환
     */
    public PageResponse<JobPostingResponseDto> searchJobPostingsAdvancedWithDto(
            String title, String location, JobType jobType, ExperienceLevel experienceLevel,
            Integer minSalary, Integer maxSalary, Pageable pageable) {
        Page<JobPosting> jobPostings = searchJobPostingsAdvanced(
                title, location, jobType, experienceLevel, minSalary, maxSalary, pageable);
        Page<JobPostingResponseDto> responseDtos = jobPostings.map(JobPostingResponseDto::from);
        return new PageResponse<>(responseDtos);
    }

    /**
     * POST 방식 고급 검색 결과 DTO 변환
     */
    public PageResponse<JobPostingResponseDto> searchJobPostingsWithSpecificationAndDto(
            JobPostingSearchDto searchDto, Pageable pageable) {
        Page<JobPosting> jobPostings = searchJobPostingsWithSpecification(searchDto, pageable);
        Page<JobPostingResponseDto> responseDtos = jobPostings.map(JobPostingResponseDto::from);
        return new PageResponse<>(responseDtos);
    }

}