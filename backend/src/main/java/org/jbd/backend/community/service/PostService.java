package org.jbd.backend.community.service;

import org.jbd.backend.community.dto.PostDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 커뮤니티 게시글 서비스 인터페이스
 *
 * 커뮤니티 게시판의 게시글 관리 기능을 정의하는 인터페이스입니다.
 * 게시글의 CRUD 작업, 검색, 통계, AI 기반 감정 분석 기능을 제공합니다.
 *
 * 주요 기능:
 * - 게시글 CRUD 작업 (생성, 조회, 수정, 삭제)
 * - 카테고리별, 작성자별 게시글 조회
 * - 제목 기반 게시글 검색
 * - 조회수 관리 및 인기 게시글 조회
 * - AI 기반 감정 분석 및 감정별 게시글 필터링
 * - 이미지 체버실 게시글 조회
 * - 페이징 지원
 *
 * 보안 기능:
 * - 작성자 본인만 수정/삭제 가능
 * - 소프트 삭제로 데이터 보존
 * - 게시글 상태 관리 (공개/비공개/삭제)
 */
public interface PostService {
    
    /**
     * 모든 게시글을 페이징하여 조회합니다.
     *
     * 삭제되지 않은 모든 게시글을 최신순으로 정렬하여 페이징 처리하여 반환합니다.
     * 메인 게시판 목록에서 사용되며 카테고리 구분 없이 전체 게시글을 보여줍니다.
     *
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return PostDto.PageResponse 페이징된 게시글 목록
     */
    PostDto.PageResponse getAllPosts(Pageable pageable);
    
    /**
     * 새로운 게시글을 생성합니다.
     *
     * 사용자가 작성한 게시글을 데이터베이스에 저장하고 AI 감정 분석을 수행합니다.
     * 카테고리 설정, 이미지 업로드, 감정 분석 결과 저장 등의 처리가 포함됩니다.
     *
     * @param request 게시글 생성 요청 정보 (제목, 내용, 카테고리, 이미지 등)
     * @param authorEmail 게시글 작성자의 이메일 주소
     * @return PostDto.Response 생성된 게시글 응답 DTO
     * @throws ResourceNotFoundException 카테고리나 사용자를 찾을 수 없는 경우
     * @throws BusinessException 유효성 검증 실패 또는 기타 비즈니스 규칙 위반 시
     */
    PostDto.Response createPost(PostDto.CreateRequest request, String authorEmail);
    
    /**
     * ID로 게시글을 조회합니다.
     *
     * 주어진 ID에 해당하는 게시글의 상세 정보를 조회합니다.
     * 조회 시 자동으로 조회수가 1 증가하며, 삭제된 게시글은 조회할 수 없습니다.
     *
     * @param id 조회할 게시글 ID
     * @return PostDto.Response 게시글 상세 정보 DTO
     * @throws ResourceNotFoundException 게시글을 찾을 수 없음 경우
     */
    PostDto.Response getPostById(Long id);
    
    /**
     * 특정 카테고리의 게시글을 조회합니다.
     *
     * 주어진 카테고리에 속한 모든 게시글을 최신순으로 정렬하여 페이징 처리하여 반환합니다.
     * 카테고리별 게시판에서 사용되며 해당 카테고리의 게시글만 필터링합니다.
     *
     * @param categoryId 조회할 카테고리 ID
     * @param pageable 페이징 정보
     * @return PostDto.PageResponse 해당 카테고리의 게시글 목록
     * @throws ResourceNotFoundException 카테고리를 찾을 수 없는 경우
     */
    PostDto.PageResponse getPostsByCategory(Long categoryId, Pageable pageable);
    
    /**
     * 제목으로 게시글을 검색합니다.
     *
     * 게시글 제목에 주어진 키워드가 포함된 게시글을 검색합니다.
     * 부분 일치 검색을 지원하며 대소문자를 구분하지 않습니다.
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return PostDto.PageResponse 검색된 게시글 목록
     */
    PostDto.PageResponse searchPostsByTitle(String keyword, Pageable pageable);
    
    /**
     * 특정 작성자의 게시글을 조회합니다.
     *
     * 주어진 이메일 주소의 사용자가 작성한 모든 게시글을 조회합니다.
     * 내 게시글 목록 보기나 사용자 활동 내역 확인 시 사용됩니다.
     *
     * @param authorEmail 게시글 작성자의 이메일 주소
     * @param pageable 페이징 정보
     * @return PostDto.PageResponse 해당 사용자의 게시글 목록
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    PostDto.PageResponse getPostsByAuthor(String authorEmail, Pageable pageable);
    
    /**
     * 게시글을 수정합니다.
     *
     * 기존 게시글의 제목, 내용, 카테고리 등을 수정합니다.
     * 오직 게시글 작성자만 수정이 가능하며 수정 시간이 기록됩니다.
     * 내용 변경 시 AI 감정 분석이 다시 수행됩니다.
     *
     * @param id 수정할 게시글 ID
     * @param request 게시글 수정 요청 정보
     * @param authorEmail 수정 요청자의 이메일 주소
     * @return PostDto.Response 수정된 게시글 응답 DTO
     * @throws ResourceNotFoundException 게시글을 찾을 수 없는 경우
     * @throws BusinessException 수정 권한이 없거나 삭제된 게시글인 경우
     */
    PostDto.Response updatePost(Long id, PostDto.UpdateRequest request, String authorEmail);
    
