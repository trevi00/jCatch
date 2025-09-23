package org.jbd.backend.community.dto;

import org.jbd.backend.community.domain.Category;

public class CategoryDto {

    public static class Request {
        private String name;
        private String description;
        private Integer displayOrder;

        public Request() {}

        public Request(String name, String description, Integer displayOrder) {
            this.name = name;
            this.description = description;
            this.displayOrder = displayOrder;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }
    }

    public static class Response {
        private Long id;
        private String name;
        private String description;
        private Long postCount;
        private Integer displayOrder;
        private Boolean active;

        public Response() {}

        public Response(Category category) {
            this.id = category.getId();
            this.name = category.getName();
            this.description = category.getDescription();
            this.postCount = 0L; // 기본값, 나중에 실제 카운트로 설정
            this.displayOrder = category.getDisplayOrder();
            this.active = category.isActive();
        }

        public Response(Category category, Long postCount) {
            this.id = category.getId();
            this.name = category.getName();
            this.description = category.getDescription();
            this.postCount = postCount != null ? postCount : 0L;
            this.displayOrder = category.getDisplayOrder();
            this.active = category.isActive();
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Long getPostCount() {
            return postCount;
        }

        public void setPostCount(Long postCount) {
            this.postCount = postCount;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }
}