package com.mgrunt.blog.services.impl;

import com.mgrunt.blog.domain.dtos.CreateCommentRequest;
import com.mgrunt.blog.domain.entities.Comment;
import com.mgrunt.blog.domain.entities.Post;
import com.mgrunt.blog.domain.entities.User;
import com.mgrunt.blog.repositories.CommentRepository;
import com.mgrunt.blog.services.CommentService;
import com.mgrunt.blog.services.PostService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;

    @Override
    public List<Comment> getCommentsByPost(UUID postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
    }

    @Override
    public Comment createComment(User loggedInUser, CreateCommentRequest createCommentRequest, UUID postId) {
        Post post = postService.getPost(postId);
        Comment comment = Comment.builder()
                .content(createCommentRequest.getContent())
                .post(post)
                .author(loggedInUser)
                .build();
        commentRepository.save(comment);

        return comment;
    }

    @Override
    public Comment getComment(UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comment with id: " + id + "does not exist."));
    }

    @Override
    public void deleteComment(UUID id) {
        if (!commentRepository.existsById(id)) {
            throw new EntityNotFoundException("Comment with id: " + id + " does not exist.");
        }
        commentRepository.deleteById(id);
    }
}
