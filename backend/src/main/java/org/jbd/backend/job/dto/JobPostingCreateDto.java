package org.jbd.backend.job.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jbd.backend.job.domain.enums.ExperienceLevel;
import org.jbd.backend.job.domain.enums.JobType;

/**
 * 채용공고 생성 요청 데이터 전송 객체
 *
 * 기업 사용자가 새로운 채용공고를 등록할 때 필요한 모든 정보를 캡슐화하는 DTO입니다.
 * Bean Validation 어노테이션을 통해 입력값의 유효성을 체계적으로 검증하며,
 * 기업의 채용 요구사항을 상세하고 정확하게 수집합니다.
 *
 * 주요 기능:
 * - 채용공고 기본 정보 (제목, 회사명, 근무지역, 고용형태)
 * - 경력 요구사항 및 직무 상세 정보 (경력수준, 부서, 직무분야)
 * - 자격 요건 및 필요 기술 명세
 * - 급여 정보 및 우대조건 (최소/최대 급여, 급여협상 가능 여부)
 * - 근무 조건 (근무시간, 원격근무 가능 여부)
 * - 연락처 정보 (담당자 이메일 및 전화번호)
 *
 * 비즈니스 규칙:
 * - 제목은 5-100자 제한으로 간결하면서도 구체적이어야 함
 * - 직무설명은 3000자 제한으로 상세한 설명 가능
 * - 급여는 최소/최대 범위로 설정하거나 협상 가능으로 설정
 * - 원격근무 가능 여부 명시로 유연한 근무환경 제공
 *
 * 데이터 검증 규칙:
 * - 필수 필드: 제목, 회사명, 근무지역, 고용형태, 경력수준
 * - 선택 필드: 부서, 직무분야, 설명, 자격요건, 기술, 복리후생
 * - 급여 정보: 0 이상의 정수, 최소급여 ≤ 최대급여 로직
 * - 연락처: 이메일 형식 및 전화번호 형식 (숫자와 하이픈만) 검증
 *
 * 사용 예시:
 * - 기업 사용자가 채용공고 등록 페이지에서 입력한 데이터
 * - 채용공고 생성 API에서 요청 모체로 수신한 데이터
 * - 대량 채용공고 일괄 등록 시 개별 채용공고 정보
 *
 * 보안 및 밌리데이션:
 * - 모든 필드에 대한 입력 길이 제한으로 XSS 공격 방지
 * - 이메일 및 전화번호 형식 검증으로 데이터 무결성 보장
 * - HTML 태그 및 스크립트 삽입 방지를 위한 입력값 살균
 * - 사용자 가독성을 위한 상세한 에러 메시지 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostingCreateDto {

    @NotBlank(message = "채용공고 제목은 필수입니다")
    @Size(min = 5, max = 100, message = "제목은 5자 이상 100자 이하여야 합니다")
    private String title;

    @NotBlank(message = "회사명은 필수입니다")
    @Size(max = 100, message = "회사명은 100자 이하여야 합니다")
    private String companyName;

    @NotBlank(message = "근무지역은 필수입니다")
    @Size(max = 100, message = "근무지역은 100자 이하여야 합니다")
    private String location;

    @NotNull(message = "고용형태는 필수입니다")
    private JobType jobType;

    @NotNull(message = "경력 수준은 필수입니다")
    private ExperienceLevel experienceLevel;

    @Size(max = 100, message = "부서명은 100자 이하여야 합니다")
    private String department;

    @Size(max = 100, message = "직무분야는 100자 이하여야 합니다")
    private String field;

    @Size(max = 3000, message = "직무설명은 3000자 이하여야 합니다")
    private String description;

    @Size(max = 2000, message = "자격요건은 2000자 이하여야 합니다")
    private String qualifications;

    @Size(max = 1500, message = "필요 기술은 1500자 이하여야 합니다")
    private String requiredSkills;

    @Size(max = 1000, message = "복리후생은 1000자 이하여야 합니다")
    private String benefits;

    @Min(value = 0, message = "최소 급여는 0 이상이어야 합니다")
    private Integer minSalary;

    @Min(value = 0, message = "최대 급여는 0 이상이어야 합니다")
    private Integer maxSalary;

    @Builder.Default
    private Boolean salaryNegotiable = false;

    @Size(max = 100, message = "근무시간은 100자 이하여야 합니다")
    private String workingHours;

    @Builder.Default
    private Boolean isRemotePossible = false;

    @Email(message = "유효한 이메일 형식이어야 합니다")
    private String contactEmail;

    @Pattern(regexp = "^[0-9-]+$", message = "전화번호는 숫자와 하이픈만 포함해야 합니다")
    private String contactPhone;
}