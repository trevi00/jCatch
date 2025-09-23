package org.jbd.backend.community.service;

import org.jbd.backend.community.dto.CategoryDto;

import java.util.List;

/**
 * 커뮤니티 카테고리 서비스 인터페이스
 *
 * 커뮤니티 게시판의 카테고리 관리 기능을 정의하는 인터페이스입니다.
 * 카테고리의 생성, 조회, 수정, 삭제 및 정렬 순서 관리 기능을 제공합니다.
 *
 * 주요 기능:
 * - 카테고리 생성 및 관리
 * - 카테고리 조회 및 목록 가져오기
 * - 카테고리 정보 수정
 * - 카테고리 삭제 (소프트 삭제)
 * - 카테고리 노출 순서 관리
 *
 * 구현 클래스에서는 비즈니스 로직과 데이터 유효성 검증을 담당합니다.
 */
public interface CategoryService {
    
    /**
     * 새로운 카테고리를 생성합니다.
     *
     * 요청된 정보로 새로운 카테고리를 생성하고 데이터베이스에 저장합니다.
     * 카테고리는 기본적으로 활성 상태로 생성되며 노출 순서가 자동 설정됩니다.
     *
     * @param request 카테고리 생성 요청 정보 (이름, 설명, 아이콘 등)
     * @return CategoryDto.Response 생성된 카테고리 응답 DTO
     * @throws BusinessException 카테고리 이름 중복 또는 유효성 검증 실패 시
     */
    CategoryDto.Response createCategory(CategoryDto.Request request);
    
    /**
     * ID로 카테고리를 조회합니다.
     *
     * 주어진 ID에 해당하는 카테고리를 조회하여 상세 정보를 반환합니다.
     * 삭제된 카테고리는 조회되지 않습니다.
     *
     * @param id 조회할 카테고리 ID
     * @return CategoryDto.Response 카테고리 상세 정보 DTO
     * @throws ResourceNotFoundException 카테고리를 찾을 수 없는 경우
     */
    CategoryDto.Response getCategoryById(Long id);
    
    /**
     * 모든 활성 카테고리를 조회합니다.
     *
     * 삭제되지 않은 모든 카테고리를 노출 순서대로 정렬하여 반환합니다.
     * 게시판 메뉴나 카테고리 선택 멶더드로서트에서 사용됩니다.
     *
     * @return List<CategoryDto.Response> 활성 카테고리 목록 (노출 순서대로 정렬)
     */
    List<CategoryDto.Response> getAllActiveCategories();
    
    /**
     * 카테고리 정보를 수정합니다.
     *
     * 기존 카테고리의 이름, 설명, 아이콘 등의 정보를 업데이트합니다.
     * 카테고리 이름 중복 및 유효성 검증을 수행합니다.
     *
     * @param id 수정할 카테고리 ID
     * @param request 카테고리 수정 요청 정보
     * @return CategoryDto.Response 수정된 카테고리 응답 DTO
     * @throws ResourceNotFoundException 카테고리를 찾을 수 없는 경우
     * @throws BusinessException 이름 중복 또는 유효성 검증 실패 시
     */
    CategoryDto.Response updateCategory(Long id, CategoryDto.Request request);
    
    /**
     * 카테고리를 삭제합니다.
     *
     * 카테고리를 소프트 삭제하여 비활성 상태로 만듭니다.
     * 해당 카테고리에 속한 게시글이 있는지 확인하고 이동 또는 삭제 처리를 수행합니다.
     *
     * @param id 삭제할 카테고리 ID
     * @throws ResourceNotFoundException 카테고리를 찾을 수 없는 경우
     * @throws BusinessException 카테고리에 삭제할 수 없는 게시글이 있는 경우
     */
    void deleteCategory(Long id);
    
    /**
     * 카테고리의 노출 순서를 변경합니다.
     *
     * 카테고리 목록에서의 노출 순서를 조정합니다.
     * 낙은 순서 번호일수록 먼저 노출되며, 같은 순서인 경우 ID 순으로 정렬됩니다.
     *
     * @param id 순서를 변경할 카테고리 ID
     * @param displayOrder 새로운 노출 순서 (양수)
     * @return CategoryDto.Response 순서가 변경된 카테고리 응답 DTO
     * @throws ResourceNotFoundException 카테고리를 찾을 수 없는 경우
     * @throws ValidationException 순서 값이 유효하지 않은 경우
     */
    CategoryDto.Response updateDisplayOrder(Long id, Integer displayOrder);
}