package org.jbd.backend.community.repository;

import org.jbd.backend.community.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 커뮤니티 카테고리 리포지토리
 *
 * 커뮤니티 게시판의 카테고리 엔티티에 대한 데이터 액세스 기능을 제공하는 리포지토리입니다.
 * JpaRepository를 확장하여 기본적인 CRUD 기능 외에도 카테고리 관리에 필요한
 * 커스텀 쿼리 메서드를 제공합니다.
 *
 * 주요 기능:
 * - 활성화된 카테고리 조회 및 정렬 순서 관리
 * - 카테고리명 기반 검색 및 중복 검증
 * - 논리적 삭제(Soft Delete) 처리된 카테고리 필터링
 * - 카테고리 표시 순서 관리를 통한 UI 레이아웃 제어
 *
 * 비즈니스 로직 지원:
 * - 커뮤니티 게시판 카테고리 분류 및 구성
 * - 카테고리별 게시글 필터링 지원
 * - 관리자 설정에 따른 카테고리 활성화/비활성화 관리
 * - 사용자 친화적인 카테고리 정렬 순서 제공
 *
 * 성능 최적화:
 * - 활성 카테고리만 조회하여 불필요한 데이터 로딩 방지
 * - 정렬 순서 기반 인덱스 활용으로 빠른 조회 성능 제공
 * - 카테고리명 기반 빠른 검색 지원
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 활성화된 모든 카테고리를 표시 순서대로 조회합니다.
     *
     * 커뮤니티 메인 페이지나 게시글 작성 시 카테고리 선택 목록을 보여줄 때 사용됩니다.
     * 관리자가 설정한 표시 순서에 따라 정렬되어 사용자에게 일관된 카테고리 구조를 제공합니다.
     * 비활성화된 카테고리는 제외하여 현재 사용 가능한 카테고리만 반환합니다.
     *
     * @return List<Category> 활성화된 카테고리 목록 (표시 순서대로 정렬)
     */
    List<Category> findByActiveTrueOrderByDisplayOrder();

    /**
     * 특정 이름의 활성화된 카테고리를 조회합니다.
     *
     * 카테고리명으로 특정 카테고리를 검색하거나 중복 카테고리 생성을 방지할 때 사용됩니다.
     * 관리자가 카테고리를 관리할 때나 API에서 카테고리명으로 필터링할 때 활용됩니다.
     * 활성화된 카테고리만 조회하여 현재 사용 중인 카테고리의 유효성을 보장합니다.
     *
     * @param name 조회할 카테고리명
     * @return Optional<Category> 해당 이름의 활성화된 카테고리 (없으면 empty)
     */
    Optional<Category> findByNameAndActiveTrue(String name);
}