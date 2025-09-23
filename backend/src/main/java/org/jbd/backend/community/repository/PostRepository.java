package org.jbd.backend.community.repository;

import org.jbd.backend.community.domain.Category;
import org.jbd.backend.community.domain.Post;
import org.jbd.backend.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 게시글 리포지토리
 *
 * 커뮤니티 게시판의 게시글 엔티티에 대한 데이터 액세스 리포지토리입니다.
 * JpaRepository를 확장하여 기본적인 CRUD 기능 외에도 게시글 관리에 필요한
 * 다양한 커스텀 쿼리 메서드를 제공합니다.
 *
 * 주요 기능:
 * - 삭제되지 않은 게시글 조회 및 페이징 처리
 * - 카테고리별 게시글 필터링 및 정렬
 * - 게시글 제목 기반 검색 (대소문자 무시)
 * - 작성자별 게시글 조회 및 관리
 * - 조회수 및 인기도 기반 정렬
 * - AI 감정 분석 결과 기반 게시글 분류
 * - 이미지가 포함된 게시글 전용 조회
 * - 성능 최적화된 페치 조인 쿼리
 *
 * 지원 기능:
 * - 논리적 삭제(Soft Delete) 처리로 데이터 보존
 * - 다양한 정렬 옵션 (최신순, 조회수순, 감정점수순)
 * - 카테고리별 게시글 수 통계
 * - 감정 분석 레이블 및 점수 범위 필터링
 * - 연관 엔티티 즉시 로딩으로 N+1 문제 해결
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 삭제되지 않은 게시글을 최신순으로 페이징 조회합니다.
     *
     * 커뮤니티 메인 페이지에서 전체 게시글 목록을 보여줄 때 사용되며,
     * 논리적 삭제 처리된 게시글은 제외하고 최신 작성순으로 정렬합니다.
     *
     * @param pageable 페이징 및 정렬 정보 (페이지 번호, 크기, 정렬 조건)
     * @return Page<Post> 삭제되지 않은 게시글의 페이징된 결과
     */
    Page<Post> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 특정 카테고리의 삭제되지 않은 게시글을 최신순으로 조회합니다.
     *
     * 카테고리별 게시글 필터링 기능에서 사용되며, 질문게시판, 자유게시판,
     * 기업게시판 등 카테고리별로 게시글을 분류하여 보여줍니다.
     *
     * @param category 조회할 게시글 카테고리
     * @param pageable 페이징 정보 (페이지 크기, 오프셋)
     * @return List<Post> 해당 카테고리의 게시글 목록 (최신순)
     */
    List<Post> findByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(Category category, Pageable pageable);

    /**
     * 제목에 특정 키워드가 포함된 삭제되지 않은 게시글을 검색합니다.
     *
     * 게시글 검색 기능에서 사용되며, 대소문자를 구분하지 않고 제목에서
     * 부분 일치하는 게시글들을 최신순으로 조회합니다.
     *
     * @param title 검색할 키워드 (제목에서 부분 일치 검색)
     * @param pageable 페이징 정보 (검색 결과 제한)
     * @return List<Post> 제목에 키워드가 포함된 게시글 목록
     */
    List<Post> findByTitleContainingIgnoreCaseAndIsDeletedFalseOrderByCreatedAtDesc(String title, Pageable pageable);

    /**
     * 특정 작성자의 삭제되지 않은 게시글을 최신순으로 조회합니다.
     *
     * 사용자 프로필 페이지에서 해당 사용자가 작성한 게시글 목록을 보여주거나,
     * 마이페이지에서 자신이 작성한 게시글을 확인할 때 사용됩니다.
     *
     * @param author 조회할 게시글 작성자
     * @param pageable 페이징 정보 (목록 제한)
     * @return List<Post> 해당 작성자의 게시글 목록 (최신순)
     */
    List<Post> findByAuthorAndIsDeletedFalseOrderByCreatedAtDesc(User author, Pageable pageable);

    /**
     * 특정 카테고리의 삭제되지 않은 게시글을 조회수 순으로 조회합니다.
     *
     * 인기 게시글이나 트렌딩 게시글을 보여주는 기능에서 사용되며,
     * 조회수가 높은 순서대로 정렬하여 관심도가 높은 게시글을 우선 표시합니다.
     *
     * @param category 조회할 게시글 카테고리
     * @param pageable 페이징 정보 (인기 게시글 제한)
     * @return List<Post> 해당 카테고리의 게시글 목록 (조회수 높은 순)
     */
    List<Post> findByCategoryAndIsDeletedFalseOrderByViewCountDesc(Category category, Pageable pageable);

    /**
     * 게시글 ID로 삭제되지 않은 게시글과 연관 엔티티를 함께 조회합니다.
     *
     * 게시글 상세 보기 페이지에서 사용되며, 페치 조인을 통해 카테고리와 작성자 정보를
     * 한 번의 쿼리로 함께 조회하여 N+1 문제를 방지합니다.
     *
     * @param id 조회할 게시글 ID
     * @return Optional<Post> 게시글과 연관 엔티티 (카테고리, 작성자)가 포함된 결과
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.category " +
           "JOIN FETCH p.author " +
           "WHERE p.id = :id AND p.isDeleted = false")
    Optional<Post> findByIdWithCategoryAndAuthor(@Param("id") Long id);

    /**
     * 게시글 ID로 모든 연관 엔티티를 포함하여 조회합니다.
     *
     * 관리자 기능이나 게시글 복구 시 삭제된 게시글도 포함하여 조회해야 할 때 사용됩니다.
     * 삭제 여부와 관계없이 게시글의 전체 데이터를 페치 조인으로 효율적으로 조회합니다.
     *
     * @param id 조회할 게시글 ID
     * @return Optional<Post> 게시글과 모든 연관 엔티티 (삭제된 게시글 포함)
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.category " +
           "JOIN FETCH p.author " +
           "WHERE p.id = :id")
    Optional<Post> findByIdWithFullData(@Param("id") Long id);

    /**
     * 특정 감정 분석 라벨의 삭제되지 않은 게시글을 최신순으로 조회합니다.
     *
     * AI 감정 분석 결과를 기반으로 긍정, 부정, 중립 등의 감정 상태별로
     * 게시글을 분류하여 조회할 때 사용됩니다. 커뮤니티 분위기 파악에 활용됩니다.
     *
     * @param sentimentLabel 감정 분석 라벨 (positive, negative, neutral 등)
     * @param pageable 페이징 정보 (결과 제한)
     * @return List<Post> 해당 감정 라벨의 게시글 목록 (최신순)
     */
    List<Post> findBySentimentLabelAndIsDeletedFalseOrderByCreatedAtDesc(String sentimentLabel, Pageable pageable);

    /**
     * 특정 카테고리와 감정 라벨의 삭제되지 않은 게시글을 최신순으로 조회합니다.
     *
     * 카테고리별로 감정 분석 결과를 세분화하여 분석할 때 사용됩니다.
     * 예를 들어 자유게시판의 긍정적인 게시글만 조회하는 등의 복합 필터링이 가능합니다.
     *
     * @param category 조회할 게시글 카테고리
     * @param sentimentLabel 감정 분석 라벨
     * @param pageable 페이징 정보
     * @return List<Post> 카테고리와 감정 라벨을 모두 만족하는 게시글 목록
     */
    List<Post> findByCategoryAndSentimentLabelAndIsDeletedFalseOrderByCreatedAtDesc(
            Category category, String sentimentLabel, Pageable pageable);

    /**
     * 감정 점수 범위에 해당하는 삭제되지 않은 게시글을 최신순으로 조회합니다.
     *
     * AI 감정 분석의 신뢰도 점수를 기반으로 특정 범위의 감정 강도를 가진
     * 게시글들을 조회할 때 사용됩니다. 더 세밀한 감정 분석이 필요한 경우에 활용됩니다.
     *
     * @param minScore 최소 감정 점수 (예: -1.0)
     * @param maxScore 최대 감정 점수 (예: 1.0)
     * @param pageable 페이징 정보
     * @return List<Post> 지정된 감정 점수 범위의 게시글 목록
     */
    List<Post> findBySentimentScoreBetweenAndIsDeletedFalseOrderByCreatedAtDesc(
            Double minScore, Double maxScore, Pageable pageable);

    /**
     * 이미지가 포함된 삭제되지 않은 게시글을 최신순으로 조회합니다.
     *
     * 갤러리 형태의 게시글 보기나 이미지가 포함된 게시글만 따로 보여주는
     * 기능에서 사용됩니다. 시각적 컨텐츠가 풍부한 게시글을 우선적으로 표시합니다.
     *
     * @param pageable 페이징 정보 (이미지 게시글 제한)
     * @return List<Post> 이미지가 포함된 게시글 목록 (최신순)
     */
    @Query("SELECT p FROM Post p WHERE p.imageUrl IS NOT NULL AND p.isDeleted = false ORDER BY p.createdAt DESC")
    List<Post> findPostsWithImagesOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 특정 카테고리의 삭제되지 않은 게시글 수를 카운트합니다.
     *
     * 카테고리별 게시글 통계나 대시보드에서 카테고리별 활성도를 표시할 때 사용됩니다.
     * 각 카테고리의 게시글 수를 통해 커뮤니티의 관심사와 활동량을 파악할 수 있습니다.
     *
     * @param category 카운트할 게시글 카테고리
     * @return Long 해당 카테고리의 활성 게시글 수
     */
    Long countByCategoryAndIsDeletedFalse(Category category);
}