package vn.iotstar.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.mock.web.MockMultipartFile;

import vn.iotstar.config.ImageStorageService;
import vn.iotstar.dto.ProductForm;
import vn.iotstar.entity.Category;
import vn.iotstar.exception.InvalidImageException;
import vn.iotstar.service.ICategoryService;
import vn.iotstar.service.IProductService;
import vn.iotstar.service.IVideoService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private IProductService productService;

    @Mock
    private ICategoryService categoryService;

    @Mock
    private IVideoService videoService;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        when(categoryService.findAll()).thenReturn(List.of(new Category(3, "Điện thoại", null)));
    }

    @Test
    void invalidImageReturnsFormWithActionableValidationMessage() throws Exception {
        ProductForm form = validForm();
        MockMultipartFile file = new MockMultipartFile("imageFile", "invalid.png", "image/png", new byte[] { 1 });
        when(imageStorageService.store(file, "products"))
                .thenThrow(new InvalidImageException("Tệp hình ảnh không hợp lệ."));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = productController.add(form, bindingResult(form), file, model,
                new RedirectAttributesModelMap());

        assertEquals("admin/products/product-form", view);
        assertEquals("Tệp hình ảnh không hợp lệ.", model.getAttribute("errorMessage"));
        verify(productService, never()).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void databaseConstraintFailureReturnsFormAndRemovesUploadedImage() throws Exception {
        ProductForm form = validForm();
        MockMultipartFile file = new MockMultipartFile("imageFile", "product.png", "image/png", new byte[] { 1 });
        when(imageStorageService.store(file, "products")).thenReturn("/uploads/products/new.png");
        when(productService.create(form, "/uploads/products/new.png"))
                .thenThrow(new DataIntegrityViolationException("database constraint"));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = productController.add(form, bindingResult(form), file, model,
                new RedirectAttributesModelMap());

        assertEquals("admin/products/product-form", view);
        assertEquals(true, model.getAttribute("errorMessage").toString().contains("cơ sở dữ liệu"));
        verify(imageStorageService).deleteManagedImage("/uploads/products/new.png");
    }

    private ProductForm validForm() {
        ProductForm form = new ProductForm();
        form.setProductName("Điện thoại thử nghiệm");
        form.setPrice(new BigDecimal("1000"));
        form.setQuantity(5);
        form.setCategoryId(3);
        form.setStatus(true);
        return form;
    }

    private BeanPropertyBindingResult bindingResult(ProductForm form) {
        return new BeanPropertyBindingResult(form, "form");
    }
}
