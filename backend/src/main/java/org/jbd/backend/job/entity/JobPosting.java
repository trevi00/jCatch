package org.jbd.backend.job.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.AllArgsConstructor;
import org.jbd.backend.common.entity.BaseEntity;
import org.jbd.backend.company.domain.Company;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 채용 공고 도메인 엔티티입니다.
 * 기업이 등록하는 채용 공고 정보를 관리하며, 구직자들이 지원할 수 있는 채용 정보를 담고 있습니다.
 *
 * 도메인 관계:
 * - Company (N:1): 채용 공고를 등록한 기업
 * - JobApplication (1:N): 해당 공고에 대한 지원서들
 *
 * 주요 기능:
 * - 채용 공고 기본 정보 관리 (제목, 설명, 요구사항)
 * - 근무 조건 정보 (지역, 근무 형태, 경력 요구사항)
 * - 급여 정보 관리 (최소/최대 급여)
 * - 공고 상태 관리 (활성/비활성, 마감일)
 * - 조회수 및 통계 정보
 *
 * 비즈니스 규칙:
 * - 활성 상태인 공고만 일반 사용자에게 노출
 * - 마감일이 지난 공고는 자동으로 비활성화
 * - 기업 사용자만 공고 등록/수정 가능
 *
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 * @see Company
 * @see JobType
 * @see BaseEntity
 */
