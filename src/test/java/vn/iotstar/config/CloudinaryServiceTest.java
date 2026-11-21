package vn.iotstar.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import vn.iotstar.exception.InvalidImageException;

class CloudinaryServiceTest {

    private final CloudinaryService service = new CloudinaryService("", "", "");

    @Test
    void rejectsEmptyAvatar() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[0]);
        assertThrows(InvalidImageException.class, () -> service.uploadAvatar(file));
    }

    @Test
    void rejectsWrongMimeType() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.txt", "text/plain", "text".getBytes());
        assertThrows(InvalidImageException.class, () -> service.uploadAvatar(file));
    }

    @Test
    void rejectsAvatarLargerThanFiveMb() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[5 * 1024 * 1024 + 1]);
        assertThrows(InvalidImageException.class, () -> service.uploadAvatar(file));
    }
}
