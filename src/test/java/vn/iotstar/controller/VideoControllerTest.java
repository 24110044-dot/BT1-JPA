package vn.iotstar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.iotstar.config.GlobalModelAdvice;
import vn.iotstar.entity.Category;
import vn.iotstar.entity.Video;
import vn.iotstar.service.ICartService;
import vn.iotstar.service.ICategoryService;
import vn.iotstar.service.IVideoService;

@WebMvcTest(VideoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalModelAdvice.class)
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IVideoService videoService;

    @MockitoBean
    private ICategoryService categoryService;

    @MockitoBean
    private ICartService cartService;

    @Test
    void addVideoFormRenders() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/videos/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/videos/video-form"));
    }

    @Test
    void videoListRendersAndIncludesSearchFilters() throws Exception {
        when(videoService.searchAdmin("abc", 2, false, 0, 12)).thenReturn(Page.empty());
        when(categoryService.findAll()).thenReturn(List.of(new Category(2, "Danh mục", null)));

        mockMvc.perform(get("/admin/videos")
                        .param("keyword", "abc")
                        .param("categoryId", "2")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/videos/video-list"));
    }

    @Test
    void publicVideoListShowsActiveVideosForCategory() throws Exception {
        when(videoService.browseActive(2, 0, 12)).thenReturn(Page.empty());
        when(categoryService.findAll()).thenReturn(List.of(new Category(2, "Danh mục", null)));
        when(categoryService.findById(2)).thenReturn(java.util.Optional.of(new Category(2, "Danh mục", null)));

        mockMvc.perform(get("/videos").param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/video-list"));
    }

    @Test
    void publicVideoDetailUsesQueryParameter() throws Exception {
        Video video = new Video();
        video.setVideoId("youtube_id");
        video.setTitle("Video");
        when(videoService.findActiveById("youtube_id")).thenReturn(java.util.Optional.of(video));
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/videos/detail").param("id", "youtube_id"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/video-detail"));
    }

    @Test
    void editVideoUsesQueryParameterAndShowsForm() throws Exception {
        Video video = new Video();
        video.setVideoId("video/id");
        video.setTitle("Video");
        when(videoService.getById("video/id")).thenReturn(video);
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/videos/edit").param("id", "video/id"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/videos/video-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"videoId\"")));
    }

    @Test
    void editPostSubmitsImmutableVideoIdAndUpdatesRecord() throws Exception {
        Video video = new Video();
        video.setVideoId("video/id");
        video.setTitle("Video cũ");
        when(videoService.getById("video/id")).thenReturn(video);

        mockMvc.perform(post("/admin/videos/edit")
                        .param("id", "video/id")
                        .param("videoId", "video/id")
                        .param("title", "Video mới")
                        .param("views", "0")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/videos"));

        verify(videoService).update(org.mockito.ArgumentMatchers.eq("video/id"), any());
    }

    @Test
    void validAddFormCreatesVideoAndRedirects() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(new Category(1, "Danh mục", null)));

        mockMvc.perform(post("/admin/videos/add")
                        .param("videoId", "video-1")
                        .param("title", "Video mới")
                        .param("views", "0")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/videos"));

        verify(videoService).create(any());
    }
}
