package vn.iotstar.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VideoForm {

    @NotBlank(message = "Mã video không được để trống.")
    @Size(max = 50, message = "Mã video tối đa 50 ký tự.")
    private String videoId;

    @NotBlank(message = "Tiêu đề không được để trống.")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự.")
    private String title;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự.")
    private String description;

    @Size(max = 500, message = "Đường dẫn poster tối đa 500 ký tự.")
    private String poster;

    @Min(value = 0, message = "Lượt xem không được âm.")
    private Integer views = 0;

    private Integer categoryId;

    private Boolean active = true;

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPoster() { return poster; }
    public void setPoster(String poster) { this.poster = poster; }
    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
