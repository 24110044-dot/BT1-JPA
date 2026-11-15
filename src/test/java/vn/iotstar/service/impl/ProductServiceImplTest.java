package vn.iotstar.service.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import vn.iotstar.entity.Product;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void categorySearchForStorefrontExcludesHiddenProducts() {
        Page<Product> expected = Page.empty();
        when(productRepository
                .findByStatusTrueAndCategory_CateIdAndProductNameContainingIgnoreCase(
                        eq(4), eq("phone"), any()))
                .thenReturn(expected);

        Page<Product> actual = productService.browse(4, "phone", "newest", 0, 12);

        assertSame(expected, actual);
        verify(productRepository).findByStatusTrueAndCategory_CateIdAndProductNameContainingIgnoreCase(
                eq(4), eq("phone"), any());
    }

    @Test
    void adminCategorySearchIncludesHiddenProducts() {
        Page<Product> expected = Page.empty();
        when(productRepository.findByCategory_CateIdAndProductNameContainingIgnoreCase(
                eq(4), eq("phone"), any()))
                .thenReturn(expected);

        Page<Product> actual = productService.searchAdmin("phone", 4, 0, 12);

        assertSame(expected, actual);
        verify(productRepository).findByCategory_CateIdAndProductNameContainingIgnoreCase(
                eq(4), eq("phone"), any());
    }
}
