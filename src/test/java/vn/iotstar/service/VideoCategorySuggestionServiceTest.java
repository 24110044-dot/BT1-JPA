package vn.iotstar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.iotstar.entity.Category;
import vn.iotstar.entity.Product;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class VideoCategorySuggestionServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private VideoCategorySuggestionService suggestionService;

    @Test
    void suggestsCategoryFromVietnameseCategoryName() {
        Category category = new Category(2, "Điện thoại", null);
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(productRepository.findByStatusTrue()).thenReturn(List.of());

        assertEquals(category, suggestionService.suggest("Đánh giá điện thoại", "").orElseThrow());
    }

    @Test
    void suggestsProductCategoryWhenVideoMentionsProductName() {
        Category category = new Category(3, "Điện tử", null);
        Product product = new Product();
        product.setProductName("Samsung Galaxy S24");
        product.setCategory(category);
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(productRepository.findByStatusTrue()).thenReturn(List.of(product));

        assertEquals(category, suggestionService.suggest("Đánh giá Samsung Galaxy S24", null).orElseThrow());
    }

    @Test
    void doesNotSuggestWhenProductMatchesAreAmbiguous() {
        Category phones = new Category(3, "Điện thoại", null);
        Category accessories = new Category(4, "Phụ kiện", null);
        Product phone = new Product();
        phone.setProductName("Galaxy S24");
        phone.setCategory(phones);
        Product accessory = new Product();
        accessory.setProductName("Galaxy S24");
        accessory.setCategory(accessories);
        when(categoryRepository.findAll()).thenReturn(List.of(phones, accessories));
        when(productRepository.findByStatusTrue()).thenReturn(List.of(phone, accessory));

        assertTrue(suggestionService.suggest("Galaxy S24 đánh giá", "").isEmpty());
    }
}
