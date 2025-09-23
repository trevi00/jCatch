package org.jbd.backend.community.repository;

import org.jbd.backend.community.domain.Comment;
import org.jbd.backend.community.domain.Post;
import org.jbd.backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 커뮤니티 댓글 리포지토리
 *
 * 커뮤니티 게시판의 댓글 엔티티에 대한 데이터 액세스 기능을 제공하는 리포지토리입니다.
 * JpaRepository를 확장하여 기본적인 CRUD 기능 외에도 댓글 관리에 필요한
 * 다양한 커스텀 쿼리 메서드를 제공합니다.
 *
 * 주요 기능:
 * - 게시글별 댓글 조회 및 시간순 정렬
 * - 작성자별 댓글 이력 추적 및 관리
 * - 계층형 댓글 구조 지원 (대댓글 및 댓글 답글)
 * - 댓글 수 통계 및 대댓글 수 카운트
 * - 성능 최적화된 정렬 및 필터링
 *
 * 계층형 구조 지원:
 * - 부모 댓글과 자식 댓글 간의 관계 관리
 * - 스레드형 댓글 구조를 통한 상세한 대화 지원
 * - 댓글 및 대댓글 계층 구조 체계적 관리
 * - 중첩된 댓글 인터성션 지원
 *
 * 성능 최적화:
 * - 시간순 정렬을 통한 효율적인 댓글 표시
 * - 게시글별 댓글 그룹화로 빠른 데이터 조회
 * - 인덱스 최적화를 통한 빠른 댓글 검색
 * - 댓글 수 카운트 캐시를 통한 대시보드 성능 향상
 *
 * 비즈니스 로직 지원:
 * - 커뮤니티 상호작용 촉진을 위한 댓글 시스템
 * - 사용자 참여도 측정을 위한 댓글 활동 추적
 * - 커뮤니티 활성화를 위한 댓글 및 대화 비대화 및 대화 진행
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 게시글의 모든 댓글을 작성 시간순으로 조회합니다.
     *
     * 게시글 상세 보기 페이지에서 해당 게시글에 대한 모든 댓글을 시간순으로 표시할 때 사용됩니다.
     * 댓글과 대댓글을 모두 포함하여 전체 댓글 스레드를 표시하는 데 활용됩니다.
     * 오래된 댓글부터 최신 댓글 순으로 정렬하여 댓글 흐름을 논리적으로 제시합니다.
     *
     * @param post 댓글을 조회할 게시글
     * @return List<Comment> 해당 게시글의 댓글 목록 (시간 만무립버)
     */
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);

    /**
     * 특정 작성자의 모든 댓글을 최신순으로 조회합니다.
     *
     * 사용자 프로필 페이지나 마이페이지에서 해당 사용자가 작성한 댓글 내역을 확인할 때 사용됩니다.
     * 사용자의 커뮤니티 활동 이력을 추적하고 참여도를 측정하는 데 활용됩니다.
     * 최신 댓글부터 오래된 댓글 순으로 정렬하여 최근 활동을 우선 표시합니다.
     *
     * @param author 댓글을 조회할 작성자
     * @return List<Comment> 해당 사용자의 댓글 목록 (최신순)
     */
    List<Comment> findByAuthorOrderByCreatedAtDesc(User author);

    /**
     * 특정 게시글의 최상위 댓글만 작성 시간순으로 조회합니다.
     *
     * 계층형 댓글 구조에서 부모 댓글만 별도로 분리하여 조회할 때 사용됩니다.
     * 대댓글은 제외하고 원본 댓글만 조회하여 댓글 스레드의 주요 흐름을 파악할 수 있습니다.
     * 페이지네이션이나 단계별 댓글 로딩에 활용됩니다.
     *
     * @param post 댓글을 조회할 게시글
     * @return List<Comment> 해당 게시글의 최상위 댓글 목록 (시간순)
     */
    List<Comment> findByPostAndParentCommentIsNullOrderByCreatedAtAsc(Post post);

    /**
     * 특정 부모 댓글에 대한 모든 대댓글을 작성 시간순으로 조회합니다.
     *
     * 계층형 댓글 구조에서 특정 댓글에 대한 답글들을 조회할 때 사용됩니다.
     * 댓글 상세 보기나 대댓글 확장 표시 기능에서 활용됩니다.
     * 댓글 내 대화의 시간적 흐름을 유지하기 위해 시간순으로 정렬합니다.
     *
     * @param parentComment 대댓글을 조회할 부모 댓글
     * @return List<Comment> 해당 부모 댓글의 대댓글 목록 (시간순)
     */
    List<Comment> findByParentCommentOrderByCreatedAtAsc(Comment parentComment);

    /**
     * 특정 게시글의 총 댓글 수를 카운트합니다.
     *
     * 게시글 목록이나 상세 보기에서 해당 게시글에 달린 전체 댓글 수를 표시할 때 사용됩니다.
     * 댓글과 대댓글을 모두 포함하여 전체 댓글 활동량을 측정합니다.
     * 게시글의 인기도나 참여도를 측정하는 지표로 활용됩니다.
     *
     * @param post 댓글 수를 카운트할 게시글
     * @return long 해당 게시글의 총 댓글 수
     */
    long countByPost(Post post);

    /**
     * 특정 부모 댓글에 대한 대댓글 수를 카운트합니다.
     *
     * 계층형 댓글 구조에서 특정 댓글에 달린 답글 수를 파악할 때 사용됩니다.
     * 댓글별 대화 활성도를 측정하거나 대댓글 표시 여부를 결정할 때 활용됩니다.
     * UI에서 "답글 N개" 같은 형태로 대댓글 수를 표시하는 데 사용됩니다.
     *
     * @param parentComment 대댓글 수를 카운트할 부모 댓글
     * @return long 해당 부모 댓글의 대댓글 수
     */
    long countByParentComment(Comment parentComment);
}