    /**
     * 게시글을 삭제합니다.
     *
     * 게시글을 소프트 삭제하여 비활성 상태로 만듭니다.
     * 오직 게시글 작성자만 삭제가 가능하며 관련된 댓글들도 함께 비활성화됩니다.
     *
     * @param id 삭제할 게시글 ID
     * @param authorEmail 삭제 요청자의 이메일 주소
     * @throws ResourceNotFoundException 게시글을 찾을 수 없는 경우
     * @throws BusinessException 삭제 권한이 없는 경우
     */
    void deletePost(Long id, String authorEmail);
    
    /**
     * 게시글의 조회수를 증가시킵니다.
     *
     * 주어진 게시글의 조회수를 1 증가시키고 업데이트된 게시글 정보를 반환합니다.
     * 게시글 상세 조회 시 자동으로 호출되어 인기도를 측정하는 데 사용됩니다.
     *
     * @param id 조회수를 증가시킬 게시글 ID
     * @return PostDto.Response 조회수가 업데이트된 게시글 응답 DTO
     * @throws ResourceNotFoundException 게시글을 찾을 수 없는 경우
     */
    PostDto.Response incrementViewCount(Long id);
    
    /**
     * 인기 게시글을 조회합니다.
     *
     * 조회수가 높은 인기 게시글을 조회합니다. 카테고리를 지정하면 해당 카테고리 내에서만 조회합니다.
     * 메인 페이지나 카테고리 페이지의 인기 게시글 섹션에서 사용됩니다.
     *
     * @param categoryId 인기 게시글을 조회할 카테고리 ID (null이면 전체)
     * @param limit 조회할 게시글 개수
     * @return List<PostDto.Response> 인기 게시글 목록 (조회수 내림차순)
     */
    List<PostDto.Response> getPopularPosts(Long categoryId, int limit);
    
    /**
     * 감정 라벨로 게시글을 조회합니다.
     *
     * AI 감정 분석 결과에 따라 특정 감정에 해당하는 게시글을 조회합니다.
     * 긍정적, 부정적, 중립적 감정으로 게시글을 필터링하여 감정 기반 컨텐츠 분석에 활용됩니다.
     *
     * @param sentimentLabel 감정 라벨 (POSITIVE, NEGATIVE, NEUTRAL)
     * @param pageable 페이징 정보
     * @return PostDto.PageResponse 해당 감정의 게시글 목록
     */
    PostDto.PageResponse getPostsBySentiment(String sentimentLabel, Pageable pageable);
    
    /**
     * 카테고리와 감정으로 게시글을 조회합니다.
     *
     * 특정 카테고리 내에서 특정 감정에 해당하는 게시글을 조회합니다.
     * 카테고리와 감정을 모두 적용한 고도화된 필터링 기능을 제공합니다.
     *
     * @param categoryId 카테고리 ID
     * @param sentimentLabel 감정 라벨
     * @param pageable 페이징 정보
     * @return PostDto.PageResponse 해당 카테고리와 감정의 게시글 목록
     * @throws ResourceNotFoundException 카테고리를 찾을 수 없는 경우
     */
    PostDto.PageResponse getPostsByCategoryAndSentiment(Long categoryId, String sentimentLabel, Pageable pageable);
    
    /**
     * 감정 점수 범위로 게시글을 조회합니다.
     *
     * AI 감정 분석에서 나온 점수를 기준으로 게시글을 필터링합니다.
     * 최소와 최대 점수 범위를 설정하여 세밀한 감정 조절이 가능합니다.
     *
     * @param minScore 최소 감정 점수 (-1.0 ~ 1.0)
     * @param maxScore 최대 감정 점수 (-1.0 ~ 1.0)
     * @param pageable 페이징 정보
     * @return PostDto.PageResponse 해당 감정 점수 범위의 게시글 목록
     * @throws ValidationException 점수 범위가 잘못된 경우
     */
    PostDto.PageResponse getPostsBySentimentScore(Double minScore, Double maxScore, Pageable pageable);
    
    /**
     * 이미지가 있는 게시글을 조회합니다.
     *
     * 이미지 URL이 설정된 게시글만 필터링하여 조회합니다.
     * 시각적 컨텐츠를 선호하는 사용자나 이미지 갤러리 형태의 페이지에서 사용됩니다.
     *
     * @param pageable 페이징 정보
     * @return PostDto.PageResponse 이미지가 있는 게시글 목록
     */
    PostDto.PageResponse getPostsWithImages(Pageable pageable);
}