package vn.iotstar.service;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import vn.iotstar.config.ImageStorageService;
import vn.iotstar.dto.UserVideoForm;
import vn.iotstar.entity.Category;
import vn.iotstar.entity.User;
import vn.iotstar.entity.Video;
import vn.iotstar.exception.UserNotFoundException;
import vn.iotstar.exception.InvalidImageException;
import vn.iotstar.exception.VideoNotFoundException;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.VideoRepository;

@Service
@Transactional
public class UserVideoService {

    private static final int PAGE_SIZE = 6;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserVideoService.class);

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final VideoFileStorageService storageService;
    private final ImageStorageService imageStorageService;
    private final CategoryRepository categoryRepository;

    public UserVideoService(VideoRepository videoRepository, UserRepository userRepository,
            VideoFileStorageService storageService, ImageStorageService imageStorageService,
            CategoryRepository categoryRepository) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.imageStorageService = imageStorageService;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<Video> listForUser(Integer userId, int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Trang không hợp lệ.");
        }
        return videoRepository.findByUser_IdOrderByUploadedAtDesc(userId, PageRequest.of(page, PAGE_SIZE));
    }

    public Video recordViewAndGetPublished(String videoId) {
        if (videoRepository.incrementViewsForActiveVideo(videoId) == 0) {
            throw new VideoNotFoundException(videoId);
        }
        return videoRepository.findByVideoIdAndActiveTrue(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));
    }

    @Transactional(readOnly = true)
    public Video getForOwner(String videoId, Integer userId, Integer roleId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));
        if (!isAdmin(roleId) && (video.getUser() == null || !userId.equals(video.getUser().getId()))) {
            throw new AccessDeniedException("Bạn không có quyền quản lý video này.");
        }
        return video;
    }

    public Video create(UserVideoForm form, Integer userId) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        String storedUrl = storageService.store(form.getFile());
        String storedThumbnail = null;
        try {
            if (form.getThumbnailFile() != null && !form.getThumbnailFile().isEmpty()) {
                storedThumbnail = imageStorageService.store(form.getThumbnailFile(), "video-thumbnails");
            }
            Video video = new Video();
            video.setVideoId(UUID.randomUUID().toString());
            video.setUser(user);
            video.setVideoUrl(storedUrl);
            video.setTitle(form.getTitle().trim());
            video.setDescription(normalizeDescription(form.getDescription()));
            video.setCategory(resolveCategory(form.getCategoryId()));
            video.setPoster(storedThumbnail != null ? storedThumbnail : normalizePoster(form.getPoster()));
            video.setActive(true);
            video.setViews(0);
            Video saved = videoRepository.saveAndFlush(video);
            scheduleFileCleanup(storedUrl, null);
            if (storedThumbnail != null) {
                scheduleImageCleanup(storedThumbnail, null);
            }
            return saved;
        } catch (IOException | RuntimeException exception) {
            deleteStoredFile(storedUrl);
            deleteStoredImage(storedThumbnail);
            throw exception;
        }
    }

    public Video update(String videoId, UserVideoForm form, Integer userId, Integer roleId) throws IOException {
        Video video = getForOwner(videoId, userId, roleId);
        String previousUrl = video.getVideoUrl();
        String previousPoster = video.getPoster();
        String replacementUrl = null;
        String replacementThumbnail = null;
        try {
            if (form.getFile() != null && !form.getFile().isEmpty()) {
                replacementUrl = storageService.store(form.getFile());
            }
            if (form.getThumbnailFile() != null && !form.getThumbnailFile().isEmpty()) {
                replacementThumbnail = imageStorageService.store(form.getThumbnailFile(), "video-thumbnails");
            }
        } catch (IOException | RuntimeException exception) {
            deleteStoredFile(replacementUrl);
            deleteStoredImage(replacementThumbnail);
            throw exception;
        }
        try {
            video.setTitle(form.getTitle().trim());
            video.setDescription(normalizeDescription(form.getDescription()));
            video.setCategory(resolveCategory(form.getCategoryId()));
            if (replacementThumbnail != null) {
                video.setPoster(replacementThumbnail);
            } else if (form.getPoster() != null && !form.getPoster().isBlank()) {
                video.setPoster(normalizePoster(form.getPoster()));
            }
            if (replacementUrl != null) {
                video.setVideoUrl(replacementUrl);
            }
            Video saved = videoRepository.saveAndFlush(video);
            if (replacementUrl != null) {
                String obsoleteUrl = previousUrl != null && !previousUrl.equals(replacementUrl)
                        && !videoRepository.existsByVideoUrl(previousUrl) ? previousUrl : null;
                scheduleFileCleanup(replacementUrl, obsoleteUrl);
            }
            if (replacementThumbnail != null || isPosterChanged(previousPoster, form.getPoster())) {
                String obsoletePoster = previousPoster != null && !previousPoster.equals(video.getPoster())
                        && !videoRepository.existsByPoster(previousPoster) ? previousPoster : null;
                scheduleImageCleanup(replacementThumbnail, obsoletePoster);
            }
            return saved;
        } catch (RuntimeException exception) {
            if (replacementUrl != null) {
                deleteStoredFile(replacementUrl);
            }
            deleteStoredImage(replacementThumbnail);
            throw exception;
        }
    }

    public void delete(String videoId, Integer userId, Integer roleId) throws IOException {
        Video video = getForOwner(videoId, userId, roleId);
        String videoUrl = video.getVideoUrl();
        String poster = video.getPoster();
        videoRepository.delete(video);
        videoRepository.flush();
        if (videoUrl != null && !videoRepository.existsByVideoUrl(videoUrl)) {
            scheduleFileCleanup(null, videoUrl);
        }
        if (poster != null && !videoRepository.existsByPoster(poster)) {
            scheduleImageCleanup(null, poster);
        }
    }

    public void removeThumbnail(String videoId, Integer userId, Integer roleId) {
        Video video = getForOwner(videoId, userId, roleId);
        String poster = video.getPoster();
        if (poster == null || poster.isBlank()) {
            return;
        }
        video.setPoster(null);
        videoRepository.saveAndFlush(video);
        if (!videoRepository.existsByPoster(poster)) {
            scheduleImageCleanup(null, poster);
        }
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private Category resolveCategory(Integer categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Vui lòng chọn danh mục video.");
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục được chọn không tồn tại."));
    }

    private String normalizePoster(String poster) {
        return poster == null || poster.isBlank() ? null : poster.trim();
    }

    private boolean isAdmin(Integer roleId) {
        return Integer.valueOf(1).equals(roleId);
    }

    private boolean isPosterChanged(String previousPoster, String formPoster) {
        return formPoster != null && !formPoster.isBlank() && !formPoster.trim().equals(previousPoster);
    }

    private void scheduleFileCleanup(String newFileToDeleteOnRollback, String oldFileToDeleteOnCommit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (oldFileToDeleteOnCommit != null) {
                deleteStoredFile(oldFileToDeleteOnCommit);
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String fileToDelete = status == TransactionSynchronization.STATUS_COMMITTED
                        ? oldFileToDeleteOnCommit : newFileToDeleteOnRollback;
                if (fileToDelete != null) {
                    deleteStoredFile(fileToDelete);
                }
            }
        });
    }

    private void deleteStoredFile(String videoUrl) {
        try {
            storageService.delete(videoUrl);
        } catch (IOException exception) {
            LOGGER.error("Unable to clean up managed video file {}.", videoUrl, exception);
        }
    }

    private void scheduleImageCleanup(String newFileToDeleteOnRollback, String oldFileToDeleteOnCommit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (oldFileToDeleteOnCommit != null) {
                deleteStoredImage(oldFileToDeleteOnCommit);
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String fileToDelete = status == TransactionSynchronization.STATUS_COMMITTED
                        ? oldFileToDeleteOnCommit : newFileToDeleteOnRollback;
                if (fileToDelete != null) {
                    deleteStoredImage(fileToDelete);
                }
            }
        });
    }

    private void deleteStoredImage(String imageUrl) {
        try {
            imageStorageService.deleteManagedImage(imageUrl);
        } catch (IOException exception) {
            LOGGER.error("Unable to clean up managed video thumbnail {}.", imageUrl, exception);
        }
    }
}
