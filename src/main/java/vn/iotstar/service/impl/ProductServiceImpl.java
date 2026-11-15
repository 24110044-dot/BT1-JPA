package vn.iotstar.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.dto.ProductForm;
import vn.iotstar.entity.Category;
import vn.iotstar.entity.Product;
import vn.iotstar.exception.CategoryNotFoundException;
import vn.iotstar.exception.ProductNotFoundException;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.service.IProductService;

@Service
@Transactional
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Product create(ProductForm form, String image) {
        Product product = new Product();
        apply(product, form, image);
        return productRepository.save(product);
    }

    @Override
    public Product update(Integer productId, ProductForm form, String image) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        apply(product, form, image == null ? product.getImage() : image);
        return productRepository.save(product);
    }

    @Override
    public void hide(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        product.setStatus(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findActiveById(Integer productId) {
        return productRepository.findWithCategoryByProductId(productId).filter(product -> Boolean.TRUE.equals(product.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public Product getById(Integer productId) {
        return productRepository.findWithCategoryByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> browse(Integer categoryId, String keyword, String sort, int page, int pageSize) {
        PageRequest pageable = pageRequest(page, pageSize, sort);
        String query = keyword == null ? "" : keyword.trim();
        if (categoryId != null && !query.isEmpty()) {
            return productRepository.findByStatusTrueAndCategory_CateIdAndProductNameContainingIgnoreCase(
                    categoryId, query, pageable);
        }
        if (categoryId != null) {
            return productRepository.findByStatusTrueAndCategory_CateId(categoryId, pageable);
        }
        if (!query.isEmpty()) {
            return productRepository.findByStatusTrueAndProductNameContainingIgnoreCase(query, pageable);
        }
        return productRepository.findByStatusTrue(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchAdmin(String keyword, Integer categoryId, int page, int pageSize) {
        PageRequest pageable = pageRequest(page, pageSize, "newest");
        String query = keyword == null ? "" : keyword.trim();
        if (categoryId != null && !query.isEmpty()) {
            return productRepository.findByCategory_CateIdAndProductNameContainingIgnoreCase(
                    categoryId, query, pageable);
        }
        if (categoryId != null) {
            return productRepository.findByCategory_CateId(categoryId, pageable);
        }
        if (!query.isEmpty()) {
            return productRepository.findByProductNameContainingIgnoreCase(query, pageable);
        }
        return productRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> newest() {
        return productRepository.findTop8ByStatusTrueOrderByCreatedDateDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return productRepository.countByStatusTrue();
    }

    private void apply(Product product, ProductForm form, String image) {
        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(form.getCategoryId()));
        product.setProductName(form.getProductName().trim());
        product.setDescription(form.getDescription());
        product.setPrice(form.getPrice());
        product.setQuantity(form.getQuantity());
        product.setImage(image == null || image.isBlank() ? "/images/avatar.png" : image);
        product.setStatus(form.getStatus() == null || form.getStatus());
        product.setCategory(category);
    }

    private PageRequest pageRequest(int page, int pageSize, String sort) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Tham số phân trang không hợp lệ.");
        }
        Sort ordering = switch (sort == null ? "newest" : sort) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "createdDate");
        };
        return PageRequest.of(page, pageSize, ordering);
    }
}
