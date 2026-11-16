package vn.iotstar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import vn.iotstar.dto.UserVideoForm;
import vn.iotstar.entity.User;
import vn.iotstar.entity.Video;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.VideoRepository;

@ExtendWith(MockitoExtension.class)
class UserVideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoFileStorageService storageService;

    private UserVideoService service;

    @BeforeEach
    void setUp() {
        service = new UserVideoService(videoRepository, userRepository, storageService);
    }

    @Test
    void userVideoPageUsesSixItemPageable() {
        when(videoRepository.findByUser_IdOrderByUploadedAtDesc(eq(12), any(PageRequest.class)))
                .thenReturn(Page.empty());

        service.listForUser(12, 1);

        verify(videoRepository).findByUser_IdOrderByUploadedAtDesc(12, PageRequest.of(1, 6));
    }

    @Test
    void updateWithoutReplacementKeepsStoredVideoPath() throws Exception {
        User owner = new User();
        owner.setId(12);
        Video video = new Video();
        video.setVideoId("video-id");
        video.setUser(owner);
        video.setVideoUrl("/uploads/videos/existing.mp4");
        video.setTitle("Old");
        when(videoRepository.findById("video-id")).thenReturn(Optional.of(video));
        when(videoRepository.saveAndFlush(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserVideoForm form = new UserVideoForm();
        form.setTitle("Updated");

        Video updated = service.update("video-id", form, 12, 5);

        assertEquals("/uploads/videos/existing.mp4", updated.getVideoUrl());
        assertEquals("Updated", updated.getTitle());
        verify(storageService, never()).store(any(MultipartFile.class));
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
}
