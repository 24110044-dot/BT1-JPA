package vn.iotstar.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import vn.iotstar.dto.VideoForm;
import vn.iotstar.entity.Video;

public interface IVideoService {

    Page<Video> searchAdmin(String keyword, Integer categoryId, Boolean active, int page, int pageSize);

    Page<Video> browseActive(Integer categoryId, int page, int pageSize);

    List<Video> featuredActive();

    Optional<Video> findActiveById(String videoId);

    Optional<Video> recordViewAndFindActiveById(String videoId);

    Video getById(String videoId);

    Video create(VideoForm form);

    Video update(String videoId, VideoForm form);

    void hide(String videoId);

    long count();
}
