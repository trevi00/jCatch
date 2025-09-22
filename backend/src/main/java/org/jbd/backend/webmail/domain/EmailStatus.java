package org.jbd.backend.webmail.domain;

public enum EmailStatus {
    SENT,           // 발송 완료
    FAILED,         // 발송 실패
    PENDING,        // 발송 대기
    DELIVERED,      // 전달 완료
    BOUNCED,        // 반송
    OPENED,         // 열람
    CLICKED         // 링크 클릭
}