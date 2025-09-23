package org.jbd.backend.community.service.impl;

import org.jbd.backend.common.exception.ResourceNotFoundException;
import org.jbd.backend.community.domain.Comment;
import org.jbd.backend.community.domain.Post;
import org.jbd.backend.community.dto.CommentDto;
import org.jbd.backend.community.repository.CommentRepository;
import org.jbd.backend.community.repository.PostRepository;
import org.jbd.backend.community.service.CommentService;
import org.jbd.backend.user.domain.User;
import org.jbd.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentServiceImpl(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    public CommentDto.Response createComment(CommentDto.CreateRequest request, String authorEmail) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));
        
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", request.getPostId()));

        Comment comment;
        
        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentCommentId()));
            comment = new Comment(request.getContent(), author, post, parentComment);
        } else {
            comment = new Comment(request.getContent(), author, post);
        }

        Comment savedComment = commentRepository.save(comment);
        return new CommentDto.Response(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto.Response getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
        return new CommentDto.Response(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto.Response> getCommentsByPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        return commentRepository.findByPostOrderByCreatedAtAsc(post)
                .stream()
                .map(CommentDto.Response::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto.Response> getCommentsByAuthor(String authorEmail) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));

        return commentRepository.findByAuthorOrderByCreatedAtDesc(author)
                .stream()
                .map(CommentDto.Response::new)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto.Response updateComment(Long id, CommentDto.UpdateRequest request, String authorEmail) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
        
        User requestUser = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));

        // 작성자 확인
        if (!comment.getAuthor().equals(requestUser)) {
            throw new IllegalArgumentException("Only the author can update this comment");
        }

        comment.updateContent(request.getContent());
        Comment savedComment = commentRepository.save(comment);
        
        return new CommentDto.Response(savedComment);
    }

    @Override
    public void deleteComment(Long id, String authorEmail) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
        
        User requestUser = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authorEmail));

        // 작성자 확인
        if (!comment.getAuthor().equals(requestUser)) {
            throw new IllegalArgumentException("Only the author can delete this comment");
        }

        comment.delete();
        commentRepository.save(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto.Response> getRepliesByParentComment(Long parentCommentId) {
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", parentCommentId));

        return commentRepository.findByParentCommentOrderByCreatedAtAsc(parentComment)
                .stream()
                .map(CommentDto.Response::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getCommentCountByPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        return commentRepository.countByPost(post);
    }
}