package org.jbd.backend.community.domain;

import jakarta.persistence.*;
import org.jbd.backend.common.entity.BaseEntity;
import org.jbd.backend.user.domain.User;

/**
 * 커뮤니티 게시글 도메인 엔티티
 *
 * 사용자가 작성하는 커뮤니티 게시글을 관리하는 도메인 객체입니다.
 * 취업 정보 공유, 질문과 답변, 경험 공유 등 다양한 콘텐츠를 지원하며
 * AI 감정 분석을 통한 게시글 톤 분석 기능을 포함합니다.
 *
 * 도메인 관계:
 * - User (N:1): 게시글 작성자
 * - Category (N:1): 게시글이 속한 카테고리
 * - Comment (1:N): 게시글에 달린 댓글들
 * - PostLike (1:N): 게시글 좋아요 (미구현)
 *
 * 주요 기능:
 * - 조회수 자동 증가: 게시글 조회 시 카운터 증가
 * - 좋아요 시스템: 사용자 반응 측정
 * - 공지사항 및 상단 고정: 중요 게시글 우선 노출
 * - 이미지 첨부: AI 생성 이미지 등 시각적 콘텐츠 지원
 * - 감정 분석: AI 서비스를 통한 텍스트 감정 점수 및 라벨링
 *
 * 비즈니스 규칙:
 * - 모든 사용자가 게시글을 작성할 수 있음 (인증 필요)
 * - 작성자와 관리자만 게시글을 수정/삭제할 수 있음
 * - 공지사항과 상단 고정은 관리자만 설정 가능
 * - 감정 분석은 게시글 작성/수정 시 자동으로 수행됨
 *
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 * @see User
 * @see Category
 * @see Comment
 * @see BaseEntity
 */
@Entity
@Table(name = "posts")
public class Post extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;
    
    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;
    
    @Column(name = "is_notice", nullable = false)
    private Boolean noticePost = false;
    
    @Column(name = "is_pinned", nullable = false)
    private Boolean pinned = false;
    
    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    
    @Column(name = "sentiment_score")
    private Double sentimentScore;
    
    @Column(name = "sentiment_label", length = 20)
    private String sentimentLabel;
    
    protected Post() {}
    
    public Post(String title, String content, User author, Category category) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.category = category;
    }
    
    public void updatePost(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void updatePost(String title, String content, Category category, String imageUrl) {
        this.title = title;
        this.content = content;
        if (category != null) {
            this.category = category;
        }
        this.imageUrl = imageUrl;
    }
    
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    public void incrementLikeCount() {
        this.likeCount++;
    }
    
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
    
    public void markAsNotice() {
        this.noticePost = true;
    }
    
    public void pin() {
        this.pinned = true;
    }
    
    public void unpin() {
        this.pinned = false;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public void updateSentiment(Double score, String label) {
        this.sentimentScore = score;
        this.sentimentLabel = label;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getContent() {
        return content;
    }
    
    public User getAuthor() {
        return author;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public Long getViewCount() {
        return viewCount;
    }
    
    public Long getLikeCount() {
        return likeCount;
    }
    
    public boolean isNoticePost() {
        return noticePost;
    }
    
    public boolean isPinned() {
        return pinned;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public Double getSentimentScore() {
        return sentimentScore;
    }
    
    public String getSentimentLabel() {
        return sentimentLabel;
    }
}