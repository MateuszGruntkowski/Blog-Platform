package com.mgrunt.blog;

import com.mgrunt.blog.domain.CreatePostRequest;
import com.mgrunt.blog.domain.PostStatus;
import com.mgrunt.blog.domain.UpdatePostRequest;
import com.mgrunt.blog.domain.dtos.PostDto;
import com.mgrunt.blog.domain.entities.*;
import com.mgrunt.blog.mappers.PostMapper;
import com.mgrunt.blog.repositories.PostRepository;
import com.mgrunt.blog.services.CategoryService;
import com.mgrunt.blog.services.PostImageService;
import com.mgrunt.blog.services.TagService;
import com.mgrunt.blog.services.UserService;
import com.mgrunt.blog.services.impl.PostServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private TagService tagService;
    @Mock
    private UserService userService;
    @Mock
    private PostImageService postImageService;
    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostServiceImpl postService;

    private UUID postId;
    private UUID authorId;
    private User author;
    private Post existingPost;
    private Category category;
    private Tag tag;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        authorId = UUID.randomUUID();

        author = new User();
        author.setId(authorId);

        category = new Category();
        category.setId(UUID.randomUUID());

        tag = Tag.builder().id(UUID.randomUUID()).name("java").build();

        existingPost = new Post();
        existingPost.setId(postId);
        existingPost.setTitle("Original Title");
        existingPost.setContent("This is some sample content for the blog post.");
        existingPost.setStatus(PostStatus.PUBLISHED);
        existingPost.setAuthor(author);
        existingPost.setCategory(category);
        existingPost.setTags(new HashSet<>(List.of(tag)));
        existingPost.setLikes(new HashSet<>());
    }

    // ---------- getPost() ----------

    @Test
    void getPost_shouldReturnPost_whenExists() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        Post result = postService.getPost(postId);

        assertThat(result).isEqualTo(existingPost);
    }

    @Test
    void getPost_shouldThrowException_whenNotFound() {
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(postId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // ---------- getAllPosts() ----------

    @Test
    void getAllPosts_shouldFilterByCategoryAndTag_whenBothAreProvided() {
        UUID catId = category.getId();
        UUID tId = tag.getId();
        when(categoryService.getCategoryById(catId)).thenReturn(category);
        when(tagService.getTagById(tId)).thenReturn(tag);
        when(postRepository.findAllByStatusAndCategoryAndTagsContainingOrderByCreatedAtDesc(PostStatus.PUBLISHED, category, tag))
                .thenReturn(List.of(existingPost));

        List<Post> result = postService.getAllPosts(catId, tId);

        assertThat(result).containsExactly(existingPost);
    }

    @Test
    void getAllPosts_shouldFilterByCategoryOnly_whenTagIdIsNull() {
        UUID catId = category.getId();
        when(categoryService.getCategoryById(catId)).thenReturn(category);
        when(postRepository.findAllByStatusAndCategoryOrderByCreatedAtDesc(PostStatus.PUBLISHED, category))
                .thenReturn(List.of(existingPost));

        List<Post> result = postService.getAllPosts(catId, null);

        assertThat(result).containsExactly(existingPost);
    }

    @Test
    void getAllPosts_shouldFilterByTagOnly_whenCategoryIdIsNull() {
        UUID tId = tag.getId();
        when(tagService.getTagById(tId)).thenReturn(tag);
        when(postRepository.findAllByStatusAndTagsContainingOrderByCreatedAtDesc(PostStatus.PUBLISHED, tag))
                .thenReturn(List.of(existingPost));

        List<Post> result = postService.getAllPosts(null, tId);

        assertThat(result).containsExactly(existingPost);
    }

    @Test
    void getAllPosts_shouldReturnAllPublished_whenFiltersAreNull() {
        when(postRepository.findAllByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED))
                .thenReturn(List.of(existingPost));

        List<Post> result = postService.getAllPosts(null, null);

        assertThat(result).containsExactly(existingPost);
    }

    // ---------- getDraftPosts() ----------

    @Test
    void getDraftPosts_shouldReturnDraftsForUser() {
        when(postRepository.findAllByAuthorAndStatusOrderByCreatedAtDesc(author, PostStatus.DRAFT))
                .thenReturn(List.of(existingPost));

        List<Post> result = postService.getDraftPosts(author);

        assertThat(result).containsExactly(existingPost);
    }

    // ---------- createPost() ----------

    @Test
    void createPost_shouldSaveAndReturnPost_withCalculatedReadingTime() {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("New Post");
        // 201 words will force the ceiling to be 2 minutes with WORDS_PER_MINUTE = 200
        request.setContent(String.join(" ", Collections.nCopies(201, "word")));
        request.setStatus(PostStatus.PUBLISHED);
        request.setCategoryId(category.getId());
        request.setTagIds(Set.of(tag.getId()));

        MockMultipartFile mockFile = new MockMultipartFile("image", "img.jpg", "image/jpeg", new byte[]{1});
        request.setImage(mockFile);

        PostImage mockSavedImage = new PostImage();

        when(categoryService.getCategoryById(category.getId())).thenReturn(category);
        when(tagService.getTagByIds(Set.of(tag.getId()))).thenReturn(List.of(tag));
        when(postImageService.createImage(mockFile)).thenReturn(mockSavedImage);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = postService.createPost(author, request);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());

        Post savedPost = captor.getValue();
        assertThat(savedPost.getTitle()).isEqualTo("New Post");
        assertThat(savedPost.getReadingTime()).isEqualTo(2); // 201 / 200 = 1.005 -> ceil = 2
        assertThat(savedPost.getCategory()).isEqualTo(category);
        assertThat(savedPost.getTags()).containsExactly(tag);
        assertThat(savedPost.getImage()).isEqualTo(mockSavedImage);
        assertThat(result).isEqualTo(savedPost);
    }

    // ---------- updatePost() ----------

    @Test
    void updatePost_shouldUpdateFields_whenUserIsAuthor() {
        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("Updated Title");
        request.setContent("Short content");
        request.setStatus(PostStatus.DRAFT);
        request.setCategoryId(category.getId());
        request.setTagIds(Set.of(tag.getId()));

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = postService.updatePost(author, postId, request);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getReadingTime()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(PostStatus.DRAFT);
        verify(postRepository).save(existingPost);
    }

    @Test
    void updatePost_shouldThrowAccessDenied_whenUserIsNotAuthor() {
        User stranger = new User();
        stranger.setId(UUID.randomUUID()); // ID different from the post author

        UpdatePostRequest request = new UpdatePostRequest();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        assertThatThrownBy(() -> postService.updatePost(stranger, postId, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed to edit");

        verify(postRepository, never()).save(any());
    }

    // ---------- deletePost() ----------

    @Test
    void deletePost_shouldDelete_whenPostExists() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        postService.deletePost(postId);

        verify(postRepository).delete(existingPost);
    }

    // ---------- toggleLike() ----------

    @Test
    void toggleLike_shouldAddLike_whenUserHasNotLikedYet() {
        User userWhoLikes = new User();
        UUID likerId = UUID.randomUUID();
        userWhoLikes.setId(likerId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(userService.getUserById(likerId)).thenReturn(userWhoLikes);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = postService.toggleLike(postId, likerId);

        assertThat(result.getLikes()).contains(userWhoLikes);
    }

    @Test
    void toggleLike_shouldRemoveLike_whenUserAlreadyLiked() {
        User userWhoLiked = new User();
        UUID likerId = UUID.randomUUID();
        userWhoLiked.setId(likerId);

        existingPost.getLikes().add(userWhoLiked); // Post already has a like from this user

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(userService.getUserById(likerId)).thenReturn(userWhoLiked);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = postService.toggleLike(postId, likerId);

        assertThat(result.getLikes()).doesNotContain(userWhoLiked);
    }

    // ---------- addUserContext() ----------

    @Test
    void addUserContext_shouldSetIsLikedToTrue_whenUserLikedPost() {
        User user = new User();
        user.setId(authorId);
        existingPost.getLikes().add(user);

        PostDto mockDto = new PostDto();
        when(postMapper.toDto(existingPost)).thenReturn(mockDto);

        PostDto result = postService.addUserContext(existingPost, authorId);

        assertThat(result.getIsLikedByCurrentUser()).isTrue();
    }

    @Test
    void addUserContext_shouldSetIsLikedToFalse_whenUserDidNotLikePost() {
        PostDto mockDto = new PostDto();
        when(postMapper.toDto(existingPost)).thenReturn(mockDto);

        PostDto result = postService.addUserContext(existingPost, UUID.randomUUID());

        assertThat(result.getIsLikedByCurrentUser()).isFalse();
    }
}