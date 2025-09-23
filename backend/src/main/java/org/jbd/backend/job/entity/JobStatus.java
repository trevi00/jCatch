package org.jbd.backend.job.entity;

public enum JobStatus {
    DRAFT("임시저장"),
    PUBLISHED("게시중"),
    CLOSED("마감"),
    EXPIRED("기간만료");

    private final String description;

    JobStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}