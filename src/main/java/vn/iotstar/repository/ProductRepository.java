package vn.iotstar.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.iotstar.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByStatusTrue(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByStatusTrueAndCategory_CateId(Integer categoryId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByStatusTrueAndProductNameContainingIgnoreCase(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByStatusTrueAndCategory_CateIdAndProductNameContainingIgnoreCase(
            Integer categoryId, String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByProductNameContainingIgnoreCase(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategory_CateId(Integer categoryId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategory_CateIdAndProductNameContainingIgnoreCase(
            Integer categoryId, String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryByProductId(Integer productId);

    @EntityGraph(attributePaths = "category")
    List<Product> findTop8ByStatusTrueOrderByCreatedDateDesc();

    @EntityGraph(attributePaths = "category")
    List<Product> findByStatusTrue();

    long countByCategory_CateId(Integer categoryId);

    long countByStatusTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId in :ids order by p.productId")
    List<Product> findAllByIdInForUpdate(@Param("ids") Collection<Integer> ids);
}
