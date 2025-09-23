package org.jbd.backend.job.entity;

public enum ExperienceLevel {
    ENTRY_LEVEL("신입"),
    JUNIOR("경력 1-3년"),
    MID_LEVEL("경력 3-5년"),
    SENIOR("경력 5-10년"),
    EXPERT("경력 10년 이상"),
    MANAGER("관리자"),
    DIRECTOR("임원"),
    ANY("경력무관");

    private final String description;

    ExperienceLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}