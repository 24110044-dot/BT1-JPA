package vn.iotstar.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import vn.iotstar.entity.Category;

public interface ICategoryService {

    Category insert(Category category);

    Category update(Category category);

    void delete(Integer id);

    Optional<Category> findById(Integer id);

    List<Category> findAll();

    List<Category> searchByName(String keyword);

    Page<Category> findAll(int page, int pageSize);

    Page<Category> searchByName(String keyword, int page, int pageSize);

    long count();
}
