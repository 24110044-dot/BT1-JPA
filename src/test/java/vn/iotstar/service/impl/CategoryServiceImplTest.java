package vn.iotstar.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.iotstar.entity.Category;
import vn.iotstar.entity.Video;
import vn.iotstar.exception.DuplicateCategoryNameException;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.VideoRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void insertTrimsNameAndPersistsCategory() {
        Category category = new Category(null, "  Điện tử  ", null);
        when(categoryRepository.existsByCateNameIgnoreCase("Điện tử")).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        Category saved = categoryService.insert(category);

        assertEquals("Điện tử", saved.getCateName());
        verify(categoryRepository).save(category);
    }

    @Test
    void insertRejectsDuplicateName() {
        Category category = new Category(null, "Điện tử", null);
        when(categoryRepository.existsByCateNameIgnoreCase("Điện tử")).thenReturn(true);

        assertThrows(DuplicateCategoryNameException.class, () -> categoryService.insert(category));
    }

    @Test
    void deleteRemovesExistingCategory() {
        Category category = new Category(8, "Âm nhạc", null);
        when(categoryRepository.findById(8)).thenReturn(Optional.of(category));
        when(videoRepository.countByCategory_CateId(8)).thenReturn(0L);

        categoryService.delete(8);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteRejectsCategoryWithVideos() {
        Category category = new Category(8, "Âm nhạc", null);
        when(categoryRepository.findById(8)).thenReturn(Optional.of(category));
        when(videoRepository.countByCategory_CateId(8)).thenReturn(2L);

        assertThrows(vn.iotstar.exception.CategoryHasVideosException.class, () -> categoryService.delete(8));
    }

    @Test
    void categoryAndVideoKeepBothSidesOfRelationshipInSync() {
        Category firstCategory = new Category(1, "Đầu tiên", null);
        Category secondCategory = new Category(2, "Thứ hai", null);
        firstCategory.setVideos(java.util.List.of());
        Video video = new Video();

        video.setCategory(firstCategory);
        video.setCategory(secondCategory);

        org.junit.jupiter.api.Assertions.assertFalse(firstCategory.getVideos().contains(video));
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(video), secondCategory.getVideos());
    }
}
