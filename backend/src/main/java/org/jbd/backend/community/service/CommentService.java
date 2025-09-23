package org.jbd.backend.community.service;

import org.jbd.backend.community.dto.CommentDto;

import java.util.List;

/**
 * 커뮤니티 댓글 서비스 인터페이스
 *
 * 커뮤니티 게시판의 댓글 및 대댓글 관리 기능을 정의하는 인터페이스입니다.
 * 댓글의 생성, 조회, 수정, 삭제 및 계층구조 관리 기능을 제공합니다.
 *
 * 주요 기능:
 * - 댓글 생성 및 관리
 * - 대댓글 (답리) 지원
 * - 게시글별 댓글 조회
 * - 작성자별 댓글 조회
 * - 댓글 수정 및 삭제 (권한 검증)
 * - 댓글 수 통계
 *
 * 보안 기능:
 * - 작성자 본인만 수정/삭제 가능
 * - 소프트 삭제로 데이터 보존
 * - 계층구조 지원 (부모-자식 관계)
 */
public interface CommentService {
    
    /**
     * 새로운 댓글을 생성합니다.
     *
     * 주어진 게시글에 댓글을 작성하거나 기존 댓글에 대댓글을 작성합니다.
     * 사용자 인증 및 게시글 존재 여부를 확인한 후 댓글을 등록합니다.
     *
     * @param request 댓글 생성 요청 정보 (내용, 게시글ID, 부모댓글ID 등)
     * @param authorEmail 댓글 작성자의 이메일 주소
     * @return CommentDto.Response 생성된 댓글 응답 DTO
     * @throws ResourceNotFoundException 게시글이나 부모댓글을 찾을 수 없는 경우
     * @throws BusinessException 사용자 권한이 없거나 유효성 검증 실패 시
     */
    CommentDto.Response createComment(CommentDto.CreateRequest request, String authorEmail);
    
    /**
     * ID로 댓글을 조회합니다.
     *
     * 주어진 ID에 해당하는 댓글의 상세 정보를 조회합니다.
     * 삭제된 댓글도 조회 가능하지만 내용은 마스킹 처리됩니다.
     *
     * @param id 조회할 댓글 ID
     * @return CommentDto.Response 댓글 상세 정보 DTO
     * @throws ResourceNotFoundException 댓글을 찾을 수 없는 경우
     */
    CommentDto.Response getCommentById(Long id);
    
    /**
     * 특정 게시글의 댓글 목록을 조회합니다.
     *
     * 주어진 게시글에 달린 모든 댓글을 계층구조로 조회합니다.
     * 댓글은 작성 시간 순으로 정렬되며 대댓글은 부모 댓글 하위에 비너됨니다.
     *
     * @param postId 댓글을 조회할 게시글 ID
     * @return List<CommentDto.Response> 해당 게시글의 댓글 목록
     * @throws ResourceNotFoundException 게시글을 찾을 수 없는 경우
     */
    List<CommentDto.Response> getCommentsByPost(Long postId);
    
    /**
     * 특정 작성자의 댓글 목록을 조회합니다.
     *
     * 주어진 이메일 주소의 사용자가 작성한 모든 댓글을 조회합니다.
     * 내 댓글 목록 보기나 사용자 활동 버리 확인 시 사용됩니다.
     *
     * @param authorEmail 댓글 작성자의 이메일 주소
     * @return List<CommentDto.Response> 해당 사용자의 댓글 목록
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    List<CommentDto.Response> getCommentsByAuthor(String authorEmail);
    
    /**
     * 댓글을 수정합니다.
     *
     * 기존 댓글의 내용을 수정합니다. 오직 댓글 작성자만 수정이 가능하며
     * 수정 시간이 기록됩니다. 삭제된 댓글은 수정할 수 없습니다.
     *
     * @param id 수정할 댓글 ID
     * @param request 댓글 수정 요청 정보 (새로운 내용)
     * @param authorEmail 수정 요청자의 이메일 주소
     * @return CommentDto.Response 수정된 댓글 응답 DTO
     * @throws ResourceNotFoundException 댓글을 찾을 수 없는 경우
     * @throws BusinessException 수정 권한이 없거나 삭제된 댓글인 경우
     */
    CommentDto.Response updateComment(Long id, CommentDto.UpdateRequest request, String authorEmail);
    
    /**
     * 댓글을 삭제합니다.
     *
     * 댓글을 소프트 삭제하여 비활성 상태로 만듭니다.
     * 오직 댓글 작성자만 삭제가 가능하며 대댓글이 있는 경우 내용만 마스킹 처리됩니다.
     *
     * @param id 삭제할 댓글 ID
     * @param authorEmail 삭제 요청자의 이메일 주소
     * @throws ResourceNotFoundException 댓글을 찾을 수 없는 경우
     * @throws BusinessException 삭제 권한이 없는 경우
     */
    void deleteComment(Long id, String authorEmail);
    
    /**
     * 특정 댓글의 대댓글 목록을 조회합니다.
     *
     * 주어진 부모 댓글에 달린 모든 대댓글을 작성 시간 순으로 조회합니다.
     * 계층구조를 지원하여 대댓글의 대댓글도 포함될 수 있습니다.
     *
     * @param parentCommentId 대댓글을 조회할 부모 댓글 ID
     * @return List<CommentDto.Response> 대댓글 목록
     * @throws ResourceNotFoundException 부모 댓글을 찾을 수 없는 경우
     */
    List<CommentDto.Response> getRepliesByParentComment(Long parentCommentId);
    
    /**
     * 특정 게시글의 댓글 수를 조회합니다.
     *
     * 주어진 게시글에 달린 댓글과 대댓글의 총 개수를 계산합니다.
     * 삭제된 댓글도 카운트에 포함되어 계층구조를 유지합니다.
     *
     * @param postId 댓글 수를 조회할 게시글 ID
     * @return long 해당 게시글의 댓글 총 개수
     * @throws ResourceNotFoundException 게시글을 찾을 수 없는 경우
     */
    long getCommentCountByPost(Long postId);
}