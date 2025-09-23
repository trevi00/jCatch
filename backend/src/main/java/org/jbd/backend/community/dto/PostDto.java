package org.jbd.backend.community.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jbd.backend.community.domain.Post;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PostDto {

    public static class CreateRequest {
        private String title;
        private String content;
        private Long categoryId;
        @JsonProperty("imageUrl")
        private String imageUrl;
        @JsonProperty("imagePrompt")
        private String imagePrompt;

        public CreateRequest() {}

        public CreateRequest(String title, String content, Long categoryId) {
            this.title = title;
            this.content = content;
            this.categoryId = categoryId;
        }

        public CreateRequest(String title, String content, Long categoryId, String imageUrl) {
            this.title = title;
            this.content = content;
            this.categoryId = categoryId;
            this.imageUrl = imageUrl;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getImagePrompt() {
            return imagePrompt;
        }

        public void setImagePrompt(String imagePrompt) {
            this.imagePrompt = imagePrompt;
        }
    }

    public static class UpdateRequest {
        private String title;
        private String content;
        private Long categoryId;
        @JsonProperty("imageUrl")
        private String imageUrl;

        public UpdateRequest() {}

        public UpdateRequest(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public UpdateRequest(String title, String content, Long categoryId, String imageUrl) {
            this.title = title;
            this.content = content;
            this.categoryId = categoryId;
            this.imageUrl = imageUrl;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    public static class Response {
        private Long id;
        private String title;
        private String content;
        private String categoryName;
        private Long categoryId;
        private String authorName;
        private Long authorId;
        private Long viewCount;
        private Long likeCount;
        private Boolean noticePost;
        private Boolean pinned;
        private String imageUrl;
        private Double sentimentScore;
        private String sentimentLabel;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Response() {}

        public Response(Post post) {
            this.id = post.getId();
            this.title = post.getTitle();
            this.content = post.getContent();
            this.categoryName = post.getCategory().getName();
            this.categoryId = post.getCategory().getId();
            this.authorName = post.getAuthor().getName(); // User 엔티티의 임시 getName() 메서드 사용
            this.authorId = post.getAuthor().getId();
            this.viewCount = post.getViewCount();
            this.likeCount = post.getLikeCount();
            this.noticePost = post.isNoticePost();
            this.pinned = post.isPinned();
            this.imageUrl = post.getImageUrl();
            this.sentimentScore = post.getSentimentScore();
            this.sentimentLabel = post.getSentimentLabel();
            this.createdAt = post.getCreatedAt();
            this.updatedAt = post.getUpdatedAt();
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getAuthorName() {
            return authorName;
        }

        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }

        public Long getAuthorId() {
            return authorId;
        }

        public void setAuthorId(Long authorId) {
            this.authorId = authorId;
        }

        public Long getViewCount() {
            return viewCount;
        }

        public void setViewCount(Long viewCount) {
            this.viewCount = viewCount;
        }

        public Long getLikeCount() {
            return likeCount;
        }

        public void setLikeCount(Long likeCount) {
            this.likeCount = likeCount;
        }

        public Boolean getNoticePost() {
            return noticePost;
        }

        public void setNoticePost(Boolean noticePost) {
            this.noticePost = noticePost;
        }

        public Boolean getPinned() {
            return pinned;
        }

        public void setPinned(Boolean pinned) {
            this.pinned = pinned;
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

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public Double getSentimentScore() {
            return sentimentScore;
        }

        public void setSentimentScore(Double sentimentScore) {
            this.sentimentScore = sentimentScore;
        }

        public String getSentimentLabel() {
            return sentimentLabel;
        }

        public void setSentimentLabel(String sentimentLabel) {
            this.sentimentLabel = sentimentLabel;
        }
    }

    public static class PageResponse {
        private List<Response> posts;
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;

        public PageResponse() {}

        public PageResponse(Page<Post> page) {
            this.posts = page.getContent().stream()
                    .map(Response::new)
                    .collect(Collectors.toList());
            this.pageNumber = page.getNumber();
            this.pageSize = page.getSize();
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
            this.first = page.isFirst();
            this.last = page.isLast();
        }

        public List<Response> getPosts() {
            return posts;
        }

        public void setPosts(List<Response> posts) {
            this.posts = posts;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public boolean isFirst() {
            return first;
        }

        public void setFirst(boolean first) {
            this.first = first;
        }

        public boolean isLast() {
            return last;
        }

        public void setLast(boolean last) {
            this.last = last;
        }
    }
}