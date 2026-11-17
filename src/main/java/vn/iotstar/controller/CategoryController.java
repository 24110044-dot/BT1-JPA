package vn.iotstar.controller;

import java.io.IOException;
import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.iotstar.config.ImageStorageService;
import vn.iotstar.entity.Category;
import vn.iotstar.exception.CategoryHasVideosException;
import vn.iotstar.exception.CategoryNotFoundException;
import vn.iotstar.exception.DuplicateCategoryNameException;
import vn.iotstar.exception.InvalidImageException;
import vn.iotstar.service.ICategoryService;

@Controller
public class CategoryController {

    private static final String LIST_VIEW = "admin/category/category-list";
    private static final String ADD_VIEW = "admin/category/category-add";
    private static final String EDIT_VIEW = "admin/category/category-edit";
    private static final int PAGE_SIZE = 10;

    private final ICategoryService categoryService;
    private final ImageStorageService imageStorageService;

    public CategoryController(ICategoryService categoryService, ImageStorageService imageStorageService) {
        this.categoryService = categoryService;
        this.imageStorageService = imageStorageService;
    }

    @InitBinder("category")
    void restrictCategoryBinding(WebDataBinder binder) {
        binder.setAllowedFields("cateName");
    }

    @GetMapping({ "/admin/category/list", "/admin/categories" })
    public String adminList(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword, Model model) {
        return list(page, keyword, model, LIST_VIEW);
    }

    @GetMapping("/manager/category/list")
    public String managerList(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword, Model model) {
        return list(page, keyword, model, "manager/category/category-list");
    }

    @GetMapping("/manager/category/detail/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Category category = categoryService.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        model.addAttribute("category", category);
        return "manager/category/category-detail";
    }

    @GetMapping("/admin/category/add")
    public String addForm(Model model) {
        model.addAttribute("category", new Category());
        return ADD_VIEW;
    }

    @PostMapping("/admin/category/add")
    public String add(@Valid @ModelAttribute("category") Category category, BindingResult bindingResult,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) throws IOException {
        if (bindingResult.hasErrors()) {
            return ADD_VIEW;
        }
        String storedIcon = null;
        try {
            category.setCateId(null);
            if (imageFile != null && !imageFile.isEmpty()) {
                category.setIcon(imageStorageService.store(imageFile));
                storedIcon = category.getIcon();
            } else if (category.getIcon() == null || category.getIcon().isBlank()) {
                category.setIcon("/images/avatar.png");
            }
            categoryService.insert(category);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục thành công.");
            return "redirect:/admin/category/list";
        } catch (DuplicateCategoryNameException | InvalidImageException exception) {
            if (storedIcon != null) {
                imageStorageService.deleteManagedImage(storedIcon);
            }
            bindingResult.rejectValue(exception instanceof DuplicateCategoryNameException ? "cateName" : "icon",
                    "category.invalid", exception.getMessage());
            return ADD_VIEW;
        }
    }

    @GetMapping("/admin/category/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        model.addAttribute("category", categoryService.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id)));
        return EDIT_VIEW;
    }

    @PostMapping("/admin/category/edit/{id}")
    public String edit(@PathVariable Integer id, @Valid @ModelAttribute("category") Category category,
            BindingResult bindingResult, @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes, Model model) throws IOException {
        Category current = categoryService.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        if (bindingResult.hasErrors()) {
            category.setCateId(id);
            if ((category.getIcon() == null || category.getIcon().isBlank())
                    && (imageFile == null || imageFile.isEmpty())) {
                category.setIcon(current.getIcon());
            }
            model.addAttribute("currentIcon", current.getIcon());
            return EDIT_VIEW;
        }
        String newIcon = null;
        try {
            category.setCateId(id);
            if (imageFile != null && !imageFile.isEmpty()) {
                category.setIcon(imageStorageService.store(imageFile));
                newIcon = category.getIcon();
            } else if (category.getIcon() == null || category.getIcon().isBlank()) {
                category.setIcon(current.getIcon());
            }
            categoryService.update(category);
            if (newIcon != null && current.getIcon() != null && !current.getIcon().equals(newIcon)) {
                imageStorageService.deleteManagedImage(current.getIcon());
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công.");
            return "redirect:/admin/category/list";
        } catch (DuplicateCategoryNameException | InvalidImageException exception) {
            if (newIcon != null) {
                imageStorageService.deleteManagedImage(newIcon);
            }
            bindingResult.rejectValue(exception instanceof DuplicateCategoryNameException ? "cateName" : "icon",
                    "category.invalid", exception.getMessage());
            model.addAttribute("currentIcon", current.getIcon());
            return EDIT_VIEW;
        }
    }

    @PostMapping("/admin/category/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục thành công.");
        } catch (CategoryHasVideosException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/category/list";
    }

    private String list(int page, String keyword, Model model, String view) {
        String query = keyword == null ? "" : keyword.trim();
        Page<Category> categories = query.isEmpty() ? categoryService.findAll(page, PAGE_SIZE)
                : categoryService.searchByName(query, page, PAGE_SIZE);
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", query);
        model.addAttribute("totalCategories", categoryService.count());
        return view;
    }
}
