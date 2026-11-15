package vn.iotstar.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;
import vn.iotstar.dto.VideoForm;
import vn.iotstar.entity.Category;
import vn.iotstar.entity.Video;
import vn.iotstar.service.ICategoryService;
import vn.iotstar.service.IVideoService;

@Controller
public class VideoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoController.class);
    private static final int PAGE_SIZE = 12;

    private final IVideoService videoService;
    private final ICategoryService categoryService;

    public VideoController(IVideoService videoService, ICategoryService categoryService) {
        this.videoService = videoService;
        this.categoryService = categoryService;
    }

    @InitBinder("form")
    void restrictFormBinding(WebDataBinder binder) {
        binder.setAllowedFields("videoId", "title", "description", "poster", "views", "categoryId", "active");
    }

    @GetMapping("/admin/videos")
    public String list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page, Model model) {
        String query = keyword == null ? "" : keyword.trim();
        model.addAttribute("videos", videoService.searchAdmin(query, categoryId, active, page, PAGE_SIZE));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("keyword", query);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedActive", active);
        model.addAttribute("totalVideos", videoService.count());
        return "admin/videos/video-list";
    }

    @GetMapping("/videos")
    public String browse(@RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("videos", videoService.browseActive(categoryId, page, PAGE_SIZE));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("pageTitle", categoryId == null ? "Tất cả video"
                : categoryService.findById(categoryId)
                        .map(Category::getCateName)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "shop/video-list";
    }

    @GetMapping("/videos/detail")
    public String detail(@RequestParam String id, Model model) {
        Video video = videoService.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("video", video);
        model.addAttribute("categories", categoryService.findAll());
        return "shop/video-detail";
    }

    @GetMapping("/admin/videos/add")
    public String addForm(Model model) {
        prepareForm(model, new VideoForm(), true);
        return "admin/videos/video-form";
    }

    @PostMapping("/admin/videos/add")
    public String add(@Valid @ModelAttribute("form") VideoForm form, BindingResult result,
            Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            prepareForm(model, form, true);
            return "admin/videos/video-form";
        }
        try {
            videoService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm video thành công.");
            return "redirect:/admin/videos";
        } catch (IllegalArgumentException exception) {
            return showFormError(model, form, true, null, exception.getMessage());
        } catch (DataAccessException exception) {
            LOGGER.error("Database failed while creating video {}.", form.getVideoId(), exception);
            return showFormError(model, form, true, null,
                    "Không lưu được video vào cơ sở dữ liệu. Kiểm tra bảng videos và xem log ứng dụng để biết chi tiết.");
        }
    }

    @GetMapping("/admin/videos/edit")
    public String editForm(@RequestParam String id, Model model) {
        Video video = videoService.getById(id);
        prepareForm(model, toForm(video), false);
        model.addAttribute("videoId", id);
        return "admin/videos/video-form";
    }

    @PostMapping("/admin/videos/edit")
    public String edit(@RequestParam String id, @Valid @ModelAttribute("form") VideoForm form,
            BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        Video current = videoService.getById(id);
        if (result.hasErrors()) {
            prepareForm(model, form, false);
            model.addAttribute("videoId", id);
            return "admin/videos/video-form";
        }
        try {
            videoService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật video thành công.");
            return "redirect:/admin/videos";
        } catch (IllegalArgumentException exception) {
            return showFormError(model, form, false, current, exception.getMessage(), id);
        } catch (DataAccessException exception) {
            LOGGER.error("Database failed while updating video {}.", id, exception);
            return showFormError(model, form, false, current,
                    "Không cập nhật được video trong cơ sở dữ liệu. Kiểm tra bảng videos và xem log ứng dụng để biết chi tiết.",
                    id);
        }
    }

    @PostMapping("/admin/videos/hide")
    public String hide(@RequestParam String id, RedirectAttributes redirectAttributes) {
        videoService.hide(id);
        redirectAttributes.addFlashAttribute("successMessage", "Video đã được ẩn.");
        return "redirect:/admin/videos";
    }

    private void prepareForm(Model model, VideoForm form, boolean isNew) {
        model.addAttribute("form", form);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isNew", isNew);
    }

    private String showFormError(Model model, VideoForm form, boolean isNew, Video current, String message) {
        model.addAttribute("errorMessage", message);
        prepareForm(model, form, isNew);
        if (current != null) {
            model.addAttribute("videoId", current.getVideoId());
        }
        return "admin/videos/video-form";
    }

    private String showFormError(Model model, VideoForm form, boolean isNew, Video current, String message,
            String videoId) {
        String view = showFormError(model, form, isNew, current, message);
        model.addAttribute("videoId", videoId);
        return view;
    }

    private VideoForm toForm(Video video) {
        VideoForm form = new VideoForm();
        form.setVideoId(video.getVideoId());
        form.setTitle(video.getTitle());
        form.setDescription(video.getDescription());
        form.setPoster(video.getPoster());
        form.setViews(video.getViews());
        Category category = video.getCategory();
        form.setCategoryId(category == null ? null : category.getCateId());
        form.setActive(video.getActive());
        return form;
    }
}
