package vn.iotstar.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import vn.iotstar.exception.InvalidImageException;

class ImageStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesImageWithGeneratedName() throws Exception {
        ImageStorageService service = new ImageStorageService(temporaryDirectory.toString());
        MockMultipartFile image = new MockMultipartFile(
                "imageFile", "avatar.png", "image/png", createPng());

        String imageUrl = service.store(image);
        Path storedFile = temporaryDirectory.getParent().resolve("categories")
                .resolve(imageUrl.substring(imageUrl.lastIndexOf('/') + 1));

        assertTrue(imageUrl.startsWith("/uploads/categories/"));
        assertTrue(Files.exists(storedFile));
        assertFalse(imageUrl.contains("avatar.png"));
    }

    @Test
    void rejectsNonImageContentRegardlessOfFilenameAndMimeType() {
        ImageStorageService service = new ImageStorageService(temporaryDirectory.toString());
        MockMultipartFile fakeImage = new MockMultipartFile(
                "imageFile", "malicious.png", "image/png", "not an image".getBytes());

        assertThrows(InvalidImageException.class, () -> service.store(fakeImage));
    }

    @Test
    void rejectsFilesOverFiveMegabytes() {
        ImageStorageService service = new ImageStorageService(temporaryDirectory.toString());
        MockMultipartFile oversizedImage = new MockMultipartFile(
                "imageFile", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertThrows(InvalidImageException.class, () -> service.store(oversizedImage));
    }

    private byte[] createPng() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
