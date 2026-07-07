package com.mgrunt.blog;

import com.mgrunt.blog.domain.entities.Post;
import com.mgrunt.blog.domain.entities.Tag;
import com.mgrunt.blog.repositories.TagRepository;
import com.mgrunt.blog.services.impl.TagServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    private UUID tagId;
    private Tag existingTag;

    @BeforeEach
    void setUp() {
        tagId = UUID.randomUUID();

        existingTag = Tag.builder()
                .id(tagId)
                .name("java")
                .posts(new HashSet<>())
                .build();
    }

    // ---------- getTags() ----------

    @Test
    void getTags_shouldReturnAllTagsWithPostCount() {
        List<Tag> tags = List.of(existingTag);
        when(tagRepository.findAllWithPostCount()).thenReturn(tags);

        List<Tag> result = tagService.getTags();

        assertThat(result).hasSize(1).containsExactly(existingTag);
        verify(tagRepository).findAllWithPostCount();
    }

    // ---------- createTags() ----------

    @Test
    void createTags_shouldCreateOnlyNewTagsAndReturnAll() {
        Set<String> tagNamesInput = Set.of("java", "spring");

        when(tagRepository.findByNameIn(tagNamesInput)).thenReturn(List.of(existingTag));

        Tag newTag = Tag.builder().name("spring").posts(new HashSet<>()).build();
        when(tagRepository.saveAll(any())).thenReturn(new ArrayList<>(List.of(newTag)));

        List<Tag> result = tagService.createTags(tagNamesInput);

        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository).saveAll(captor.capture());

        List<Tag> savedTags = captor.getValue();
        assertThat(savedTags).hasSize(1);
        assertThat(savedTags.get(0).getName()).isEqualTo("spring");

        assertThat(result).hasSize(2).contains(existingTag, newTag);
    }

    @Test
    void createTags_shouldNotSaveAnything_whenAllTagsAlreadyExist() {
        Set<String> tagNamesInput = Set.of("java");

        when(tagRepository.findByNameIn(tagNamesInput)).thenReturn(List.of(existingTag));

        List<Tag> result = tagService.createTags(tagNamesInput);

        verify(tagRepository, never()).saveAll(any());
        assertThat(result).hasSize(1).containsExactly(existingTag);
    }

    // ---------- deleteTag() ----------

    @Test
    void deleteTag_shouldDeleteTag_whenExistsAndHasNoPosts() {
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(existingTag));

        tagService.deleteTag(tagId);

        verify(tagRepository).deleteById(tagId);
    }

    @Test
    void deleteTag_shouldThrowException_whenTagHasPosts() {
        existingTag.getPosts().add(new Post());
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(existingTag));

        assertThatThrownBy(() -> tagService.deleteTag(tagId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete tag with posts");

        verify(tagRepository, never()).deleteById(any());
    }

    @Test
    void deleteTag_shouldDoNothing_whenTagDoesNotExist() {
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        tagService.deleteTag(tagId);

        verify(tagRepository, never()).deleteById(any());
    }

    // ---------- getTagById() ----------

    @Test
    void getTagById_shouldReturnTag_whenExists() {
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(existingTag));

        Tag result = tagService.getTagById(tagId);

        assertThat(result).isEqualTo(existingTag);
    }

    @Test
    void getTagById_shouldThrowException_whenNotFound() {
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.getTagById(tagId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // ---------- getTagByIds() ----------

    @Test
    void getTagByIds_shouldReturnTags_whenAllIdsExist() {
        UUID secondTagId = UUID.randomUUID();
        Tag secondTag = Tag.builder().id(secondTagId).name("spring").build();

        Set<UUID> ids = Set.of(tagId, secondTagId);
        List<Tag> foundTags = List.of(existingTag, secondTag);

        when(tagRepository.findAllById(ids)).thenReturn(foundTags);

        List<Tag> result = tagService.getTagByIds(ids);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(existingTag, secondTag);
    }

    @Test
    void getTagByIds_shouldThrowException_whenSomeIdsDoNotExist() {
        UUID secondTagId = UUID.randomUUID();
        Set<UUID> ids = Set.of(tagId, secondTagId);

        when(tagRepository.findAllById(ids)).thenReturn(List.of(existingTag));

        assertThatThrownBy(() -> tagService.getTagByIds(ids))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Not all specified tag IDs exist");
    }
}