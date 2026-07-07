package com.mgrunt.blog;

import com.mgrunt.blog.domain.entities.Category;
import com.mgrunt.blog.domain.entities.Post;
import com.mgrunt.blog.repositories.CategoryRepository;
import com.mgrunt.blog.services.impl.CategoryServiceImpl;
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
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = new Category();
        category.setId(categoryId);
        category.setName("Technology");
    }

    // ---------- listCategories() ----------

    @Test
    void listCategories_shouldReturnAllCategories() {
        Category second = new Category();
        second.setName("Sports");
        List<Category> categories = List.of(category, second);

        when(categoryRepository.findAllWithPostCount()).thenReturn(categories);

        List<Category> result = categoryService.listCategories();

        assertThat(result).hasSize(2).containsExactly(category, second);
        verify(categoryRepository).findAllWithPostCount();
    }

    @Test
    void listCategories_shouldReturnEmptyList_whenNoCategoriesExist() {
        when(categoryRepository.findAllWithPostCount()).thenReturn(Collections.emptyList());

        List<Category> result = categoryService.listCategories();

        assertThat(result).isEmpty();
    }

    // ---------- createCategory() ----------

    @Test
    void createCategory_shouldSaveCategory_whenNameIsUnique() {
        when(categoryRepository.existsByNameIgnoreCase(category.getName())).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        Category result = categoryService.createCategory(category);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());

        assertThat(result).isEqualTo(category);
        assertThat(captor.getValue().getName()).isEqualTo("Technology");
    }

    @Test
    void createCategory_shouldThrowException_whenNameAlreadyExists() {
        when(categoryRepository.existsByNameIgnoreCase(category.getName())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(category))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(categoryRepository, never()).save(any());
    }

    // ---------- deleteCategory() ----------

    @Test
    void deleteCategory_shouldDeleteCategory_whenNoPostsAssociated() {
        category.setPosts(Collections.emptyList());
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    void deleteCategory_shouldThrowException_whenCategoryHasPosts() {
        category.setPosts(List.of(new Post()));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("posts associated");

        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void deleteCategory_shouldDoNothing_whenCategoryDoesNotExist() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository, never()).deleteById(any());
    }

    // ---------- getCategoryById() ----------

    @Test
    void getCategoryById_shouldReturnCategory_whenExists() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        Category result = categoryService.getCategoryById(categoryId);

        assertThat(result).isEqualTo(category);
    }

    @Test
    void getCategoryById_shouldThrowException_whenNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }
}