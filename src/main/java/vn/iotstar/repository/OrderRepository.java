package vn.iotstar.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.iotstar.entity.CustomerOrder;

public interface OrderRepository extends JpaRepository<CustomerOrder, Integer> {

    Page<CustomerOrder> findByUser_IdOrderByOrderDateDesc(Integer userId, Pageable pageable);

    Page<CustomerOrder> findByOrderStatusOrderByOrderDateDesc(String orderStatus, Pageable pageable);

    @EntityGraph(attributePaths = { "items", "items.product" })
    Optional<CustomerOrder> findWithItemsByOrderIdAndUser_Id(Integer orderId, Integer userId);

    @EntityGraph(attributePaths = { "user", "items", "items.product" })
    Optional<CustomerOrder> findWithItemsByOrderId(Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.orderId = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Integer id);

    Page<CustomerOrder> findByOrderStatusOrderByOrderIdDesc(String status, Pageable pageable);

    @Query("""
            select o from CustomerOrder o
            where (:status is null or o.orderStatus = :status)
              and (:orderId is null or o.orderId = :orderId)
            order by o.orderDate desc
            """)
    Page<CustomerOrder> searchAdmin(@Param("status") String status,
            @Param("orderId") Integer orderId, Pageable pageable);
}
