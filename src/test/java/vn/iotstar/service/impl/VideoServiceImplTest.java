package vn.iotstar.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;

import vn.iotstar.dto.VideoForm;
import vn.iotstar.entity.Video;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.VideoRepository;

@ExtendWith(MockitoExtension.class)
class VideoServiceImplTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private VideoServiceImpl videoService;

    @Test
    void adminSearchTrimsKeywordAndPassesFilters() {
        Page<Video> expected = Page.empty();
        when(videoRepository.searchAdmin(eq("lesson"), eq(3), eq(true), any(PageRequest.class)))
                .thenReturn(expected);

        Page<Video> actual = videoService.searchAdmin(" lesson ", 3, true, 0, 12);

        assertSame(expected, actual);
        verify(videoRepository).searchAdmin(eq("lesson"), eq(3), eq(true), any(PageRequest.class));
    }

    @Test
    void homeLoadsAtMostTenNewestActiveVideos() {
        List<Video> expected = List.of(new Video());
        when(videoRepository.findTop10ByActiveTrueOrderByUploadedAtDescTitleAsc()).thenReturn(expected);

        assertSame(expected, videoService.featuredActive());

        verify(videoRepository).findTop10ByActiveTrueOrderByUploadedAtDescTitleAsc();
    }

    @Test
    void openingPublicVideoIncrementsViewsAndReturnsFreshVideo() {
        Video video = new Video();
        video.setVideoId("video-1");
        video.setViews(24);
        when(videoRepository.incrementViewsForActiveVideo("video-1")).thenReturn(1);
        when(videoRepository.findByVideoIdAndActiveTrue("video-1"))
                .thenReturn(java.util.Optional.of(video));

        java.util.Optional<Video> result = videoService.recordViewAndFindActiveById("video-1");

        assertTrue(result.isPresent());
        assertEquals(24, result.get().getViews());
        verify(videoRepository).incrementViewsForActiveVideo("video-1");
    }

    @Test
    void hiddenPublicVideoIsNotReturnedOrCounted() {
        when(videoRepository.incrementViewsForActiveVideo("hidden-video")).thenReturn(0);

        assertTrue(videoService.recordViewAndFindActiveById("hidden-video").isEmpty());
    }

    @Test
    void storefrontCategorySearchOnlyLoadsActiveVideos() {
        Page<Video> expected = Page.empty();
        when(videoRepository.findByActiveTrueAndCategory_CateId(eq(5), any(PageRequest.class)))
                .thenReturn(expected);

        Page<Video> actual = videoService.browseActive(5, 0, 12);

        assertSame(expected, actual);
        verify(videoRepository).findByActiveTrueAndCategory_CateId(eq(5), any(PageRequest.class));
    }

    @Test
    void createTrimsIdentifierAndTitleAndSetsDefaults() {
        when(videoRepository.existsById("video-1")).thenReturn(false);
        when(videoRepository.save(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VideoForm form = new VideoForm();
        form.setVideoId(" video-1 ");
        form.setTitle(" Tiêu đề ");

        Video saved = videoService.create(form);

        assertEquals("video-1", saved.getVideoId());
        assertEquals("Tiêu đề", saved.getTitle());
        assertEquals(0, saved.getViews());
        assertEquals(true, saved.getActive());
    }

    @Test
    void updateCanChangeVideoIdWithoutMutatingPersistentIdentifier() {
        Video existing = new Video();
        existing.setVideoId("old-id");
        existing.setTitle("Old title");
        when(videoRepository.findById("old-id")).thenReturn(java.util.Optional.of(existing));
        when(videoRepository.existsById("new-id")).thenReturn(false);
        when(videoRepository.save(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VideoForm form = new VideoForm();
        form.setVideoId(" new-id ");
        form.setTitle("Updated title");

        Video updated = videoService.update("old-id", form);

        assertEquals("new-id", updated.getVideoId());
        assertEquals("Updated title", updated.getTitle());
        verify(videoRepository).delete(existing);
        verify(videoRepository).flush();
    }
}
