package vn.iotstar.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.entity.Category;
import vn.iotstar.exception.CategoryHasVideosException;
import vn.iotstar.exception.CategoryNotFoundException;
import vn.iotstar.exception.DuplicateCategoryNameException;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.VideoRepository;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.service.ICategoryService;

@Service
@Transactional
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoryRepository;
    private final VideoRepository videoRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, VideoRepository videoRepository,
            ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.videoRepository = videoRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Category insert(Category category) {
        String name = normalize(category.getCateName());
        if (categoryRepository.existsByCateNameIgnoreCase(name)) {
            throw new DuplicateCategoryNameException(name);
        }
        category.setCateName(name);
        return categoryRepository.save(category);
    }

    @Override
    public Category update(Category category) {
        Integer id = category.getCateId();
        Category current = id == null ? null : categoryRepository.findById(id).orElse(null);
        if (current == null) {
            throw new CategoryNotFoundException(id);
        }

        String name = normalize(category.getCateName());
        if (categoryRepository.existsByCateNameIgnoreCaseAndCateIdNot(name, id)) {
            throw new DuplicateCategoryNameException(name);
        }

        current.setCateName(name);
        current.setIcon(category.getIcon());
        return categoryRepository.save(current);
    }

    @Override
    public void delete(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        if (videoRepository.countByCategory_CateId(id) > 0) {
            throw new CategoryHasVideosException(id);
        }
        if (productRepository.countByCategory_CateId(id) > 0) {
            throw new IllegalStateException("Không thể xóa danh mục còn sản phẩm. Hãy chuyển sản phẩm sang danh mục khác trước.");
        }
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(Integer id) {
        return categoryRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> searchByName(String keyword) {
        String query = normalize(keyword);
        return query.isEmpty() ? findAll() : categoryRepository.findByCateNameContainingIgnoreCase(query);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Category> findAll(int page, int pageSize) {
        return categoryRepository.findAll(createPageable(page, pageSize));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Category> searchByName(String keyword, int page, int pageSize) {
        Pageable pageable = createPageable(page, pageSize);
        String query = normalize(keyword);
        return query.isEmpty() ? categoryRepository.findAll(pageable)
                : categoryRepository.findByCateNameContainingIgnoreCase(query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return categoryRepository.count();
    }

    private Pageable createPageable(int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Trang phải từ 0 trở lên, kích thước trang từ 1 đến 100.");
        }
        return PageRequest.of(page, pageSize);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
