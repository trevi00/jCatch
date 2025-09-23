package org.jbd.backend.community.domain;

import jakarta.persistence.*;
import org.jbd.backend.common.entity.BaseEntity;
import org.jbd.backend.user.domain.User;

@Entity
@Table(name = "comments")
public class Comment extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;
    
    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;
    
    protected Comment() {}
    
    public Comment(String content, User author, Post post) {
        this.content = content;
        this.author = author;
        this.post = post;
    }
    
    public Comment(String content, User author, Post post, Comment parentComment) {
        this.content = content;
        this.author = author;
        this.post = post;
        this.parentComment = parentComment;
    }
    
    public void updateContent(String content) {
        this.content = content;
    }
    
    public void delete() {
        super.delete();
        this.content = "삭제된 댓글입니다.";
    }
    
    public void incrementLikeCount() {
        this.likeCount++;
    }
    
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
    
    public Long getId() {
        return id;
    }
    
    public String getContent() {
        return content;
    }
    
    public User getAuthor() {
        return author;
    }
    
    public Post getPost() {
        return post;
    }
    
    public Comment getParentComment() {
        return parentComment;
    }
    
    public Long getLikeCount() {
        return likeCount;
    }
}