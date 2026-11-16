package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @Column(name = "VideoId", length = 50)
    private String videoId;

    @Column(name = "Active")
    private Boolean active;

    @Column(name = "Description", length = 4000)
    private String description;

    @Column(name = "Poster", length = 500)
    private String poster;

    @Column(name = "Title")
    private String title;

    @Column(name = "Views")
    private Integer views;

    @Column(name = "VideoUrl", length = 500)
    private String videoUrl;

    @Column(name = "UploadedAt", nullable = false)
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryId")
    private Category category;

    public Video() {
    }

    public Video(String videoId, Boolean active, String description, String poster, String title, Integer views,
            Category category) {
        this.videoId = videoId;
        this.active = active;
        this.description = description;
        this.poster = poster;
        this.title = title;
        this.views = views;
        setCategory(category);
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public User getUser() { return user; }
    public void setUser(User user) {
        if (this.user == user) {
            return;
        }
        User previousUser = this.user;
        this.user = user;
        if (previousUser != null && previousUser.getVideos().contains(this)) {
            previousUser.getVideos().remove(this);
        }
        if (user != null && !user.getVideos().contains(this)) {
            user.getVideos().add(this);
        }
    }

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (views == null) {
            views = 0;
        }
        if (active == null) {
            active = true;
        }
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        if (this.category == category) {
            return;
        }

        Category previousCategory = this.category;
        this.category = category;
        if (previousCategory != null && previousCategory.getVideos().contains(this)) {
            previousCategory.getVideos().remove(this);
        }
        if (category != null && !category.getVideos().contains(this)) {
            category.getVideos().add(this);
        }
    }
}
