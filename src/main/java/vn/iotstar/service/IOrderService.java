package vn.iotstar.service;

import java.util.Optional;

import org.springframework.data.domain.Page;

import vn.iotstar.dto.CheckoutForm;
import vn.iotstar.entity.CustomerOrder;

public interface IOrderService {

    CustomerOrder checkout(Integer userId, CheckoutForm form);

    Page<CustomerOrder> userOrders(Integer userId, int page);

    Optional<CustomerOrder> findOwnOrder(Integer orderId, Integer userId);

    void cancelOwnOrder(Integer orderId, Integer userId);

    Page<CustomerOrder> searchAdmin(String keyword, String status, int page);

    Optional<CustomerOrder> findAdminOrder(Integer orderId);

    void updateStatus(Integer orderId, String status);
}
