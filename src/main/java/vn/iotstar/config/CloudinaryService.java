package vn.iotstar.config;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import vn.iotstar.exception.InvalidImageException;

@Service
public class CloudinaryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudinaryService.class);
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final boolean configured;

    public CloudinaryService(@Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.configured = !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
        this.cloudinary = configured
                ? new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret,
                        "secure", true))
                : null;
    }

    public String uploadAvatar(MultipartFile file) {
        validate(file);
        if (!configured) {
            throw new InvalidImageException("Cloudinary chưa được cấu hình. Hãy thiết lập CLOUDINARY_CLOUD_NAME, "
                    + "CLOUDINARY_API_KEY và CLOUDINARY_API_SECRET.");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "categoryhub/avatars",
                    "resource_type", "image",
                    "overwrite", false));
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null) {
                throw new InvalidImageException("Cloudinary không trả về URL ảnh.");
            }
            return secureUrl.toString();
        } catch (IOException | RuntimeException exception) {
            throw new InvalidImageException("Không thể tải ảnh đại diện lên Cloudinary.");
        }
    }

    public void deleteAvatarIfManaged(String avatarUrl) {
        if (!configured || avatarUrl == null || !avatarUrl.contains("res.cloudinary.com/")) {
            return;
        }
        int uploadIndex = avatarUrl.indexOf("/upload/");
        if (uploadIndex < 0) {
            return;
        }
        String publicPath = avatarUrl.substring(uploadIndex + "/upload/".length());
        publicPath = publicPath.replaceFirst("^v\\d+/", "");
        int dotIndex = publicPath.lastIndexOf('.');
        if (dotIndex > 0) {
            publicPath = publicPath.substring(0, dotIndex);
        }
        if (!publicPath.startsWith("categoryhub/avatars/")) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicPath, ObjectUtils.emptyMap());
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not remove previous managed avatar from Cloudinary.");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Vui lòng chọn một tệp ảnh đại diện.");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new InvalidImageException("Ảnh đại diện không được vượt quá 5 MB.");
        }
        String type = file.getContentType();
        if (type == null || !java.util.Set.of("image/jpeg", "image/jpg", "image/png", "image/webp").contains(type.toLowerCase(java.util.Locale.ROOT))) {
            throw new InvalidImageException("Chỉ chấp nhận ảnh JPEG, PNG hoặc WEBP.");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 12 || !isPng(bytes) && !isJpeg(bytes) && !isWebp(bytes)) {
                throw new InvalidImageException("Nội dung tệp không phải ảnh JPEG, PNG hoặc WEBP hợp lệ.");
            }
        } catch (IOException exception) {
            throw new InvalidImageException("Không thể đọc tệp ảnh đại diện.");
        }
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
    }

    private boolean isPng(byte[] bytes) {
        return bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
                && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a
                && bytes[6] == 0x1a && bytes[7] == 0x0a;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P';
    }
}
