package com.mgrunt.blog;

import com.mgrunt.blog.domain.dtos.CreateCommentRequest;
import com.mgrunt.blog.domain.entities.Comment;
import com.mgrunt.blog.domain.entities.Post;
import com.mgrunt.blog.domain.entities.User;
import com.mgrunt.blog.repositories.CommentRepository;
import com.mgrunt.blog.services.PostService;
import com.mgrunt.blog.services.impl.CommentServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostService postService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private UUID postId;
    private UUID commentId;
    private Post post;
    private User loggedInUser;
    private Comment comment;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        commentId = UUID.randomUUID();

        post = new Post();
        post.setId(postId);

        loggedInUser = new User();
        loggedInUser.setId(UUID.randomUUID());

        comment = Comment.builder()
                .id(commentId)
                .content("Great post!")
                .post(post)
                .author(loggedInUser)
                .build();
    }

    // ---------- getCommentsByPost() ----------

    @Test
    void getCommentsByPost_shouldReturnCommentsOrderedByCreatedAtDesc() {
        List<Comment> comments = List.of(comment);
        when(commentRepository.findByPostIdOrderByCreatedAtDesc(postId)).thenReturn(comments);

        List<Comment> result = commentService.getCommentsByPost(postId);

        assertThat(result).hasSize(1).containsExactly(comment);
        verify(commentRepository).findByPostIdOrderByCreatedAtDesc(postId);
    }

    @Test
    void getCommentsByPost_shouldReturnEmptyList_whenPostHasNoComments() {
        when(commentRepository.findByPostIdOrderByCreatedAtDesc(postId)).thenReturn(Collections.emptyList());

        List<Comment> result = commentService.getCommentsByPost(postId);

        assertThat(result).isEmpty();
    }

    // ---------- createComment() ----------

    @Test
    void createComment_shouldSaveAndReturnComment_whenPostExists() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Nice article!");

        when(postService.getPost(postId)).thenReturn(post);

        Comment result = commentService.createComment(loggedInUser, request, postId);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());

        Comment savedComment = captor.getValue();
        assertThat(savedComment.getContent()).isEqualTo("Nice article!");
        assertThat(savedComment.getPost()).isEqualTo(post);
        assertThat(savedComment.getAuthor()).isEqualTo(loggedInUser);

        assertThat(result.getContent()).isEqualTo("Nice article!");
        assertThat(result.getPost()).isEqualTo(post);
        assertThat(result.getAuthor()).isEqualTo(loggedInUser);
    }

    @Test
    void createComment_shouldPropagateException_whenPostDoesNotExist() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Nice article!");

        when(postService.getPost(postId)).thenThrow(new EntityNotFoundException("Post with id '" + postId + "' not found."));

        assertThatThrownBy(() -> commentService.createComment(loggedInUser, request, postId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");

        verify(commentRepository, never()).save(any());
    }

    // ---------- getComment() ----------

    @Test
    void getComment_shouldReturnComment_whenExists() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        Comment result = commentService.getComment(commentId);

        assertThat(result).isEqualTo(comment);
    }

    @Test
    void getComment_shouldThrowException_whenNotFound() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getComment(commentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("does not exist");
    }

    // ---------- deleteComment() ----------

    @Test
    void deleteComment_shouldDeleteComment_whenExists() {
        when(commentRepository.existsById(commentId)).thenReturn(true);

        commentService.deleteComment(commentId);

        verify(commentRepository).deleteById(commentId);
    }

    @Test
    void deleteComment_shouldThrowException_whenNotFound() {
        when(commentRepository.existsById(commentId)).thenReturn(false);

        assertThatThrownBy(() -> commentService.deleteComment(commentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("does not exist");

        verify(commentRepository, never()).deleteById(any());
    }
}