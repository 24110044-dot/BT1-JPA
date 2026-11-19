package vn.iotstar.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.iotstar.config.GlobalModelAdvice;
import vn.iotstar.config.ImageStorageService;
import vn.iotstar.entity.Category;
import vn.iotstar.service.ICartService;
import vn.iotstar.service.ICategoryService;
import vn.iotstar.service.IProductService;
import vn.iotstar.service.IVideoService;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(org.sitemesh.autoconfigure.SiteMeshViewResolverAutoConfiguration.class)
@Import(GlobalModelAdvice.class)
class ProductFormViewTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProductService productService;

    @MockitoBean
    private IVideoService videoService;

    @MockitoBean
    private ICategoryService categoryService;

    @MockitoBean
    private ICartService cartService;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @Test
    void addProductFormRendersWithCategories() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(new Category(1, "Điện thoại", null)));

        mockMvc.perform(get("/admin/products/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/product-form"));
    }

    @Test
    void addProductFormRendersWhenThereAreNoCategories() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/products/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/product-form"));
    }

    @Test
    void validAddProductFormSubmitsToProductServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/products/add")
                        .param("productName", "Điện thoại kiểm thử")
                        .param("categoryId", "1")
                        .param("price", "12990000")
                        .param("quantity", "3")
                        .param("status", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        verify(productService).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void storefrontProductListRendersWithResponsiveShopHeader() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of());
        when(productService.browse(null, null, "newest", 0, 12)).thenReturn(Page.empty());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/product-list"))
                .andExpect(content().string(containsString("bootstrap.min.css")))
                .andExpect(content().string(containsString("class=\"shop-page\"")))
                .andExpect(content().string(not(containsString("sitemesh:write"))));
    }

    @Test
    void categoryProductListShowsRelatedVideos() throws Exception {
        when(categoryService.findById(1)).thenReturn(java.util.Optional.of(new Category(1, "Danh mục", null)));
        when(categoryService.findAll()).thenReturn(List.of(new Category(1, "Danh mục", null)));
        when(productService.browse(1, null, "newest", 0, 12)).thenReturn(Page.empty());
        when(videoService.browseActive(1, 0, 8)).thenReturn(Page.empty());

        mockMvc.perform(get("/products/category/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/product-list"));

        verify(videoService).browseActive(1, 0, 8);
    }
}
