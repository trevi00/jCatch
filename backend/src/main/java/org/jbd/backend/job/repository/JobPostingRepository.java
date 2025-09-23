package org.jbd.backend.job.repository;

import org.jbd.backend.job.domain.JobPosting;
import org.jbd.backend.company.domain.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 채용공고 리포지토리
 *
 * 채용공고 엔티티에 대한 다양한 데이터 액세스 기능을 제공하는 리포지토리입니다.
 * JpaRepository와 JpaSpecificationExecutor를 확장하여 기본적인 CRUD 기능과
 * 동적 쿼리 생성 기능을 제공합니다.
 *
 * 주요 기능:
 * - 다양한 조건에 따른 채용공고 검색 및 필터링
 * - 지역, 직무유형, 경력수준별 채용공고 조회
 * - 기업별 채용공고 관리 및 통계
 * - 채용공고 상태별 대시보드 데이터 제공
 * - 마감일이 임박한 채용공고 알림 기능
 * - 연관 엔티티 즉시 로딩으로 N+1 문제 해결
 * - 복합 조건 검색 및 동적 쿼리 생성
 *
 * 성능 최적화:
 * - EntityGraph를 활용한 연관 엔티티 종속 로딩
 * - 다양한 집계 쿼리로 대시보드 성능 향상
 * - 페이징 처리로 대용량 데이터 효율적 처리
 * - JPA Specification을 통한 동적 쿼리 최적화
 *
 * 지원 기능:
 * - 채용공고 상태 관리 (발행, 마감, 중단 등)
 * - 지웭별/직무별/경력별 세분화된 검색
 * - 급여 범위 및 키워드 기반 다중 필터링
 * - 기업 대시보드용 지원자 수 통계
 * - 조회수 및 인기도 기반 정렬
 */
public interface JobPostingRepository extends JpaRepository<JobPosting, Long>, JpaSpecificationExecutor<JobPosting> {

    /**
     * 채용공고 ID로 채용공고와 기업 정보를 함께 조회합니다.
     *
     * EntityGraph를 사용하여 기업 정보를 즉시 로딩하여 N+1 문제를 방지합니다.
     * 채용공고 상세 보기 페이지에서 기업 정보와 함께 표시할 때 사용됩니다.
     *
     * @param id 조회할 채용공고 ID
     * @return Optional<JobPosting> 채용공고와 기업 정보가 포함된 결과
     */
    @Override
    @EntityGraph(attributePaths = {"company"})
    Optional<JobPosting> findById(Long id);

    /**
     * 특정 지역의 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 지역별 채용공고 검색 기능에서 사용되며, 구직자의 선호 지역에 따른
     * 채용공고 필터링에 활용됩니다.
     *
     * @param location 조회할 지역명 (예: "서울", "부산", "대구" 등)
     * @return List<JobPosting> 해당 지역의 채용공고 목록 (기업 정보 포함)
     */
    @EntityGraph(attributePaths = {"company"})
    List<JobPosting> findByLocation(String location);

