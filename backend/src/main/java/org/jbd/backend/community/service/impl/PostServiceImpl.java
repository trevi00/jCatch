package org.jbd.backend.community.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jbd.backend.ai.client.AIServiceClient;
import org.jbd.backend.ai.dto.ImageGenerationDto;
import org.jbd.backend.ai.dto.SentimentAnalysisDto;
import org.jbd.backend.common.exception.ResourceNotFoundException;
import org.jbd.backend.community.domain.Category;
import org.jbd.backend.community.domain.Post;
import org.jbd.backend.community.dto.PostDto;
import org.jbd.backend.community.repository.CategoryRepository;
import org.jbd.backend.community.repository.PostRepository;
import org.jbd.backend.community.service.PostService;
import org.jbd.backend.user.domain.User;
import org.jbd.backend.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AIServiceClient aiServiceClient;

    public PostServiceImpl(PostRepository postRepository, CategoryRepository categoryRepository,
                          UserRepository userRepository, AIServiceClient aiServiceClient) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.aiServiceClient = aiServiceClient;
    }

    @Override
    public PostDto.PageResponse getAllPosts(Pageable pageable) {
        var posts = postRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
        return new PostDto.PageResponse(posts);
    }

    @Override
    @Transactional
    public PostDto.Response createPost(PostDto.CreateRequest request, String authorEmail) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Post post = new Post(request.getTitle(), request.getContent(), author, category);

        // 이미지 URL이 프론트엔드에서 전달된 경우 설정
        if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            post.setImageUrl(request.getImageUrl());
        }

        // 게시글 먼저 저장
        Post savedPost = postRepository.save(post);

        // EAGER 로딩으로 인해 연관 객체가 이미 로딩됨
        PostDto.Response response = new PostDto.Response(savedPost);

        // 감정 분석 수행
        performSentimentAnalysisInNewTransaction(savedPost.getId());

        // 특정 카테고리인 경우 이미지 생성 (이미 이미지가 있으면 건너뜀)
        if (savedPost.getImageUrl() == null || savedPost.getImageUrl().trim().isEmpty()) {
            // imagePrompt가 있으면 사용, 없으면 자동 생성
            String imagePrompt = request.getImagePrompt();
            if (imagePrompt != null && !imagePrompt.trim().isEmpty()) {
                generateImageWithCustomPromptAsync(savedPost, imagePrompt);
            } else {
                generateImageIfNeededAsync(savedPost, category);
            }
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto.Response getPostById(Long id) {
        Post post = postRepository.findByIdWithCategoryAndAuthor(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        
        return new PostDto.Response(post);
    }

    @Override
    public PostDto.Response incrementViewCount(Long id) {
        Post post = postRepository.findByIdWithCategoryAndAuthor(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        
        post.incrementViewCount();
        Post savedPost = postRepository.save(post);
        
        return new PostDto.Response(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto.PageResponse getPostsByCategory(Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        List<Post> posts = postRepository.findByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(category, pageable);
        
        return convertToPageResponse(posts, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto.PageResponse searchPostsByTitle(String keyword, Pageable pageable) {
        List<Post> posts = postRepository.findByTitleContainingIgnoreCaseAndIsDeletedFalseOrderByCreatedAtDesc(keyword, pageable);
        return convertToPageResponse(posts, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto.PageResponse getPostsByAuthor(String authorEmail, Pageable pageable) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));

        List<Post> posts = postRepository.findByAuthorAndIsDeletedFalseOrderByCreatedAtDesc(author, pageable);
        return convertToPageResponse(posts, pageable);
    }

    @Override
    @Transactional
    public PostDto.Response updatePost(Long id, PostDto.UpdateRequest request, String authorEmail) {
        // JOIN FETCH로 연관 엔티티를 즉시 로딩
        Post post = postRepository.findByIdWithFullData(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        User requestUser = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));

        // 작성자 확인
        if (!post.getAuthor().equals(requestUser)) {
            throw new IllegalArgumentException("Only the author can update this post");
        }

        // 카테고리가 제공된 경우에만 업데이트
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        // 게시글 업데이트
        post.updatePost(request.getTitle(), request.getContent(), category, request.getImageUrl());
        Post savedPost = postRepository.save(post);

        // EAGER 로딩으로 인해 연관 객체가 이미 로딩됨
        PostDto.Response response = new PostDto.Response(savedPost);

        // 감정 분석 수행
        performSentimentAnalysisInNewTransaction(savedPost.getId());

        return response;
    }

    @Override
    @Transactional
    public void deletePost(Long id, String authorEmail) {
        // JOIN FETCH로 연관 엔티티를 즉시 로딩
        Post post = postRepository.findByIdWithFullData(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        User requestUser = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));

        // EAGER 로딩으로 인해 연관 객체가 이미 로딩됨

        // 작성자이거나 관리자인 경우만 삭제 가능
        if (!post.getAuthor().equals(requestUser) &&
            !requestUser.getUserType().equals(org.jbd.backend.user.domain.enums.UserType.ADMIN)) {
            throw new IllegalArgumentException("Only the author or admin can delete this post");
        }

        post.delete();
        postRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto.Response> getPopularPosts(Long categoryId, int limit) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        Pageable pageable = PageRequest.of(0, limit);
        List<Post> posts = postRepository.findByCategoryAndIsDeletedFalseOrderByViewCountDesc(category, pageable);
        
        return posts.stream()
                .map(PostDto.Response::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto.PageResponse getPostsBySentiment(String sentimentLabel, Pageable pageable) {
        List<Post> posts = postRepository.findBySentimentLabelAndIsDeletedFalseOrderByCreatedAtDesc(sentimentLabel, pageable);
        return convertToPageResponse(posts, pageable);
    }

    @Override
    @Transactional(readOnly = true)  
    public PostDto.PageResponse getPostsByCategoryAndSentiment(Long categoryId, String sentimentLabel, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        List<Post> posts = postRepository.findByCategoryAndSentimentLabelAndIsDeletedFalseOrderByCreatedAtDesc(
                category, sentimentLabel, pageable);
        return convertToPageResponse(posts, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto.PageResponse getPostsBySentimentScore(Double minScore, Double maxScore, Pageable pageable) {
        List<Post> posts = postRepository.findBySentimentScoreBetweenAndIsDeletedFalseOrderByCreatedAtDesc(
                minScore, maxScore, pageable);
        return convertToPageResponse(posts, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto.PageResponse getPostsWithImages(Pageable pageable) {
        List<Post> posts = postRepository.findPostsWithImagesOrderByCreatedAtDesc(pageable);
        return convertToPageResponse(posts, pageable);
    }

    /**
     * 게시글 감정 분석 수행
     */
    private void performSentimentAnalysis(Post post) {
        try {
            // 제목과 내용을 합쳐서 분석
            String textToAnalyze = post.getTitle() + " " + post.getContent();
            
            SentimentAnalysisDto.AnalyzeResponse response = 
                aiServiceClient.analyzeSentiment(textToAnalyze, "ko");
            
            if (response.success() && response.data() != null) {
                post.updateSentiment(response.data().score(), response.data().label());
                log.info("Sentiment analysis completed for post: {} - Label: {}, Score: {}", 
                        post.getId(), response.data().label(), response.data().score());
            }
        } catch (Exception e) {
            log.warn("Failed to analyze sentiment for post: {}, error: {}", post.getId(), e.getMessage());
        }
    }
    
    /**
     * 특정 카테고리의 경우 이미지 생성
     */
    private void generateImageIfNeeded(Post post, Category category) {
        try {
            // 이미지 생성이 필요한 카테고리들 (예: 창작, 디자인, 프로젝트 등)
            if (shouldGenerateImage(category.getName())) {
                String imagePrompt = createImagePrompt(post.getTitle(), post.getContent());
                
                ImageGenerationDto.GenerateResponse response = 
                    aiServiceClient.generateImage(imagePrompt, "digital-art", "1024x1024", 1);
                
                if (response.isSuccess() && response.getData() != null && 
                    response.getData().getImageUrl() != null && !response.getData().getImageUrl().isEmpty()) {
                    
                    String imageUrl = response.getData().getImageUrl();
                    post.setImageUrl(imageUrl);
                    log.info("Image generated for post: {} - URL: {}", post.getId(), imageUrl);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to generate image for post: {}, error: {}", post.getId(), e.getMessage());
        }
    }
    
    /**
     * 이미지 생성이 필요한 카테고리인지 확인
     */
    private boolean shouldGenerateImage(String categoryName) {
        return categoryName != null && (
            categoryName.contains("창작") || 
            categoryName.contains("디자인") || 
            categoryName.contains("프로젝트") ||
            categoryName.contains("포트폴리오") ||
            categoryName.equalsIgnoreCase("showcase") ||
            categoryName.equalsIgnoreCase("creative")
        );
    }
    
    /**
     * 게시글 내용을 바탕으로 이미지 생성 프롬프트 생성
     */
    private String createImagePrompt(String title, String content) {
        // 내용에서 키워드 추출하여 프롬프트 생성
        String prompt = "A creative illustration representing: " + title;
        
        // 내용이 너무 긴 경우 요약
        if (content.length() > 100) {
            prompt += ". " + content.substring(0, 100) + "...";
        } else {
            prompt += ". " + content;
        }
        
        return prompt + " in a modern, professional style";
    }

    /**
     * 새로운 트랜잭션에서 감정 분석 수행 (Hibernate 세션 문제 방지)
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void performSentimentAnalysisInNewTransaction(Long postId) {
        try {
            // 새로운 트랜잭션에서 Post를 다시 조회
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

            // 제목과 내용을 합쳐서 분석
            String textToAnalyze = post.getTitle() + " " + post.getContent();

            SentimentAnalysisDto.AnalyzeResponse response =
                aiServiceClient.analyzeSentiment(textToAnalyze, "ko");

            if (response.success() && response.data() != null) {
                String sentimentLabel = response.data().label();
                Double sentimentScore = response.data().score();

                // AI 서비스 응답이 null인 경우 기본값 설정
                if (sentimentLabel == null || sentimentScore == null) {
                    log.warn("Sentiment analysis returned null data for post: {}", post.getId());
                    post.updateSentiment(0.0, "neutral");
                } else {
                    post.updateSentiment(sentimentScore, sentimentLabel.toLowerCase());
                }
                postRepository.save(post);
                log.info("Sentiment analysis completed for post: {} - Label: {}, Score: {}",
                        post.getId(), post.getSentimentLabel(), post.getSentimentScore());
            }
        } catch (Exception e) {
            log.warn("Failed to analyze sentiment for post: {}, error: {}", postId, e.getMessage());
        }
    }

    /**
     * 게시글 감정 분석 비동기 수행
     */
    private void performSentimentAnalysisAsync(Post post) {
        try {
            // 제목과 내용을 합쳐서 분석
            String textToAnalyze = post.getTitle() + " " + post.getContent();
            
            SentimentAnalysisDto.AnalyzeResponse response = 
                aiServiceClient.analyzeSentiment(textToAnalyze, "ko");
            
            if (response.success() && response.data() != null) {
                String sentimentLabel = response.data().label();
                Double sentimentScore = response.data().score();
                
                // AI 서비스 응답이 null인 경우 기본값 설정
                if (sentimentLabel == null || sentimentScore == null) {
                    log.warn("Sentiment analysis returned null data for post: {}", post.getId());
                    post.updateSentiment(0.0, "neutral");
                } else {
                    post.updateSentiment(sentimentScore, sentimentLabel.toLowerCase());
                }
                postRepository.save(post);
                log.info("Sentiment analysis completed for post: {} - Label: {}, Score: {}", 
                        post.getId(), post.getSentimentLabel(), post.getSentimentScore());
            }
        } catch (Exception e) {
            log.warn("Failed to analyze sentiment for post: {}, error: {}", post.getId(), e.getMessage());
        }
    }
    
    /**
     * 특정 카테고리의 경우 이미지 생성 비동기 수행
     */
    private void generateImageIfNeededAsync(Post post, Category category) {
        try {
            // 이미지 생성이 필요한 카테고리들 (예: 창작, 디자인, 프로젝트 등)
            if (shouldGenerateImage(category.getName())) {
                String imagePrompt = createImagePrompt(post.getTitle(), post.getContent());
                
                ImageGenerationDto.GenerateResponse response = 
                    aiServiceClient.generateImage(imagePrompt, "digital-art", "1024x1024", 1);
                
                if (response.isSuccess() && response.getData() != null && 
                    response.getData().getImageUrl() != null && !response.getData().getImageUrl().isEmpty()) {
                    
                    String imageUrl = response.getData().getImageUrl();
                    post.setImageUrl(imageUrl);
                    postRepository.save(post);
                    log.info("Image generated for post: {} - URL: {}", post.getId(), imageUrl);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to generate image for post: {}, error: {}", post.getId(), e.getMessage());
        }
    }
    
    /**
     * 사용자 지정 프롬프트로 이미지 생성 비동기 수행
     */
    private void generateImageWithCustomPromptAsync(Post post, String customPrompt) {
        try {
            ImageGenerationDto.GenerateResponse response = 
                aiServiceClient.generateImage(customPrompt, "digital-art", "1024x1024", 1);
            
            if (response.isSuccess() && response.getData() != null && 
                response.getData().getImageUrl() != null && !response.getData().getImageUrl().isEmpty()) {
                
                String imageUrl = response.getData().getImageUrl();
                post.setImageUrl(imageUrl);
                postRepository.save(post);
                log.info("Image generated with custom prompt for post: {} - URL: {}", post.getId(), imageUrl);
            }
        } catch (Exception e) {
            log.warn("Failed to generate image with custom prompt for post: {}, error: {}", post.getId(), e.getMessage());
        }
    }

    private PostDto.PageResponse convertToPageResponse(List<Post> posts, Pageable pageable) {
        PostDto.PageResponse pageResponse = new PostDto.PageResponse();
        pageResponse.setPosts(posts.stream()
                .map(PostDto.Response::new)
                .collect(Collectors.toList()));
        pageResponse.setPageNumber(pageable.getPageNumber());
        pageResponse.setPageSize(pageable.getPageSize());
        pageResponse.setTotalElements(posts.size());
        pageResponse.setTotalPages(1);
        pageResponse.setFirst(pageable.getPageNumber() == 0);
        pageResponse.setLast(true);
        
        return pageResponse;
    }
}