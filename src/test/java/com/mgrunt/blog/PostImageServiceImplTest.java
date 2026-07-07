package com.mgrunt.blog;

import com.mgrunt.blog.domain.entities.PostImage;
import com.mgrunt.blog.repositories.PostImageRepository;
import com.mgrunt.blog.services.impl.PostImageServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostImageServiceImplTest {

    @Mock
    private PostImageRepository postImageRepository;

    @InjectMocks
    private PostImageServiceImpl postImageService;

    private UUID imageId;
    private PostImage existingImage;

    @BeforeEach
    void setUp() {
        imageId = UUID.randomUUID();

        existingImage = new PostImage();
        existingImage.setId(imageId);
        existingImage.setFileName("test.jpg");
        existingImage.setContentType("image/jpeg");
        existingImage.setImageData(new byte[]{1, 2, 3});
    }

    // ---------- createImage() ----------

    @Test
    void createImage_shouldSaveAndReturnImage_whenFileIsValid() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "new-image.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
        );

        when(postImageRepository.save(any(PostImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostImage result = postImageService.createImage(file);

        ArgumentCaptor<PostImage> captor = ArgumentCaptor.forClass(PostImage.class);
        verify(postImageRepository).save(captor.capture());

        PostImage savedImage = captor.getValue();
        assertThat(savedImage.getFileName()).isEqualTo("new-image.jpg");
        assertThat(savedImage.getContentType()).isEqualTo("image/jpeg");
        assertThat(savedImage.getImageData()).containsExactly(1, 2, 3, 4);
        assertThat(savedImage.getCreatedAt()).isNotNull();

        assertThat(result).isEqualTo(savedImage);
    }

    @Test
    void createImage_shouldThrowException_whenImageTypeIsInvalid() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "text.txt", "text/plain", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> postImageService.createImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file type");

        verify(postImageRepository, never()).save(any());
    }

    @Test
    void createImage_shouldThrowException_whenFileIsTooLarge() {
        // 5 MB = 5 * 1024 * 1024 = 5242880 bytes. We create a file 1 byte larger.
        byte[] largeContent = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeContent
        );

        assertThatThrownBy(() -> postImageService.createImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is too large");

        verify(postImageRepository, never()).save(any());
    }

    @Test
    void createImage_shouldThrowRuntimeException_whenIOExceptionOccurs() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(100L);
        when(file.getBytes()).thenThrow(new IOException("Disk error"));

        assertThatThrownBy(() -> postImageService.createImage(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error saving image")
                .hasCauseInstanceOf(IOException.class);

        verify(postImageRepository, never()).save(any());
    }

    // ---------- getImageById() ----------

    @Test
    void getImageById_shouldReturnImage_whenExists() {
        when(postImageRepository.findById(imageId)).thenReturn(Optional.of(existingImage));

        PostImage result = postImageService.getImageById(imageId);

        assertThat(result).isEqualTo(existingImage);
        verify(postImageRepository).findById(imageId);
    }

    @Test
    void getImageById_shouldThrowException_whenNotFound() {
        when(postImageRepository.findById(imageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postImageService.getImageById(imageId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("cannot be found");
    }

    // ---------- deleteImage() ----------

    @Test
    void deleteImage_shouldCallRepositoryDelete() {
        postImageService.deleteImage(imageId);

        verify(postImageRepository).deleteById(imageId);
    }

    // ---------- updateImageIfChanged() ----------

    @Test
    void updateImageIfChanged_shouldReturnCurrentImage_whenNewFileIsNull() {
        PostImage result = postImageService.updateImageIfChanged(existingImage, null);

        assertThat(result).isEqualTo(existingImage);
        verify(postImageRepository, never()).save(any());
    }

    @Test
    void updateImageIfChanged_shouldReturnCurrentImage_whenNewFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "", "image/jpeg", new byte[0]
        );

        PostImage result = postImageService.updateImageIfChanged(existingImage, file);

        assertThat(result).isEqualTo(existingImage);
        verify(postImageRepository, never()).save(any());
    }

    @Test
    void updateImageIfChanged_shouldReturnCurrentImage_whenNewFileIsIdentical() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        PostImage result = postImageService.updateImageIfChanged(existingImage, file);

        assertThat(result).isEqualTo(existingImage);
        verify(postImageRepository, never()).save(any());
    }

    @Test
    void updateImageIfChanged_shouldCreateNewImage_whenFileHasDifferentContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{9, 9, 9} // Inne bajty
        );

        PostImage newlyCreatedImage = new PostImage();
        newlyCreatedImage.setFileName("test.jpg");

        when(postImageRepository.save(any(PostImage.class))).thenReturn(newlyCreatedImage);

        PostImage result = postImageService.updateImageIfChanged(existingImage, file);

        assertThat(result).isEqualTo(newlyCreatedImage);
        verify(postImageRepository).save(any(PostImage.class));
    }

    @Test
    void updateImageIfChanged_shouldCreateNewImage_whenCurrentImageIsNull() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "new.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        PostImage newlyCreatedImage = new PostImage();
        when(postImageRepository.save(any(PostImage.class))).thenReturn(newlyCreatedImage);

        PostImage result = postImageService.updateImageIfChanged(null, file);

        assertThat(result).isEqualTo(newlyCreatedImage);
        verify(postImageRepository).save(any(PostImage.class));
    }

    @Test
    void updateImageIfChanged_shouldThrowRuntimeException_whenIOExceptionOccursDuringComparison() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(3L);
        when(file.getBytes()).thenThrow(new IOException("Read error"));

        assertThatThrownBy(() -> postImageService.updateImageIfChanged(existingImage, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error comparing image")
                .hasCauseInstanceOf(IOException.class);
    }
}