    /**
     * 특정 직무 유형의 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 정규직, 임시직, 인턴, 프리랜서 등 직무 유형별로 채용공고를 필터링하여
     * 구직자의 근무 형태 선호도에 맞는 채용공고를 제공합니다.
     *
     * @param job_type 조회할 직무 유형 (FULL_TIME, PART_TIME, INTERNSHIP, FREELANCE 등)
     * @return List<JobPosting> 해당 직무 유형의 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.job_type = :jobType")
    List<JobPosting> findByJob_type(@Param("jobType") JobPosting.JobType job_type);

    /**
     * 특정 경력 수준의 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 신입, 주니어, 시니어 등 경력 단계별로 채용공고를 필터링하여
     * 구직자의 경력 수준에 적합한 채용공고를 제공합니다.
     *
     * @param experience_level 조회할 경력 수준 (ENTRY, JUNIOR, MID_LEVEL, SENIOR, EXECUTIVE 등)
     * @return List<JobPosting> 해당 경력 수준의 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.experience_level = :experienceLevel")
    List<JobPosting> findByExperience_level(@Param("experienceLevel") JobPosting.ExperienceLevel experience_level);

    /**
     * 특정 지역과 직무 유형에 맞는 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 지역과 직무 유형을 동시에 고려한 복합 검색 기능에서 사용되며,
     * 더 세밀한 조건의 채용공고 필터링을 제공합니다.
     *
     * @param location 조회할 지역명
     * @param job_type 조회할 직무 유형
     * @return List<JobPosting> 지역과 직무 유형이 모두 일치하는 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.location = :location AND j.job_type = :jobType")
    List<JobPosting> findByLocationAndJob_type(@Param("location") String location, @Param("jobType") JobPosting.JobType job_type);

    /**
     * 특정 지역과 경력 수준에 맞는 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 지역과 경력 수준을 동시에 고려한 채용공고 검색에서 사용되며,
     * 구직자의 근무 선호 지역과 경력 단계에 맞는 기회를 제공합니다.
     *
     * @param location 조회할 지역명
     * @param experience_level 조회할 경력 수준
     * @return List<JobPosting> 지역과 경력 수준이 모두 일치하는 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.location = :location AND j.experience_level = :experienceLevel")
    List<JobPosting> findByLocationAndExperience_level(@Param("location") String location, @Param("experienceLevel") JobPosting.ExperienceLevel experience_level);

    /**
     * 특정 직무 유형과 경력 수준에 맞는 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 직무 유형과 경력 수준을 동시에 고려한 채용공고 검색에서 사용되며,
     * 근무 형태와 경력 요구사항을 모두 만족하는 기회를 제공합니다.
     *
     * @param job_type 조회할 직무 유형
     * @param experience_level 조회할 경력 수준
     * @return List<JobPosting> 직무 유형과 경력 수준이 모두 일치하는 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.job_type = :jobType AND j.experience_level = :experienceLevel")
    List<JobPosting> findByJob_typeAndExperience_level(@Param("jobType") JobPosting.JobType job_type, @Param("experienceLevel") JobPosting.ExperienceLevel experience_level);

    /**
     * 지역, 직무 유형, 경력 수준을 모두 만족하는 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 가장 세밀한 조건의 채용공고 필터링 기능에서 사용되며,
     * 구직자의 모든 선호 조건을 만족하는 맞춤형 채용공고를 제공합니다.
     *
     * @param location 조회할 지역명
     * @param job_type 조회할 직무 유형
     * @param experience_level 조회할 경력 수준
     * @return List<JobPosting> 모든 조건을 만족하는 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.location = :location AND j.job_type = :jobType AND j.experience_level = :experienceLevel")
    List<JobPosting> findByLocationAndJob_typeAndExperience_level(@Param("location") String location, @Param("jobType") JobPosting.JobType job_type, @Param("experienceLevel") JobPosting.ExperienceLevel experience_level);

    /**
     * 제목이나 설명에 특정 키워드가 포함된 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 채용공고 통합 검색 기능에서 사용되며, 채용공고 제목과 상세 설명 내용에서
     * 키워드를 포함한 게시글을 검색합니다. 대소문자를 구분하지 않는 검색입니다.
     *
     * @param keyword 검색할 키워드 (제목이나 설명에서 부분 일치 검색)
     * @return List<JobPosting> 키워드가 포함된 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.title LIKE %:keyword% OR j.description LIKE %:keyword%")
    List<JobPosting> findByTitleContainingOrDescriptionContaining(@Param("keyword") String keyword);

    /**
     * 특정 기업의 모든 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 기업 대시보드에서 해당 기업이 등록한 모든 채용공고를 조회할 때 사용됩니다.
     * 기업의 채용 현황과 채용공고 관리에 활용됩니다.
     *
     * @param company 조회할 기업 엔티티
     * @return List<JobPosting> 해당 기업의 모든 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    List<JobPosting> findByCompany(Company company);

    /**
     * 특정 상태의 모든 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 채용공고 상태별 조회 기능에서 사용되며, 발행, 마감, 중단 등의 상태별로
     * 채용공고를 분류하여 관리할 때 활용됩니다.
     *
     * @param status 조회할 채용공고 상태 (PUBLISHED, CLOSED, DRAFT 등)
     * @return List<JobPosting> 해당 상태의 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    List<JobPosting> findByStatus(JobPosting.JobStatus status);

    /**
     * 특정 상태의 채용공고를 기업 정보와 함께 페이징 조회합니다.
     *
     * 대용량 채용공고 데이터의 효율적 처리를 위해 페이징을 지원합니다.
     * 사용자에게 점진적으로 채용공고를 로드하여 성능을 향상시킵니다.
     *
     * @param status 조회할 채용공고 상태
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬 조건)
     * @return Page<JobPosting> 해당 상태의 채용공고 페이징 결과
     */
    @EntityGraph(attributePaths = {"company"})
    Page<JobPosting> findByStatus(JobPosting.JobStatus status, Pageable pageable);

