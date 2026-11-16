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

import vn.iotstar.dto.UserVideoForm;
import vn.iotstar.entity.User;
import vn.iotstar.entity.Video;
import vn.iotstar.exception.UserNotFoundException;
import vn.iotstar.exception.VideoNotFoundException;
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

    public UserVideoService(VideoRepository videoRepository, UserRepository userRepository,
            VideoFileStorageService storageService) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Page<Video> listForUser(Integer userId, int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Trang không hợp lệ.");
        }
        return videoRepository.findByUser_IdOrderByUploadedAtDesc(userId, PageRequest.of(page, PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Video getPublished(String videoId) {
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
        try {
            Video video = new Video();
            video.setVideoId(UUID.randomUUID().toString());
            video.setUser(user);
            video.setVideoUrl(storedUrl);
            video.setTitle(form.getTitle().trim());
            video.setDescription(normalizeDescription(form.getDescription()));
            video.setPoster(normalizePoster(form.getPoster()));
            video.setActive(true);
            video.setViews(0);
            Video saved = videoRepository.saveAndFlush(video);
            scheduleFileCleanup(storedUrl, null);
            return saved;
        } catch (RuntimeException exception) {
            deleteStoredFile(storedUrl);
            throw exception;
        }
    }

    public Video update(String videoId, UserVideoForm form, Integer userId, Integer roleId) throws IOException {
        Video video = getForOwner(videoId, userId, roleId);
        String previousUrl = video.getVideoUrl();
        String replacementUrl = null;
        if (form.getFile() != null && !form.getFile().isEmpty()) {
            replacementUrl = storageService.store(form.getFile());
        }
        try {
            video.setTitle(form.getTitle().trim());
            video.setDescription(normalizeDescription(form.getDescription()));
            if (form.getPoster() != null && !form.getPoster().isBlank()) {
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
            return saved;
        } catch (RuntimeException exception) {
            if (replacementUrl != null) {
                deleteStoredFile(replacementUrl);
            }
            throw exception;
        }
    }

    public void delete(String videoId, Integer userId, Integer roleId) throws IOException {
        Video video = getForOwner(videoId, userId, roleId);
        String videoUrl = video.getVideoUrl();
        videoRepository.delete(video);
        videoRepository.flush();
        if (videoUrl != null && !videoRepository.existsByVideoUrl(videoUrl)) {
            scheduleFileCleanup(null, videoUrl);
        }
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private String normalizePoster(String poster) {
        return poster == null || poster.isBlank() ? null : poster.trim();
    }

    private boolean isAdmin(Integer roleId) {
        return Integer.valueOf(1).equals(roleId);
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
}
