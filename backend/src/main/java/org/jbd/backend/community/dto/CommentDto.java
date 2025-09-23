package org.jbd.backend.community.dto;

import org.jbd.backend.community.domain.Comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CommentDto {

    public static class CreateRequest {
        private String content;
        private Long postId;
        private Long parentCommentId;

        public CreateRequest() {}

        public CreateRequest(String content, Long postId, Long parentCommentId) {
            this.content = content;
            this.postId = postId;
            this.parentCommentId = parentCommentId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Long getPostId() {
            return postId;
        }

        public void setPostId(Long postId) {
            this.postId = postId;
        }

        public Long getParentCommentId() {
            return parentCommentId;
        }

        public void setParentCommentId(Long parentCommentId) {
            this.parentCommentId = parentCommentId;
        }
    }

    public static class UpdateRequest {
        private String content;

        public UpdateRequest() {}

        public UpdateRequest(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class Response {
        private Long id;
        private String content;
        private String authorName;
        private Long postId;
        private Long parentCommentId;
        private Long likeCount;
        private List<Response> childComments;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Response() {}

        public Response(Comment comment) {
            this.id = comment.getId();
            this.content = comment.getContent();
            this.authorName = comment.getAuthor().getName(); // User 엔티티의 임시 getName() 메서드 사용
            this.postId = comment.getPost().getId();
            this.parentCommentId = comment.getParentComment() != null ? comment.getParentComment().getId() : null;
            this.likeCount = comment.getLikeCount();
            this.createdAt = comment.getCreatedAt();
            this.updatedAt = comment.getUpdatedAt();
        }

        public Response(Comment comment, List<Comment> childComments) {
            this(comment);
            this.childComments = childComments.stream()
                    .map(Response::new)
                    .collect(Collectors.toList());
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getAuthorName() {
            return authorName;
        }

        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }

        public Long getPostId() {
            return postId;
        }

        public void setPostId(Long postId) {
            this.postId = postId;
        }

        public Long getParentCommentId() {
            return parentCommentId;
        }

        public void setParentCommentId(Long parentCommentId) {
            this.parentCommentId = parentCommentId;
        }

        public Long getLikeCount() {
            return likeCount;
        }

        public void setLikeCount(Long likeCount) {
            this.likeCount = likeCount;
        }

        public List<Response> getChildComments() {
            return childComments;
        }

        public void setChildComments(List<Response> childComments) {
            this.childComments = childComments;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}