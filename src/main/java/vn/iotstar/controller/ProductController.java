package vn.iotstar.controller;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.iotstar.config.ImageStorageService;
import vn.iotstar.dto.ProductForm;
import vn.iotstar.entity.Category;
import vn.iotstar.entity.Product;
import vn.iotstar.exception.CategoryNotFoundException;
import vn.iotstar.exception.InvalidImageException;
import vn.iotstar.exception.ProductNotFoundException;
import vn.iotstar.service.ICategoryService;
import vn.iotstar.service.IProductService;
import vn.iotstar.service.IVideoService;

@Controller
public class ProductController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);
    private static final int PAGE_SIZE = 12;

    private final IProductService productService;
    private final ICategoryService categoryService;
    private final ImageStorageService imageStorageService;
    private final IVideoService videoService;

    public ProductController(IProductService productService, ICategoryService categoryService,
            ImageStorageService imageStorageService, IVideoService videoService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.imageStorageService = imageStorageService;
        this.videoService = videoService;
    }

    @GetMapping("/products")
    public String products(@RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page, Model model) {
        Page<Product> products = productService.browse(categoryId, keyword, sort, page, PAGE_SIZE);
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("sort", sort);
        model.addAttribute("pageTitle", "Tất cả sản phẩm");
        return "shop/product-list";
    }

    @GetMapping("/products/category/{categoryId}")
    public String categoryProducts(@PathVariable Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page, Model model) {
        Category category = categoryService.findById(categoryId)
                .orElseThrow(() -> new ProductNotFoundException(categoryId));
        Page<Product> products = productService.browse(categoryId, keyword, sort, page, PAGE_SIZE);
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("sort", sort);
        model.addAttribute("pageTitle", category.getCateName());
        model.addAttribute("relatedVideos", videoService.browseActive(categoryId, 0, 8).getContent());
        return "shop/product-list";
    }

    @GetMapping("/products/detail/{productId}")
    public String detail(@PathVariable Integer productId, Model model) {
        Product product = productService.findActiveById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        model.addAttribute("product", product);
        return "shop/product-detail";
    }

    @GetMapping("/manager/products")
    public String managerProducts(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("products", productService.browse(null, null, "newest", page, PAGE_SIZE));
        return "manager/product-list";
    }

    @GetMapping("/admin/products")
    public String adminProducts(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("products", productService.searchAdmin(keyword, categoryId, page, PAGE_SIZE));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("selectedCategoryId", categoryId);
        return "admin/products/product-list";
    }

    @GetMapping("/admin/products/add")
    public String addForm(Model model) {
        prepareForm(model, new ProductForm(), true, null);
        return "admin/products/product-form";
    }

    @PostMapping("/admin/products/add")
    public String add(@Valid @ModelAttribute("form") ProductForm form, BindingResult result,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            Model model, RedirectAttributes redirectAttributes) throws IOException {
        if (result.hasErrors()) {
            prepareForm(model, form, true, null);
            return "admin/products/product-form";
        }
        String stored = null;
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                stored = imageStorageService.store(imageFile, "products");
            }
            productService.create(form, stored);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm thành công.");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException exception) {
            return showFormError(model, form, true, null, null, stored, exception.getMessage());
        } catch (InvalidImageException exception) {
            return showFormError(model, form, true, null, null, stored, exception.getMessage());
        } catch (CategoryNotFoundException exception) {
            return showFormError(model, form, true, null, null, stored, exception.getMessage());
        } catch (DataAccessException exception) {
            LOGGER.error("Database failed while creating a product.", exception);
            return showFormError(model, form, true, null, null, stored,
                    "Không lưu được sản phẩm vào cơ sở dữ liệu. Kiểm tra bảng Products và danh mục đã chọn; xem log ứng dụng để biết chi tiết.");
        } catch (IOException exception) {
            LOGGER.error("Image upload failed while creating a product.", exception);
            return showFormError(model, form, true, null, null, stored,
                    "Không thể lưu ảnh sản phẩm. Kiểm tra quyền ghi thư mục uploads.");
        }
    }

    @GetMapping("/admin/products/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Product product = productService.getById(id);
        ProductForm form = toForm(product);
        prepareForm(model, form, false, product);
        model.addAttribute("productId", id);
        return "admin/products/product-form";
    }

    @PostMapping("/admin/products/edit/{id}")
    public String edit(@PathVariable Integer id, @Valid @ModelAttribute("form") ProductForm form,
            BindingResult result, @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            Model model, RedirectAttributes redirectAttributes) throws IOException {
        Product current = productService.getById(id);
        if (result.hasErrors()) {
            prepareForm(model, form, false, current);
            model.addAttribute("productId", id);
            return "admin/products/product-form";
        }
        String stored = null;
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                stored = imageStorageService.store(imageFile, "products");
            }
            productService.update(id, form, stored);
            if (stored != null && current.getImage() != null && !current.getImage().equals(stored)) {
                deletePreviousImage(current.getImage());
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công.");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException exception) {
            return showFormError(model, form, false, current, id, stored, exception.getMessage());
        } catch (InvalidImageException exception) {
            return showFormError(model, form, false, current, id, stored, exception.getMessage());
        } catch (CategoryNotFoundException exception) {
            return showFormError(model, form, false, current, id, stored, exception.getMessage());
        } catch (DataAccessException exception) {
            LOGGER.error("Database failed while updating product {}.", id, exception);
            return showFormError(model, form, false, current, id, stored,
                    "Không cập nhật được sản phẩm trong cơ sở dữ liệu. Kiểm tra bảng Products và danh mục đã chọn; xem log ứng dụng để biết chi tiết.");
        } catch (IOException exception) {
            LOGGER.error("Image upload failed while updating product {}.", id, exception);
            return showFormError(model, form, false, current, id, stored,
                    "Không thể lưu ảnh sản phẩm. Kiểm tra quyền ghi thư mục uploads.");
        }
    }

    @PostMapping("/admin/products/delete/{id}")
    public String hide(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        productService.hide(id);
        redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm đã được ngừng kinh doanh.");
        return "redirect:/admin/products";
    }

    private void prepareForm(Model model, ProductForm form, boolean isNew, Product product) {
        model.addAttribute("form", form);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isNew", isNew);
        if (product != null) {
            model.addAttribute("currentImage", product.getImage());
        }
    }

    private String showFormError(Model model, ProductForm form, boolean isNew, Product current,
            Integer productId, String stored, String message) {
        deleteStoredImageAfterFailure(stored);
        model.addAttribute("errorMessage", message);
        prepareForm(model, form, isNew, current);
        if (productId != null) {
            model.addAttribute("productId", productId);
        }
        return "admin/products/product-form";
    }

    private void deleteStoredImageAfterFailure(String stored) {
        if (stored == null) {
            return;
        }
        try {
            imageStorageService.deleteManagedImage(stored);
        } catch (IOException cleanupException) {
            LOGGER.warn("Could not remove uploaded image {} after product save failed.", stored, cleanupException);
        }
    }

    private void deletePreviousImage(String previousImage) {
        try {
            imageStorageService.deleteManagedImage(previousImage);
        } catch (IOException cleanupException) {
            LOGGER.warn("Could not remove previous product image {} after update.", previousImage, cleanupException);
        }
    }

    private ProductForm toForm(Product product) {
        ProductForm form = new ProductForm();
        form.setProductName(product.getProductName());
        form.setDescription(product.getDescription());
        form.setPrice(product.getPrice());
        form.setQuantity(product.getQuantity());
        form.setCategoryId(product.getCategory().getCateId());
        form.setStatus(product.getStatus());
        return form;
    }
}
