package org.jbd.backend.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jbd.backend.job.domain.enums.ExperienceLevel;
import org.jbd.backend.job.domain.enums.JobStatus;
import org.jbd.backend.job.domain.enums.JobType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostingSearchDto {

    private String keyword;
    private String title;
    private String companyName;
    private String location;
    private JobType jobType;
    private ExperienceLevel experienceLevel;
    private Integer salaryMin;
    private Integer salaryMax;
    private Boolean salaryNegotiable;
    private String department;
    private String field;
    private List<Long> companyUserIds;
    private JobStatus status;
    private Boolean isRemotePossible;
    private String requiredSkills;

    @Min(value = 0, message = "페이지는 0 이상이어야 합니다.")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "사이즈는 1 이상이어야 합니다.")
    @Max(value = 100, message = "사이즈는 100 이하여야 합니다.")
    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sortBy = "createdAt";
    @Builder.Default
    private String sortDirection = "desc";
}