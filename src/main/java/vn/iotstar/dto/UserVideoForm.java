package vn.iotstar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.multipart.MultipartFile;

public class UserVideoForm {

    @NotBlank(message = "Vui lòng nhập tiêu đề video.")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự.")
    private String title;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự.")
    private String description;

    @Size(max = 500, message = "Đường dẫn thumbnail tối đa 500 ký tự.")
    private String poster;

    private MultipartFile file;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPoster() { return poster; }
    public void setPoster(String poster) { this.poster = poster; }
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
}
