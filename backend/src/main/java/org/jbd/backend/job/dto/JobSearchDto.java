package org.jbd.backend.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jbd.backend.job.domain.enums.ExperienceLevel;
import org.jbd.backend.job.domain.enums.JobType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchDto {

    private String keyword;
    private String location;
    private JobType jobType;
    private ExperienceLevel experienceLevel;
    private Integer minSalary;
    private Integer maxSalary;
    private Boolean isRemotePossible;
    private String field;
    private String companyName;
}