package org.jbd.backend.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jbd.backend.job.domain.enums.JobStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostingStatsDto {

    private Long jobId;
    private String title;
    private String companyName;
    private Long viewCount;
    private Long applicationCount;
    private JobStatus status;
    private LocalDateTime publishedAt;
    private LocalDate deadlineDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 계산된 통계 필드들
    private Long daysUntilDeadline;
    private Double viewToApplicationRatio;
    private Boolean isDeadlineApproaching; // 7일 이내
    private Boolean isExpired;
    private Boolean isActive;
}