package org.jbd.backend.config;

import org.jbd.backend.community.domain.Category;
import org.jbd.backend.community.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        initializeCategories();
    }

    private void initializeCategories() {
        // 카테고리가 이미 존재하면 초기화하지 않음
        if (categoryRepository.count() > 0) {
            return;
        }

        // 6개 커뮤니티 카테고리 생성
        createCategory("취업정보", "취업 관련 정보 공유", 1);
        createCategory("면접정보", "면접 관련 정보 공유", 2);
        createCategory("Q&A", "질문과 답변", 3);
        createCategory("자유게시판", "자유로운 소통 공간 (AI 이미지 생성 및 감정분석 지원)", 4);
        createCategory("기업게시판", "기업 전용 게시판", 5);
        createCategory("공지", "관리자 공지사항", 6);

        System.out.println("✅ 커뮤니티 카테고리 초기화 완료");
    }

    private void createCategory(String name, String description, int displayOrder) {
        Category category = new Category(name, description);
        category.updateDisplayOrder(displayOrder);
        
        categoryRepository.save(category);
        System.out.println("카테고리 생성: " + name);
    }
}