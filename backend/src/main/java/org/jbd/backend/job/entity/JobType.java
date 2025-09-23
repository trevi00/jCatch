package org.jbd.backend.job.entity;

/**
 * 채용 공고의 근무 형태를 정의하는 열거형입니다.
 * 다양한 근무 형태와 고용 유형을 구분하여 채용 공고를 분류하는 데 사용됩니다.
 *
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 */
public enum JobType {
    /** 정규직 - 무기한 고용 계약 */
    FULL_TIME("정규직"),
    /** 파트타임 - 시간제 근무 */
    PART_TIME("파트타임"),
    /** 계약직 - 정해진 기간 동안의 고용 계약 */
    CONTRACT("계약직"),
    /** 인턴십 - 실무 경험을 위한 단기 근무 */
    INTERNSHIP("인턴십"),
    /** 프리랜서 - 독립적인 업무 수행 */
    FREELANCE("프리랜서"),
    /** 프로젝트 기반 - 특정 프로젝트 완료까지의 근무 */
    PROJECT_BASED("프로젝트 기반"),
    /** 임시직 - 단기간 임시 근무 */
    TEMPORARY("임시직"),
    /** 원격근무 - 재택근무 또는 원격지 근무 */
    REMOTE("원격근무"),
    /** 하이브리드 - 사무실과 원격 근무의 혼합 */
    HYBRID("하이브리드");

    /** 근무 형태에 대한 한글 설명 */
    private final String description;

    /**
     * JobType 생성자
     *
     * @param description 근무 형태에 대한 한글 설명
     */
    JobType(String description) {
        this.description = description;
    }

    /**
     * 근무 형태에 대한 한글 설명을 반환합니다.
     *
     * @return 근무 형태의 한글 설명
     */
    public String getDescription() {
        return description;
    }
}