    /**
     * 특정 상태이면서 마감일이 지난 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 마감일이 지난 채용공고들을 자동으로 마감 상태로 변경하거나
     * 정리할 때 사용되는 배치 작업용 메서드입니다.
     *
     * @param status 조회할 채용공고 상태
     * @param date 기준 날짜 (이 날짜보다 이전에 마감되는 채용공고)
     * @return List<JobPosting> 마감일이 지난 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.status = :status AND j.deadline_date < :date")
    List<JobPosting> findByStatusAndDeadline_dateBefore(@Param("status") JobPosting.JobStatus status, @Param("date") LocalDate date);

    /**
     * 특정 기간 내에 발행된 채용공고를 기업 정보와 함께 조회합니다.
     *
     * 시기별 채용 현황 분석이나 마케팅 효과 발생 기간 분석 등에 사용됩니다.
     * 스케줄링된 채용공고 마케팅이나 광고 기간 대시보드에도 활용됩니다.
     *
     * @param start 발행 기간 시작 시간
     * @param end 발행 기간 종료 시간
     * @return List<JobPosting> 지정된 기간 내 발행된 채용공고 목록
     */
    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.posted_at BETWEEN :start AND :end")
    List<JobPosting> findByPosted_atBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.status = :status AND j.location = :location")
    Page<JobPosting> findByStatusAndLocation(@Param("status") JobPosting.JobStatus status,
                                             @Param("location") String location,
                                             Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.status = :status AND j.job_type = :jobType")
    Page<JobPosting> findByStatusAndJobType(@Param("status") JobPosting.JobStatus status,
                                            @Param("jobType") JobPosting.JobType jobType,
                                            Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.status = :status AND j.experience_level = :experienceLevel")
    Page<JobPosting> findByStatusAndExperienceLevel(@Param("status") JobPosting.JobStatus status,
                                                    @Param("experienceLevel") JobPosting.ExperienceLevel experienceLevel,
                                                    Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.status = :status AND j.location LIKE %:location%")
    Page<JobPosting> findByStatusAndLocationContaining(@Param("status") JobPosting.JobStatus status,
                                                       @Param("location") String location,
                                                       Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.status = :status " +
            "AND (:location IS NULL OR j.location LIKE %:location%) " +
            "AND (:jobType IS NULL OR j.job_type = :jobType) " +
            "AND (:experienceLevel IS NULL OR j.experience_level = :experienceLevel) " +
            "AND (:title IS NULL OR j.title LIKE %:title%) " +
            "AND (:minSalary IS NULL OR j.salary_min >= :minSalary) " +
            "AND (:maxSalary IS NULL OR j.salary_max <= :maxSalary)")
    Page<JobPosting> findByFilters(@Param("status") JobPosting.JobStatus status,
                                   @Param("location") String location,
                                   @Param("jobType") JobPosting.JobType jobType,
                                   @Param("experienceLevel") JobPosting.ExperienceLevel experienceLevel,
                                   @Param("title") String title,
                                   @Param("minSalary") Integer minSalary,
                                   @Param("maxSalary") Integer maxSalary,
                                   Pageable pageable);

    long countByCompany(Company company);

    long countByStatus(JobPosting.JobStatus status);

    long countByCompanyAndStatus(Company company, JobPosting.JobStatus status);

    long countByCreatedAtAfter(LocalDateTime fromDate);

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.company = :company ORDER BY j.createdAt DESC")
    List<JobPosting> findByCompanyOrderByCreatedAtDesc(@Param("company") Company company, Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.company = :company ORDER BY j.view_count DESC")
    List<JobPosting> findByCompanyOrderByViewCountDesc(@Param("company") Company company, Pageable pageable);

    @Query("SELECT j, COALESCE(COUNT(ja), 0) as applicationCount " +
            "FROM JobPosting j " +
            "LEFT JOIN JobApplication ja ON j = ja.jobPosting " +
            "WHERE j.company = :company " +
            "GROUP BY j " +
            "ORDER BY j.view_count DESC")
    List<Object[]> findByCompanyWithApplicationCountOrderByViewCountDesc(
            @Param("company") Company company, Pageable pageable);

    @Query("SELECT j, COALESCE(COUNT(ja), 0) as applicationCount " +
            "FROM JobPosting j " +
            "LEFT JOIN JobApplication ja ON j = ja.jobPosting " +
            "WHERE j.company = :company " +
            "GROUP BY j " +
            "ORDER BY j.createdAt DESC")
    List<Object[]> findByCompanyWithApplicationCountOrderByCreatedAtDesc(
            @Param("company") Company company, Pageable pageable);

    /**
     * 채용공고 전체 통계를 한 번의 쿼리로 조회합니다.
     *
     * 대시보드에서 사용할 채용공고 통계 데이터를 성능 최적화를 위해
     * 한 번의 쿼리로 조회합니다. 전체, 활성, 마감, 이번주 신규 채용공고 수를 포함합니다.
     *
     * @param weekAgo 이번주 시작 날짜 (일주일 전)
     * @return Object[] [전체수, 활성수, 마감수, 이번주신규수] 순서의 배열
     */
    @Query("SELECT " +
            "COUNT(*) as totalJobPostings, " +
            "COUNT(CASE WHEN jp.status = 'PUBLISHED' THEN 1 END) as activeJobPostings, " +
            "COUNT(CASE WHEN jp.status = 'CLOSED' THEN 1 END) as closedJobPostings, " +
            "COUNT(CASE WHEN jp.createdAt >= :weekAgo THEN 1 END) as thisWeekJobPostings " +
            "FROM JobPosting jp")
    Object[] findJobPostingStatistics(@Param("weekAgo") LocalDateTime weekAgo);

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.status = 'PUBLISHED' " +
            "AND j.deadline_date BETWEEN :now AND :deadline " +
            "ORDER BY j.deadline_date ASC")
    List<JobPosting> findDeadlineApproachingJobPostings(@Param("now") LocalDate now,
                                                        @Param("deadline") LocalDate deadline);

    @EntityGraph(attributePaths = {"company"})
    @Query("SELECT j FROM JobPosting j WHERE j.company = :company " +
            "AND j.status = 'PUBLISHED' " +
            "AND j.deadline_date BETWEEN :now AND :deadline " +
            "ORDER BY j.deadline_date ASC")
    List<JobPosting> findDeadlineApproachingJobPostingsByCompany(@Param("company") Company company,
                                                                 @Param("now") LocalDate now,
                                                                 @Param("deadline") LocalDate deadline);

    /**
     * 특정 사용자(기업 사용자)의 채용공고 수를 카운트합니다.
     *
     * 기업 사용자 기반 채용공고 관리에서 사용되며, 해당 사용자가 등록한
     * 총 채용공고 수를 파악할 때 활용됩니다.
     *
     * @param user 카운트할 기업 사용자 엔티티
     * @return long 해당 사용자의 채용공고 수
     */
    @Query("SELECT COUNT(j) FROM JobPosting j WHERE j.company.user = :user")
    long countByCompanyUser(@Param("user") org.jbd.backend.user.domain.User user);

    /**
     * 특정 사용자(기업 사용자)의 특정 상태 채용공고 수를 카운트합니다.
     *
     * 기업 사용자의 상태별 채용공고 통계를 제공하여 기업의 채용 현황을
     * 세밀하게 분석할 때 사용됩니다.
     *
     * @param user 카운트할 기업 사용자 엔티티
     * @param status 카운트할 채용공고 상태
     * @return long 해당 사용자의 해당 상태 채용공고 수
     */
    @Query("SELECT COUNT(j) FROM JobPosting j WHERE j.company.user = :user AND j.status = :status")
    long countByCompanyUserAndStatus(@Param("user") org.jbd.backend.user.domain.User user,
                                     @Param("status") JobPosting.JobStatus status);

    /**
     * 특정 사용자(기업 사용자)의 모든 채용공고를 조회합니다.
     *
     * 기업 사용자가 자신이 등록한 모든 채용공고를 확인할 때 사용됩니다.
     * 마이페이지나 기업 대시보드에서 활용됩니다.
     *
     * @param user 조회할 기업 사용자 엔티티
     * @return List<JobPosting> 해당 사용자의 모든 채용공고 목록
     */
    @Query("SELECT j FROM JobPosting j WHERE j.company.user = :user")
    List<JobPosting> findByCompanyUser(@Param("user") org.jbd.backend.user.domain.User user);

    @Query("SELECT j, COUNT(ja) as applicationCount " +
            "FROM JobPosting j " +
            "LEFT JOIN JobApplication ja ON j = ja.jobPosting " +
            "WHERE j.company.user = :user " +
            "GROUP BY j " +
            "ORDER BY j.createdAt DESC")
    List<Object[]> findByCompanyUserWithApplicationCountOrderByCreatedAtDesc(
            @Param("user") org.jbd.backend.user.domain.User user, Pageable pageable);

    @Query("SELECT j, COUNT(ja) as applicationCount " +
            "FROM JobPosting j " +
            "LEFT JOIN JobApplication ja ON j = ja.jobPosting " +
            "WHERE j.company.user = :user " +
            "GROUP BY j " +
            "ORDER BY j.view_count DESC")
    List<Object[]> findByCompanyUserWithApplicationCountOrderByViewCountDesc(
            @Param("user") org.jbd.backend.user.domain.User user, Pageable pageable);

    /**
     * 특정 상태이면서 마감일이 지난 채용공고를 조회합니다.
     *
     * 마감일이 지난 채용공고들을 자동으로 마감 상태로 변경하는
     * 스케줄링 작업에서 사용되는 중복 메서드입니다.
     *
     * @param status 조회할 채용공고 상태
     * @param date 기준 날짜 (이 날짜보다 이전에 마감되는 채용공고)
     * @return List<JobPosting> 마감일이 지난 채용공고 목록
     */
    @Query("SELECT j FROM JobPosting j WHERE j.status = :status AND j.deadline_date < :date")
    List<JobPosting> findByStatusAndDeadlineDateBefore(@Param("status") JobPosting.JobStatus status, @Param("date") LocalDate date);
}