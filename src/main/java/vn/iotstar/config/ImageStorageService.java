package vn.iotstar.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import vn.iotstar.exception.InvalidImageException;

@Component
public class ImageStorageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;

    private final Path uploadDirectory;

    public ImageStorageService(@Value("${app.upload.dir:uploads/categories}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) throws IOException {
        return store(file, "categories");
    }

    public String store(MultipartFile file, String folder) throws IOException {
        if (!"categories".equals(folder) && !"products".equals(folder) && !"users".equals(folder)
                && !"video-thumbnails".equals(folder)) {
            throw new InvalidImageException("Thư mục lưu ảnh không hợp lệ.");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Vui lòng chọn một tệp hình ảnh.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidImageException("Dung lượng hình ảnh không được vượt quá 5 MB.");
        }

        String extension = validateAndGetExtension(file);
        Files.createDirectories(uploadDirectory);
        Path destinationDirectory = uploadDirectory.getParent().resolve(folder).toAbsolutePath().normalize();
        Files.createDirectories(destinationDirectory);
        String filename = UUID.randomUUID() + "." + extension;
        Path destination = destinationDirectory.resolve(filename).normalize();
        if (!destination.getParent().equals(destinationDirectory)) {
            throw new InvalidImageException("Tên tệp hình ảnh không hợp lệ.");
        }
        try {
            file.transferTo(destination);
        } catch (IOException exception) {
            Files.deleteIfExists(destination);
            throw exception;
        }
        return "/uploads/" + folder + "/" + filename;
    }

    public void deleteManagedImage(String imageUrl) throws IOException {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return;
        }
        String relative = imageUrl.substring("/uploads/".length());
        String[] parts = relative.split("/", -1);
        if (parts.length != 2
                || !java.util.Set.of("categories", "products", "users", "video-thumbnails").contains(parts[0])
                || parts[1].isBlank() || parts[1].contains("\\") || parts[1].contains("..")) {
            return;
        }
        Path directory = uploadDirectory.getParent().resolve(parts[0]).toAbsolutePath().normalize();
        Files.deleteIfExists(directory.resolve(parts[1]).normalize());
    }

    private String validateAndGetExtension(MultipartFile file) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) {
                throw new InvalidImageException("Tệp tải lên không phải hình ảnh hợp lệ.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidImageException("Chỉ chấp nhận hình ảnh PNG, JPG, GIF hoặc BMP.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new InvalidImageException("Kích thước hình ảnh không hợp lệ hoặc quá lớn.");
                }
                if (reader.read(0) == null) {
                    throw new InvalidImageException("Tệp tải lên không phải hình ảnh hợp lệ.");
                }

                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                return switch (format) {
                    case "jpeg", "jpg" -> "jpg";
                    case "png" -> "png";
                    case "gif" -> "gif";
                    case "bmp" -> "bmp";
                    default -> throw new InvalidImageException("Chỉ chấp nhận hình ảnh PNG, JPG, GIF hoặc BMP.");
                };
            } finally {
                reader.dispose();
            }
        }
    }
}
