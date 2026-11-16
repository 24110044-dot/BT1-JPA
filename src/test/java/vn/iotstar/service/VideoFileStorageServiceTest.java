package vn.iotstar.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import vn.iotstar.exception.InvalidVideoException;

class VideoFileStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesValidMp4UnderGeneratedNameAndCanDeleteIt() throws Exception {
        Path uploadPath = temporaryDirectory.resolve("videos");
        VideoFileStorageService storage = new VideoFileStorageService(uploadPath.toString(), 1024);
        byte[] mp4Header = new byte[] { 0, 0, 0, 8, 'f', 't', 'y', 'p', 0, 0, 0, 0 };
        MockMultipartFile file = new MockMultipartFile("file", "client-name.mp4", "video/mp4", mp4Header);

        String storedUrl = storage.store(file);
        Path storedFile = uploadPath.resolve(storedUrl.substring(storedUrl.lastIndexOf('/') + 1));

        assertTrue(Files.exists(storedFile));
        assertTrue(storedUrl.matches("/uploads/videos/[0-9a-fA-F-]{36}\\.mp4"));
        storage.delete(storedUrl);
        assertFalse(Files.exists(storedFile));
    }

    @Test
    void rejectsSpoofedVideoExtensionBeforeWriting() {
        VideoFileStorageService storage = new VideoFileStorageService(temporaryDirectory.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile("file", "not-a-video.mp4", "video/mp4",
                "plain text".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThrows(InvalidVideoException.class, () -> storage.store(file));
        assertFalse(Files.exists(temporaryDirectory.resolve("not-a-video.mp4")));
    }

    @Test
    void rejectsVideoAboveConfiguredSizeLimit() {
        VideoFileStorageService storage = new VideoFileStorageService(temporaryDirectory.toString(), 4);
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4",
                new byte[] { 0, 0, 0, 8, 'f', 't', 'y', 'p' });

        assertThrows(InvalidVideoException.class, () -> storage.store(file));
    }
}
