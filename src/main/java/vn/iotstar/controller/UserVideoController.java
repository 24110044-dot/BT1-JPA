package vn.iotstar.controller;

import java.io.IOException;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.iotstar.dto.UserVideoForm;
import vn.iotstar.dto.VideoCategorySuggestion;
import vn.iotstar.exception.InvalidImageException;
import vn.iotstar.entity.Video;
import vn.iotstar.exception.InvalidVideoException;
import vn.iotstar.exception.VideoNotFoundException;
import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.service.ICategoryService;
import vn.iotstar.service.VideoCategorySuggestionService;
import vn.iotstar.service.UserVideoService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
public class UserVideoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserVideoController.class);
    private final UserVideoService userVideoService;
    private final ICategoryService categoryService;
    private final VideoCategorySuggestionService videoCategorySuggestionService;

    public UserVideoController(UserVideoService userVideoService, ICategoryService categoryService,
            VideoCategorySuggestionService videoCategorySuggestionService) {
        this.userVideoService = userVideoService;
        this.categoryService = categoryService;
        this.videoCategorySuggestionService = videoCategorySuggestionService;
    }

    @InitBinder("form")
    void restrictFormBinding(WebDataBinder binder) {
        binder.setAllowedFields("title", "description", "poster", "categoryId", "file", "thumbnailFile");
    }

    @GetMapping("/video")
    public String list(@RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        Page<Video> videos = userVideoService.listForUser(currentUser.getId(), page);
        model.addAttribute("videos", videos);
        return "video/list";
    }

    @GetMapping("/video/add")
    public String addForm(Model model) {
        model.addAttribute("form", new UserVideoForm());
        model.addAttribute("isEdit", false);
        addCategories(model);
        return "video/form";
    }

    @GetMapping("/video/suggest-category")
    @ResponseBody
    public VideoCategorySuggestion suggestCategory(@RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {
        return videoCategorySuggestionService.suggest(title, description)
                .map(category -> new VideoCategorySuggestion(category.getCateId(), category.getCateName(),
                        "Gợi ý từ tiêu đề/mô tả và sản phẩm. Hãy xác nhận hoặc đổi danh mục trước khi tải lên."))
                .orElseGet(() -> new VideoCategorySuggestion(null, null,
                        "Chưa tìm được danh mục phù hợp rõ ràng. Vui lòng tự chọn danh mục."));
    }

    @PostMapping("/video/add")
    public String add(@Valid @ModelAttribute("form") UserVideoForm form, BindingResult result,
            @AuthenticationPrincipal CustomUserDetails currentUser, Model model,
            RedirectAttributes redirectAttributes) {
        if (form.getFile() == null || form.getFile().isEmpty()) {
            result.rejectValue("file", "video.required", "Vui lòng chọn tệp video.");
        }
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            addCategories(model);
            return "video/form";
        }
        try {
            userVideoService.create(form, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã tải video lên thành công.");
            return "redirect:/video";
        } catch (InvalidImageException | InvalidVideoException | IllegalArgumentException exception) {
            model.addAttribute("isEdit", false);
            model.addAttribute("errorMessage", exception.getMessage());
            addCategories(model);
            return "video/form";
        } catch (IOException exception) {
            LOGGER.error("Unable to store uploaded video for user {}.", currentUser.getId(), exception);
            model.addAttribute("isEdit", false);
            model.addAttribute("errorMessage", "Không thể lưu tệp video. Kiểm tra thư mục upload và thử lại.");
            addCategories(model);
            return "video/form";
        }
    }

    @GetMapping("/video/edit")
    public String editForm(@RequestParam String id, @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model) {
        try {
            Video video = userVideoService.getForOwner(id, currentUser.getId(), currentUser.getRoleId());
            model.addAttribute("form", toForm(video));
            model.addAttribute("videoId", id);
            addCurrentMedia(model, video);
            model.addAttribute("isEdit", true);
            addCategories(model);
            return "video/form";
        } catch (VideoNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/video/edit")
    public String edit(@RequestParam String id, @Valid @ModelAttribute("form") UserVideoForm form,
            BindingResult result, @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model, RedirectAttributes redirectAttributes) {
        Video current;
        try {
            current = userVideoService.getForOwner(id, currentUser.getId(), currentUser.getRoleId());
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (VideoNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (result.hasErrors()) {
            model.addAttribute("videoId", id);
            addCurrentMedia(model, current);
            model.addAttribute("isEdit", true);
            addCategories(model);
            return "video/form";
        }
        try {
            userVideoService.update(id, form, currentUser.getId(), currentUser.getRoleId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật video.");
            return "redirect:/video";
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (VideoNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (InvalidImageException | InvalidVideoException | IllegalArgumentException exception) {
            model.addAttribute("videoId", id);
            addCurrentMedia(model, current);
            model.addAttribute("isEdit", true);
            model.addAttribute("errorMessage", exception.getMessage());
            addCategories(model);
            return "video/form";
        } catch (IOException exception) {
            LOGGER.error("Unable to replace uploaded video {}.", id, exception);
            model.addAttribute("videoId", id);
            addCurrentMedia(model, current);
            model.addAttribute("isEdit", true);
            model.addAttribute("errorMessage", "Không thể lưu tệp video. Video cũ vẫn được giữ lại.");
            addCategories(model);
            return "video/form";
        }
    }

    @PostMapping("/video/delete")
    public String delete(@RequestParam String id, @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            userVideoService.delete(id, currentUser.getId(), currentUser.getRoleId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa video.");
            return "redirect:/video";
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (VideoNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IOException exception) {
            LOGGER.error("Unable to remove stored file for video {}.", id, exception);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Đã xóa dữ liệu video nhưng không thể xóa tệp vật lý. Hãy kiểm tra thư mục upload.");
            return "redirect:/video";
        }
    }

    @PostMapping("/video/thumbnail/delete")
    public String removeThumbnail(@RequestParam String id,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            userVideoService.removeThumbnail(id, currentUser.getId(), currentUser.getRoleId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã gỡ thumbnail khỏi video.");
            return "redirect:/video/edit?id=" + java.net.URLEncoder.encode(
                    id, java.nio.charset.StandardCharsets.UTF_8);
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (VideoNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/video/{id}")
    public String detail(@PathVariable String id, Model model) {
        try {
            model.addAttribute("video", userVideoService.recordViewAndGetPublished(id));
            model.addAttribute("categories", categoryService.findAll());
            return "shop/video-detail";
        } catch (VideoNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private UserVideoForm toForm(Video video) {
        UserVideoForm form = new UserVideoForm();
        form.setTitle(video.getTitle());
        form.setDescription(video.getDescription());
        form.setPoster(video.getPoster());
        form.setCategoryId(video.getCategory() == null ? null : video.getCategory().getCateId());
        return form;
    }

    private void addCategories(Model model) {
        model.addAttribute("categories", categoryService.findAll());
    }

    private void addCurrentMedia(Model model, Video video) {
        model.addAttribute("currentVideoUrl", video.getVideoUrl());
        model.addAttribute("currentPoster", video.getPoster());
    }
}
