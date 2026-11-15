package vn.iotstar.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import vn.iotstar.dto.ProductForm;
import vn.iotstar.entity.Product;

public interface IProductService {

    Product create(ProductForm form, String image);

    Product update(Integer productId, ProductForm form, String image);

    void hide(Integer productId);

    Optional<Product> findActiveById(Integer productId);

    Product getById(Integer productId);

    Page<Product> browse(Integer categoryId, String keyword, String sort, int page, int pageSize);

    Page<Product> searchAdmin(String keyword, Integer categoryId, int page, int pageSize);

    List<Product> newest();

    long count();
}
