package vn.iotstar.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path uploadDirectory;
    private final Path videoUploadDirectory;

    public WebConfig(@Value("${app.upload.dir:uploads/categories}") String uploadDirectory,
            @Value("${app.video.upload-dir:uploads/videos}") String videoUploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.videoUploadDirectory = Path.of(videoUploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = uploadDirectory.getParent();
        registry.addResourceHandler("/uploads/**").addResourceLocations(directoryLocation(uploadRoot));
        registry.addResourceHandler("/uploads/videos/**")
                .addResourceLocations(directoryLocation(videoUploadDirectory));
    }

    private String directoryLocation(Path directory) {
        String location = directory.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
