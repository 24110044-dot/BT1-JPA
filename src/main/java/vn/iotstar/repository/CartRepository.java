package vn.iotstar.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import vn.iotstar.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Integer> {

    @EntityGraph(attributePaths = { "items", "items.product", "items.product.category" })
    Optional<Cart> findWithItemsByUser_Id(Integer userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "items", "items.product", "items.product.category" })
    Optional<Cart> findLockedByUser_Id(Integer userId);
}
