package org.jbd.backend.community.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jbd.backend.auth.service.JwtService;
import org.jbd.backend.auth.util.AuthenticationUtils;
import org.jbd.backend.common.dto.ApiResponse;
import org.jbd.backend.community.dto.CommentDto;
import org.jbd.backend.community.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;
    private final JwtService jwtService;
    private final AuthenticationUtils authenticationUtils;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentDto.Response>> createComment(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CommentDto.CreateRequest request) {

        String authorEmail = authenticationUtils.extractEmailFromHeader(authHeader);
        
        CommentDto.Response response = commentService.createComment(request, authorEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("댓글이 작성되었습니다.", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentDto.Response>> getCommentById(@PathVariable Long id) {
        CommentDto.Response response = commentService.getCommentById(id);
        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", response));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<List<CommentDto.Response>>> getCommentsByPost(@PathVariable Long postId) {
        List<CommentDto.Response> comments = commentService.getCommentsByPost(postId);
        return ResponseEntity.ok(ApiResponse.success("게시글별 댓글 조회 성공", comments));
    }

    @GetMapping("/author")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CommentDto.Response>>> getCommentsByAuthor(
            @RequestHeader("Authorization") String authHeader) {

        String authorEmail = authenticationUtils.extractEmailFromHeader(authHeader);
        
        List<CommentDto.Response> comments = commentService.getCommentsByAuthor(authorEmail);
        return ResponseEntity.ok(ApiResponse.success("내 댓글 목록 조회 성공", comments));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentDto.Response>> updateComment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody CommentDto.UpdateRequest request) {

        String authorEmail = authenticationUtils.extractEmailFromHeader(authHeader);
        
        CommentDto.Response response = commentService.updateComment(id, request, authorEmail);
        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다.", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        String authorEmail = authenticationUtils.extractEmailFromHeader(authHeader);
        
        commentService.deleteComment(id, authorEmail);
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다."));
    }

    @GetMapping("/parent/{parentCommentId}/replies")
    public ResponseEntity<ApiResponse<List<CommentDto.Response>>> getRepliesByParentComment(
            @PathVariable Long parentCommentId) {
        List<CommentDto.Response> replies = commentService.getRepliesByParentComment(parentCommentId);
        return ResponseEntity.ok(ApiResponse.success("답글 목록 조회 성공", replies));
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<ApiResponse<Long>> getCommentCountByPost(@PathVariable Long postId) {
        long count = commentService.getCommentCountByPost(postId);
        return ResponseEntity.ok(ApiResponse.success("댓글 개수 조회 성공", count));
    }
}