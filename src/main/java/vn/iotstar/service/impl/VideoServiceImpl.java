package vn.iotstar.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.dto.VideoForm;
import vn.iotstar.entity.Category;
import vn.iotstar.entity.Video;
import vn.iotstar.exception.VideoNotFoundException;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.VideoRepository;
import vn.iotstar.service.IVideoService;

@Service
@Transactional
public class VideoServiceImpl implements IVideoService {

    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;

    public VideoServiceImpl(VideoRepository videoRepository, CategoryRepository categoryRepository) {
        this.videoRepository = videoRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Video> searchAdmin(String keyword, Integer categoryId, Boolean active, int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Tham số phân trang không hợp lệ.");
        }
        String query = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return videoRepository.searchAdmin(query, categoryId, active,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "title")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Video> browseActive(Integer categoryId, int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Tham số phân trang không hợp lệ.");
        }
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "title"));
        return categoryId == null ? videoRepository.findByActiveTrue(pageable)
                : videoRepository.findByActiveTrueAndCategory_CateId(categoryId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Video> featuredActive() {
        return videoRepository.findTop8ByActiveTrueOrderByViewsDescTitleAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Video> findActiveById(String videoId) {
        return videoRepository.findByVideoIdAndActiveTrue(videoId);
    }

    @Override
    @Transactional(readOnly = true)
    public Video getById(String videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));
    }

    @Override
    public Video create(VideoForm form) {
        String videoId = form.getVideoId().trim();
        if (videoRepository.existsById(videoId)) {
            throw new IllegalArgumentException("Mã video đã tồn tại.");
        }
        Video video = new Video();
        video.setVideoId(videoId);
        apply(video, form);
        return videoRepository.save(video);
    }

    @Override
    public Video update(String videoId, VideoForm form) {
        Video current = getById(videoId);
        String updatedVideoId = form.getVideoId().trim();
        if (updatedVideoId.equals(current.getVideoId())) {
            apply(current, form);
            return videoRepository.save(current);
        }
        if (videoRepository.existsById(updatedVideoId)) {
            throw new IllegalArgumentException("Mã video đã được sử dụng.");
        }

        Video updated = new Video();
        updated.setVideoId(updatedVideoId);
        apply(updated, form);
        videoRepository.delete(current);
        videoRepository.flush();
        return videoRepository.save(updated);
    }

    @Override
    public void hide(String videoId) {
        Video video = getById(videoId);
        video.setActive(false);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return videoRepository.count();
    }

    private void apply(Video video, VideoForm form) {
        Category category = form.getCategoryId() == null ? null
                : categoryRepository.findById(form.getCategoryId())
                        .orElseThrow(() -> new IllegalArgumentException("Danh mục được chọn không tồn tại."));
        video.setTitle(form.getTitle().trim());
        video.setDescription(form.getDescription());
        video.setPoster(form.getPoster() == null || form.getPoster().isBlank() ? null : form.getPoster().trim());
        video.setViews(form.getViews() == null ? 0 : form.getViews());
        video.setActive(form.getActive() == null || form.getActive());
        video.setCategory(category);
    }
}
