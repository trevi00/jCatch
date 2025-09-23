package org.jbd.backend.community.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jbd.backend.auth.service.JwtService;
import org.jbd.backend.auth.util.AuthenticationUtils;
import org.jbd.backend.common.dto.ApiResponse;
import org.jbd.backend.common.service.PermissionService;
import org.jbd.backend.community.dto.PostDto;
import org.jbd.backend.community.service.PostService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 커뮤니티 게시글 관리 REST API 컨트롤러
 *
 * 커뮤니티 게시글의 생성, 조회, 수정, 삭제, 검색 등의 기능을 제공합니다.
 * 카테고리별 조회, 감정 분석 기반 조회, 이미지 포함 게시글 조회 등
 * 다양한 필터링 기능을 지원합니다.
 *
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 * @see PostService
 * @see PostDto
 */
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PostController {

    /** 게시글 비즈니스 로직을 처리하는 서비스 */
    private final PostService postService;

    /** JWT 토큰 관리 서비스 */
    private final JwtService jwtService;

    /** 사용자 권한 검증 서비스 */
    private final PermissionService permissionService;

    /** 인증 관련 유틸리티 */
    private final AuthenticationUtils authenticationUtils;

    /**
     * 모든 게시글 목록을 조회합니다.
     * 페이지네이션을 지원하며, 최신 작성순으로 정렬됩니다.
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return ResponseEntity<ApiResponse<PostDto.PageResponse>> 게시글 목록 (페이지네이션 정보 포함)
     * @apiNote GET /posts
     * @see PostDto.PageResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PostDto.PageResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        PostDto.PageResponse posts = postService.getAllPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success("게시글 목록 조회 성공", posts));
    }

    /**
     * 새로운 게시글을 작성합니다.
     * 로그인한 사용자만 게시글을 작성할 수 있습니다.
     *
     * @param authHeader Authorization 헤더 ("Bearer {token}" 형식)
     * @param request 게시글 생성 요청 데이터
     *                - title: 게시글 제목 (필수)
     *                - content: 게시글 내용 (필수)
     *                - categoryId: 카테고리 ID (필수)
     *                - images: 체부 이미지 URL 목록 (선택)
     * @return ResponseEntity<ApiResponse<PostDto.Response>> 생성된 게시글 정보
     * @apiNote POST /posts
     * @see PostDto.CreateRequest
     * @see PostDto.Response
     */
    @PostMapping
    @PreAuthorize("isAuthenticated() and @permissionService.canCreatePost(authentication, #request.categoryId)")
    public ResponseEntity<ApiResponse<PostDto.Response>> createPost(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody PostDto.CreateRequest request) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);
        String authorEmail = authenticationUtils.extractEmailFromHeader(authHeader);

        PostDto.Response response = postService.createPost(request, authorEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 생성되었습니다.", response));
    }

    /**
     * 특정 게시글의 상세 정보를 조회합니다.
     * 게시글 조회 시 조회수가 자동으로 증가합니다.
     *
     * @param id 게시글 ID
     * @return ResponseEntity<ApiResponse<PostDto.Response>> 게시글 상세 정보
     * @apiNote GET /posts/{id}
     * @see PostDto.Response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto.Response>> getPostById(@PathVariable Long id) {
        PostDto.Response response = postService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success("게시글 조회 성공", response));
    }

    /**
     * 특정 카테고리의 게시글 목록을 조회합니다.
     * 카테고리에 속한 게시글들만 필터링하여 조회합니다.
     * 기업게시판(카테고리 5)은 일반 사용자는 접근할 수 없습니다.
     *
     * @param categoryId 조회할 카테고리 ID
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return ResponseEntity<ApiResponse<PostDto.PageResponse>> 카테고리별 게시글 목록
     * @apiNote GET /posts/category/{categoryId}
     * @see PostDto.PageResponse
     */
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("@permissionService.canViewPostsByCategory(authentication, #categoryId)")
    public ResponseEntity<ApiResponse<PostDto.PageResponse>> getPostsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PostDto.PageResponse posts = postService.getPostsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success("카테고리별 게시글 조회 성공", posts));
    }

    /**
     * 제목으로 게시글을 검색합니다.
     * 게시글 제목에서 지정된 키워드를 포함하는 게시글들을 검색합니다.
     *
     * @param keyword 검색할 키워드 (필수)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return ResponseEntity<ApiResponse<PostDto.PageResponse>> 검색된 게시글 목록
     * @apiNote GET /posts/search
     * @see PostDto.PageResponse
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PostDto.PageResponse>> searchPostsByTitle(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        PostDto.PageResponse posts = postService.searchPostsByTitle(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("게시글 검색 성공", posts));
    }

    /**
     * 게시글을 수정합니다.
     * 게시글 작성자만 수정할 수 있습니다.
     *
     * @param authHeader Authorization 헤더 ("Bearer {token}" 형식)
     * @param id 수정할 게시글 ID
     * @param request 게시글 수정 요청 데이터
     *                - title: 수정할 제목 (선택)
     *                - content: 수정할 내용 (선택)
     *                - categoryId: 수정할 카테고리 ID (선택)
     *                - images: 수정할 이미지 URL 목록 (선택)
     * @return ResponseEntity<ApiResponse<PostDto.Response>> 수정된 게시글 정보
     * @apiNote PUT /posts/{id}
     * @see PostDto.UpdateRequest
     * @see PostDto.Response
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @permissionService.canUpdatePost(authentication, #id)")
    public ResponseEntity<ApiResponse<PostDto.Response>> updatePost(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody PostDto.UpdateRequest request) {

        Long userId = authenticationUtils.extractUserIdFromHeader(authHeader);
        String authorEmail = authenticationUtils.extractEmailFromHeader(authHeader);

        PostDto.Response response = postService.updatePost(id, request, authorEmail);
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다.", response));
    }

    /**
     * 게시글을 삭제합니다.
     * 게시글 작성자만 삭제할 수 있습니다. 삭제된 게시글은 복구할 수 없습니다.
     *
     * @param authHeader Authorization 헤더 ("Bearer {token}" 형식)
     * @param id 삭제할 게시글 ID
     * @return ResponseEntity<ApiResponse<Void>> 삭제 완료 응답
     * @apiNote DELETE /posts/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @permissionService.canDeletePost(authentication, #id)")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        String authorEmail = authenticationUtils.extractEmailFromHeader(authHeader);

        postService.deletePost(id, authorEmail);
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다."));
    }

    /**
     * 게시글의 조회수를 증가시킵니다.
     * 게시글을 읽을 때마다 자동으로 호출됩니다.
     *
     * @param id 조회수를 증가시킬 게시글 ID
     * @return ResponseEntity<ApiResponse<PostDto.Response>> 조회수가 증가된 게시글 정보
     * @apiNote PATCH /posts/{id}/view
     * @see PostDto.Response
     */
    @PatchMapping("/{id}/view")
    public ResponseEntity<ApiResponse<PostDto.Response>> incrementViewCount(@PathVariable Long id) {
        PostDto.Response response = postService.incrementViewCount(id);
        return ResponseEntity.ok(ApiResponse.success("조회수가 증가되었습니다.", response));
    }

    /**
     * 감정 라벨별로 게시글을 조회합니다.
     * AI 감정 분석 결과에 기반하여 긍정적, 부정적, 중립적 게시글들을 필터링합니다.
     *
     * @param sentimentLabel 감정 라벨 (POSITIVE, NEGATIVE, NEUTRAL 등)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return ResponseEntity<ApiResponse<PostDto.PageResponse>> 감정별 게시글 목록
     * @apiNote GET /posts/sentiment/{sentimentLabel}
     * @see PostDto.PageResponse
     */
    @GetMapping("/sentiment/{sentimentLabel}")
    public ResponseEntity<ApiResponse<PostDto.PageResponse>> getPostsBySentiment(
            @PathVariable String sentimentLabel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        PostDto.PageResponse posts = postService.getPostsBySentiment(sentimentLabel, pageable);
        return ResponseEntity.ok(ApiResponse.success("감정별 게시글 조회 성공", posts));
    }

    /**
     * 카테고리와 감정 라벨로 게시글을 조회합니다.
     * 특정 카테고리 내에서 지정된 감정을 가진 게시글들만 필터링합니다.
     * 기업게시판(카테고리 5)은 일반 사용자는 접근할 수 없습니다.
     *
     * @param categoryId 카테고리 ID
     * @param sentimentLabel 감정 라벨 (POSITIVE, NEGATIVE, NEUTRAL 등)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return ResponseEntity<ApiResponse<PostDto.PageResponse>> 카테고리별 감정 게시글 목록
     * @apiNote GET /posts/category/{categoryId}/sentiment/{sentimentLabel}
     * @see PostDto.PageResponse
     */
    @GetMapping("/category/{categoryId}/sentiment/{sentimentLabel}")
    @PreAuthorize("@permissionService.canViewPostsByCategory(authentication, #categoryId)")
    public ResponseEntity<ApiResponse<PostDto.PageResponse>> getPostsByCategoryAndSentiment(
            @PathVariable Long categoryId,
            @PathVariable String sentimentLabel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PostDto.PageResponse posts = postService.getPostsByCategoryAndSentiment(categoryId, sentimentLabel, pageable);
        return ResponseEntity.ok(ApiResponse.success("카테고리별 감정 게시글 조회 성공", posts));
    }

    /**
     * 감정 점수 범위로 게시글을 조회합니다.
     * AI 감정 분석에서 산출된 점수를 기반으로 지정된 범위 내의 게시글들을 조회합니다.
     *
     * @param minScore 최소 감정 점수 (필수)
     * @param maxScore 최대 감정 점수 (필수)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return ResponseEntity<ApiResponse<PostDto.PageResponse>> 감정 점수별 게시글 목록
     * @apiNote GET /posts/sentiment-score
     * @see PostDto.PageResponse
     */
    @GetMapping("/sentiment-score")
    public ResponseEntity<ApiResponse<PostDto.PageResponse>> getPostsBySentimentScore(
            @RequestParam Double minScore,
            @RequestParam Double maxScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        PostDto.PageResponse posts = postService.getPostsBySentimentScore(minScore, maxScore, pageable);
        return ResponseEntity.ok(ApiResponse.success("감정점수별 게시글 조회 성공", posts));
    }

    /**
     * 이미지가 포함된 게시글들을 조회합니다.
     * 체부 이미지가 포함된 게시글들만 필터링하여 조회합니다.
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return ResponseEntity<ApiResponse<PostDto.PageResponse>> 이미지 포함 게시글 목록
     * @apiNote GET /posts/with-images
     * @see PostDto.PageResponse
     */
    @GetMapping("/with-images")
    public ResponseEntity<ApiResponse<PostDto.PageResponse>> getPostsWithImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        PostDto.PageResponse posts = postService.getPostsWithImages(pageable);
        return ResponseEntity.ok(ApiResponse.success("이미지가 포함된 게시글 조회 성공", posts));
    }
}