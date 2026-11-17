package vn.iotstar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import vn.iotstar.config.ImageStorageService;
import vn.iotstar.dto.UserVideoForm;
import vn.iotstar.entity.Category;
import vn.iotstar.entity.User;
import vn.iotstar.entity.Video;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.VideoRepository;
import vn.iotstar.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class UserVideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoFileStorageService storageService;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private CategoryRepository categoryRepository;

    private UserVideoService service;

    @BeforeEach
    void setUp() {
        service = new UserVideoService(videoRepository, userRepository, storageService, imageStorageService,
                categoryRepository);
    }

    @Test
    void userVideoPageUsesSixItemPageable() {
        when(videoRepository.findByUser_IdOrderByUploadedAtDesc(eq(12), any(PageRequest.class)))
                .thenReturn(Page.empty());

        service.listForUser(12, 1);

        verify(videoRepository).findByUser_IdOrderByUploadedAtDesc(12, PageRequest.of(1, 6));
    }

    @Test
    void openingPublishedVideoIncrementsViewsAndReturnsUpdatedCount() {
        Video video = new Video();
        video.setVideoId("video-id");
        video.setActive(true);
        video.setViews(38);
        when(videoRepository.incrementViewsForActiveVideo("video-id")).thenReturn(1);
        when(videoRepository.findByVideoIdAndActiveTrue("video-id")).thenReturn(Optional.of(video));

        Video viewed = service.recordViewAndGetPublished("video-id");

        assertEquals(38, viewed.getViews());
        verify(videoRepository).incrementViewsForActiveVideo("video-id");
    }

    @Test
    void inactiveOrMissingVideoDoesNotLoadAsPublished() {
        when(videoRepository.incrementViewsForActiveVideo("hidden-video")).thenReturn(0);

        assertThrows(vn.iotstar.exception.VideoNotFoundException.class,
                () -> service.recordViewAndGetPublished("hidden-video"));
        verify(videoRepository, never()).findByVideoIdAndActiveTrue("hidden-video");
    }

    @Test
    void updateWithoutReplacementKeepsStoredVideoPath() throws Exception {
        Category category = new Category(3, "Điện thoại", null);
        User owner = new User();
        owner.setId(12);
        Video video = new Video();
        video.setVideoId("video-id");
        video.setUser(owner);
        video.setVideoUrl("/uploads/videos/existing.mp4");
        video.setTitle("Old");
        when(videoRepository.findById("video-id")).thenReturn(Optional.of(video));
        when(categoryRepository.findById(3)).thenReturn(Optional.of(category));
        when(videoRepository.saveAndFlush(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserVideoForm form = new UserVideoForm();
        form.setTitle("Updated");
        form.setCategoryId(3);

        Video updated = service.update("video-id", form, 12, 5);

        assertEquals("/uploads/videos/existing.mp4", updated.getVideoUrl());
        assertEquals("Updated", updated.getTitle());
        assertSame(category, updated.getCategory());
        verify(storageService, never()).store(any(MultipartFile.class));
    }

    @Test
    void updateWithThumbnailFileStoresImageAndSetsPosterPath() throws Exception {
        Category category = new Category(3, "Điện thoại", null);
        User owner = new User();
        owner.setId(12);
        Video video = new Video();
        video.setVideoId("video-id");
        video.setUser(owner);
        video.setVideoUrl("/uploads/videos/existing.mp4");
        video.setTitle("Old");
        when(videoRepository.findById("video-id")).thenReturn(Optional.of(video));
        when(categoryRepository.findById(3)).thenReturn(Optional.of(category));
        when(imageStorageService.store(any(MultipartFile.class), eq("video-thumbnails")))
                .thenReturn("/uploads/video-thumbnails/generated.png");
        when(videoRepository.saveAndFlush(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserVideoForm form = new UserVideoForm();
        form.setTitle("Updated");
        form.setCategoryId(3);
        form.setThumbnailFile(new org.springframework.mock.web.MockMultipartFile(
                "thumbnailFile", "local.png", "image/png", new byte[] { 1 }));

        Video updated = service.update("video-id", form, 12, 5);

        assertEquals("/uploads/video-thumbnails/generated.png", updated.getPoster());
        verify(imageStorageService).store(form.getThumbnailFile(), "video-thumbnails");
    }

    @Test
    void createAssignsTheConfirmedCategoryToUploadedVideo() throws Exception {
        Category category = new Category(3, "Điện thoại", null);
        category.setVideos(java.util.List.of());
        User owner = new User();
        owner.setId(12);
        when(userRepository.findById(12)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(3)).thenReturn(Optional.of(category));
        when(storageService.store(any(MultipartFile.class))).thenReturn("/uploads/videos/new-video.mp4");
        when(videoRepository.saveAndFlush(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserVideoForm form = new UserVideoForm();
        form.setTitle("Video về điện thoại");
        form.setCategoryId(3);
        form.setFile(new org.springframework.mock.web.MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[] { 1 }));

        Video created = service.create(form, 12);

        assertSame(category, created.getCategory());
        assertEquals("/uploads/videos/new-video.mp4", created.getVideoUrl());
    }

    @Test
    void anotherUserCannotReadOrChangeVideoOwnership() {
        User owner = new User();
        owner.setId(12);
        Video video = new Video();
        video.setVideoId("video-id");
        video.setUser(owner);
        when(videoRepository.findById("video-id")).thenReturn(Optional.of(video));

        assertThrows(AccessDeniedException.class, () -> service.getForOwner("video-id", 13, 5));
        assertNull(video.getVideoUrl());
    }

    @Test
    void removeThumbnailClearsPosterAndDeletesUnsharedManagedImage() throws Exception {
        User owner = new User();
        owner.setId(12);
        Video video = new Video();
        video.setVideoId("video-id");
        video.setUser(owner);
        video.setPoster("/uploads/video-thumbnails/current.png");
        when(videoRepository.findById("video-id")).thenReturn(Optional.of(video));
        when(videoRepository.existsByPoster("/uploads/video-thumbnails/current.png")).thenReturn(false);

        service.removeThumbnail("video-id", 12, 5);

        assertNull(video.getPoster());
        verify(imageStorageService).deleteManagedImage("/uploads/video-thumbnails/current.png");
    }
}
