package org.jbd.backend.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.jbd.backend.job.domain.JobPosting;
import org.jbd.backend.job.domain.enums.ExperienceLevel;
import org.jbd.backend.job.domain.enums.JobStatus;
import org.jbd.backend.job.domain.enums.JobType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostingResponseDto {

    private Long id;
    private Long companyId;
    private String companyName;
    private String title;
    private String location;
    private JobType jobType;
    private ExperienceLevel experienceLevel;
    private String department;
    private String field;
    private String description;
    private String qualifications;
    private String requiredSkills;
    private String benefits;
    private Integer minSalary;
    private Integer maxSalary;
    private Boolean salaryNegotiable;
    private String workingHours;
    private Boolean isRemotePossible;
    private String contactEmail;
    private String contactPhone;
    private JobStatus status;
    private Long viewCount;
    private Long applicationCount;
    private LocalDate deadlineDate;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static JobPostingResponseDto from(JobPosting jobPosting) {
        // Company 정보를 안전하게 가져오기
        Long companyId = null;

        // Hibernate.isInitialized()를 사용하여 프록시가 초기화되었는지 확인
        if (jobPosting.getCompany() != null &&
                Hibernate.isInitialized(jobPosting.getCompany())) {
            companyId = jobPosting.getCompany().getCompany_id();
        }

        return JobPostingResponseDto.builder()
                .id(jobPosting.getJob_id())
                .companyId(companyId)
                .companyName(jobPosting.getCompany_name())
                .title(jobPosting.getTitle())
                .location(jobPosting.getLocation())
                .jobType(org.jbd.backend.job.domain.enums.JobType.valueOf(jobPosting.getJob_type().name()))
                .experienceLevel(org.jbd.backend.job.domain.enums.ExperienceLevel.valueOf(jobPosting.getExperience_level().name()))
                .department(jobPosting.getDepartment())
                .field(jobPosting.getField())
                .description(jobPosting.getDescription())
                .qualifications(jobPosting.getQualifications())
                .requiredSkills(jobPosting.getRequired_skills())
                .benefits(jobPosting.getBenefits())
                .minSalary(jobPosting.getSalary_min() != null ? jobPosting.getSalary_min().intValue() : null)
                .maxSalary(jobPosting.getSalary_max() != null ? jobPosting.getSalary_max().intValue() : null)
                .salaryNegotiable(jobPosting.getSalary_negotiable())
                .workingHours(jobPosting.getWorking_hours())
                .isRemotePossible(jobPosting.getIs_remote_possible())
                .contactEmail(jobPosting.getContact_email())
                .contactPhone(jobPosting.getContact_phone())
                .status(org.jbd.backend.job.domain.enums.JobStatus.valueOf(jobPosting.getStatus().name()))
                .viewCount(jobPosting.getView_count() != null ? Long.valueOf(jobPosting.getView_count()) : 0L)
                .applicationCount(jobPosting.getApplication_count() != null ? Long.valueOf(jobPosting.getApplication_count()) : 0L)
                .deadlineDate(jobPosting.getDeadline_date())
                .publishedAt(jobPosting.getPublished_at())
                .createdAt(jobPosting.getCreatedAt())
                .updatedAt(jobPosting.getUpdatedAt())
                .build();
    }
}