@Entity
@Table(name = "job_postings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class JobPosting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long job_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "preferred_qualifications", columnDefinition = "TEXT")
    private String preferred_qualifications;

    @Column(name = "location", length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type")
    private JobType job_type;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level")
    private ExperienceLevel experience_level;

    @Column(name = "salary_min", precision = 12)
    private BigDecimal salary_min;

    @Column(name = "salary_max", precision = 12)
    private BigDecimal salary_max;

    @Column(name = "salary_currency", length = 3)
    @Builder.Default
    private String salary_currency = "KRW";

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "posted_date")
    @Builder.Default
    private LocalDateTime posted_at = LocalDateTime.now();

    @Column(name = "deadline")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "view_count")
    @Builder.Default
    private Integer view_count = 0;

    @Column(name = "application_count")
    @Builder.Default
    private Integer application_count = 0;

    @Column(name = "company_name", length = 200)
    private String company_name;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "field", length = 100)
    private String field;

    @Column(name = "qualifications", columnDefinition = "TEXT")
    private String qualifications;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String required_skills;

    @Column(name = "salary_negotiable")
    @Builder.Default
    private Boolean salary_negotiable = false;

    @Column(name = "working_hours", length = 100)
    private String working_hours;

    @Column(name = "is_remote_possible")
    @Builder.Default
    private Boolean is_remote_possible = false;

    @Column(name = "contact_email", length = 100)
    private String contact_email;

    @Column(name = "contact_phone", length = 50)
    private String contact_phone;

    @Column(name = "job_title", length = 200)
    private String job_title;

    @Column(name = "published_at")
    private LocalDateTime published_at;

    @Column(name = "deadline_date")
    private LocalDate deadline_date;

    public enum JobType {
        FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP
    }

    public enum ExperienceLevel {
        ENTRY, JUNIOR, MID, SENIOR, LEAD
    }

    public enum JobStatus {
        ACTIVE, CLOSED, DRAFT
    }

    // 호환성을 위한 추가 getter들
    public String getContactEmail() {
        return contact_email;
    }

    public String getContactPhone() {
        return contact_phone;
    }

    public void setContactEmail(String email) {
        this.contact_email = email;
    }

    public void setContactPhone(String phone) {
        this.contact_phone = phone;
    }

    public void setDeadline_date(LocalDate deadline_date) {
        this.deadline_date = deadline_date;
    }

    public void updateBasicInfo(String title, String description, String requirements,
                                String preferred_qualifications, String location,
                                JobType job_type, ExperienceLevel experience_level) {
        this.title = title;
        this.description = description;
        this.requirements = requirements;
        this.preferred_qualifications = preferred_qualifications;
        this.location = location;
        this.job_type = job_type;
        this.experience_level = experience_level;
    }

    public void updateSalaryInfo(BigDecimal salary_min, BigDecimal salary_max, String salary_currency) {
        this.salary_min = salary_min;
        this.salary_max = salary_max;
        this.salary_currency = salary_currency;
    }

    public void setCompanyName(String company_name) {
        this.company_name = company_name;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Integer getViewCount() {
        return view_count;
    }

    public Integer getApplicationCount() {
        return application_count;
    }

    public LocalDateTime getPublishedAt() {
        return published_at != null ? published_at : posted_at;
    }

    public LocalDate getDeadlineDate() {
        return deadline;
    }

    public JobStatus getStatus() {
        return status;
    }

    // 서비스 호환성을 위한 맞춤 생성자
    public JobPosting(Company company, String title, String description,
                      JobType jobType, ExperienceLevel experienceLevel) {
        this.company = company;
        this.title = title;
        this.description = description;
        this.job_type = jobType;
        this.experience_level = experienceLevel;
        this.status = JobStatus.DRAFT;
        this.posted_at = LocalDateTime.now();
        this.view_count = 0;
        this.application_count = 0;
    }

    // 위치 파라미터가 포함된 생성자 (서명 다름)
    public JobPosting(Company company, String title, String location,
                      JobType jobType, ExperienceLevel experienceLevel, boolean useLocation) {
        this.company = company;
        this.title = title;
        this.location = location;
        this.job_type = jobType;
        this.experience_level = experienceLevel;
        this.status = JobStatus.DRAFT;
        this.posted_at = LocalDateTime.now();
        this.view_count = 0;
        this.application_count = 0;
        this.salary_currency = "KRW";
    }

    public void updateBenefits(String benefits) {
        this.benefits = benefits;
    }

    public void updateDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public void publish() {
        this.status = JobStatus.ACTIVE;
        this.posted_at = LocalDateTime.now();
    }

    public void close() {
        this.status = JobStatus.CLOSED;
    }

    public void draft() {
        this.status = JobStatus.DRAFT;
    }

    public void incrementViewCount() {
        this.view_count++;
    }

    public void incrementApplicationCount() {
        this.application_count++;
    }

    public void decrementApplicationCount() {
        if (this.application_count > 0) {
            this.application_count--;
        }
    }

    public boolean isActive() {
        return this.status == JobStatus.ACTIVE;
    }

    public boolean isClosed() {
        return this.status == JobStatus.CLOSED;
    }

    public boolean isDraft() {
        return this.status == JobStatus.DRAFT;
    }

    public boolean isExpired() {
        return this.deadline != null && LocalDate.now().isAfter(this.deadline);
    }

    public boolean canApply() {
        return isActive() && !isExpired();
    }

    // 언더스코어 필드명과 카멜케이스 기대사항을 위한 레거시 메서드 호환성
    public String getJobTitle() {
        return job_title != null ? job_title : title;
    }

    public String getQualifications() {
        return qualifications;
    }

    public String getRequired_skills() {
        return required_skills;
    }

    public String getRequiredSkills() {
        return required_skills;
    }

    public Boolean getSalary_negotiable() {
        return salary_negotiable;
    }

    public Boolean getSalaryNegotiable() {
        return salary_negotiable;
    }

    public String getWorking_hours() {
        return working_hours;
    }

    public Boolean getIs_remote_possible() {
        return is_remote_possible;
    }

    public String getContact_email() {
        return contact_email;
    }

    public String getContact_phone() {
        return contact_phone;
    }

    public LocalDate getDeadline_date() {
        return deadline;
    }

    public LocalDateTime getPublished_at() {
        return published_at;
    }


    public String getCompanyName() {
        return company_name;
    }

    public JobType getJobType() {
        return job_type;
    }

    public String getDepartment() {
        return department;
    }

    public String getField() {
        return field;
    }

    public ExperienceLevel getExperienceLevel() {
        return experience_level;
    }

    public BigDecimal getSalaryMin() {
        return salary_min;
    }

    public BigDecimal getSalaryMax() {
        return salary_max;
    }

    public void updateContent(String description, String qualifications, String required_skills, String benefits) {
        this.description = description;
        this.qualifications = qualifications;
        this.required_skills = required_skills;
        this.benefits = benefits;
    }

    public Long getId() {
        return job_id;
    }

    public void updateWorkingConditions(String workingHours, Boolean isRemotePossible) {
        this.working_hours = workingHours;
        this.is_remote_possible = isRemotePossible;
    }

    public void updateContactInfo(String contactEmail, String contactPhone) {
        this.contact_email = contactEmail;
        this.contact_phone = contactPhone;
    }

    public String getWorkingHours() {
        return working_hours;
    }

    public Boolean getIsRemotePossible() {
        return is_remote_possible;
    }
}