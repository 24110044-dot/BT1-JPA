package vn.iotstar.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import vn.iotstar.exception.InvalidVideoException;

@Service
public class VideoFileStorageService {

    private static final int HEADER_LENGTH = 12;

    private final Path uploadDirectory;
    private final long maximumFileSize;

    public VideoFileStorageService(@Value("${app.video.upload-dir:uploads/videos}") String uploadDirectory,
            @Value("${app.video.max-file-size-bytes:209715200}") long maximumFileSize) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.maximumFileSize = maximumFileSize;
    }

    public String store(MultipartFile file) throws IOException {
        String extension = validate(file);
        Files.createDirectories(uploadDirectory);
        String filename = UUID.randomUUID() + "." + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();
        if (!destination.getParent().equals(uploadDirectory)) {
            throw new InvalidVideoException("Tên tệp video không hợp lệ.");
        }
        try {
            file.transferTo(destination);
        } catch (IOException exception) {
            Files.deleteIfExists(destination);
            throw exception;
        }
        return "/uploads/videos/" + filename;
    }

    public void delete(String videoUrl) throws IOException {
        if (videoUrl == null || !videoUrl.startsWith("/uploads/videos/")) {
            return;
        }
        String filename = videoUrl.substring("/uploads/videos/".length());
        if (!filename.matches("[0-9a-fA-F-]{36}\\.(mp4|webm|ogv)")) {
            return;
        }
        Path file = uploadDirectory.resolve(filename).normalize();
        if (file.getParent().equals(uploadDirectory)) {
            Files.deleteIfExists(file);
        }
    }

    private String validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new InvalidVideoException("Vui lòng chọn tệp video.");
        }
        if (file.getSize() > maximumFileSize) {
            throw new InvalidVideoException("Video vượt quá dung lượng tối đa cho phép.");
        }
        String originalName = file.getOriginalFilename();
        String filename = originalName == null ? "" : originalName.replace('\\', '/');
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new InvalidVideoException("Chỉ chấp nhận định dạng MP4, WebM hoặc Ogg.");
        }
        String extension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!java.util.Set.of("mp4", "webm", "ogv").contains(extension)) {
            throw new InvalidVideoException("Chỉ chấp nhận định dạng MP4, WebM hoặc Ogg.");
        }

        byte[] header;
        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(HEADER_LENGTH);
        }
        int bytesRead = header.length;
        boolean validHeader = switch (extension) {
            case "mp4" -> bytesRead >= 8 && header[4] == 'f' && header[5] == 't'
                    && header[6] == 'y' && header[7] == 'p';
            case "webm" -> bytesRead >= 4 && (header[0] & 0xFF) == 0x1A
                    && (header[1] & 0xFF) == 0x45 && (header[2] & 0xFF) == 0xDF
                    && (header[3] & 0xFF) == 0xA3;
            case "ogv" -> bytesRead >= 4 && header[0] == 'O' && header[1] == 'g'
                    && header[2] == 'g' && header[3] == 'S';
            default -> false;
        };
        if (!validHeader) {
            throw new InvalidVideoException("Nội dung tệp không khớp với định dạng video được hỗ trợ.");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equals(contentType)
                && !contentType.equals("video/" + (extension.equals("ogv") ? "ogg" : extension))) {
            throw new InvalidVideoException("Loại nội dung của tệp video không hợp lệ.");
        }
        return extension;
    }
}
