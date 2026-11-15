package vn.iotstar.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.dto.CheckoutForm;
import vn.iotstar.entity.Cart;
import vn.iotstar.entity.CartItem;
import vn.iotstar.entity.CustomerOrder;
import vn.iotstar.entity.OrderItem;
import vn.iotstar.entity.Product;
import vn.iotstar.entity.User;
import vn.iotstar.exception.InsufficientStockException;
import vn.iotstar.exception.ProductNotFoundException;
import vn.iotstar.exception.UserNotFoundException;
import vn.iotstar.repository.CartRepository;
import vn.iotstar.repository.OrderRepository;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.IOrderService;

@Service
@Transactional
public class OrderServiceImpl implements IOrderService {

    private static final List<String> ORDER_STATUSES =
            List.of("PENDING", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELLED");

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(CartRepository cartRepository, OrderRepository orderRepository,
            ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public CustomerOrder checkout(Integer userId, CheckoutForm form) {
        String method = normalizeMethod(form.getPaymentMethod());
        Cart cart = cartRepository.findLockedByUser_Id(userId)
                .orElseThrow(() -> new IllegalStateException("Giỏ hàng đang trống."));
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống.");
        }

        List<Integer> ids = cart.getItems().stream()
                .map(item -> item.getProduct().getProductId()).distinct().sorted().toList();
        Map<Integer, Product> lockedProducts = new HashMap<>();
        for (Product product : productRepository.findAllByIdInForUpdate(ids)) {
            lockedProducts.put(product.getProductId(), product);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = lockedProducts.get(cartItem.getProduct().getProductId());
            if (product == null || !Boolean.TRUE.equals(product.getStatus())) {
                throw new InsufficientStockException("Một sản phẩm trong giỏ đã ngừng kinh doanh.");
            }
            if (cartItem.getQuantity() < 1 || product.getQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException("Sản phẩm \"" + product.getProductName()
                        + "\" không đủ tồn kho. Tồn hiện tại: " + product.getQuantity() + ".");
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setRecipientName(form.getRecipientName().trim());
        order.setPhone(form.getPhone().trim());
        order.setShippingAddress(form.getShippingAddress().trim());
        order.setNote(form.getNote() == null ? null : form.getNote().trim());
        order.setPaymentMethod(method);
        order.setPaymentStatus(method.equals("COD") ? "UNPAID" : "DEMO_NOT_CHARGED");
        order.setOrderStatus("PENDING");
        order.setTotalAmount(total);

        for (CartItem cartItem : cart.getItems()) {
            Product product = lockedProducts.get(cartItem.getProduct().getProductId());
            int quantity = cartItem.getQuantity();
            product.setQuantity(product.getQuantity() - quantity);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getProductName());
            item.setProductImage(product.getImage());
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            order.addItem(item);
        }

        CustomerOrder saved = orderRepository.saveAndFlush(order);
        cart.getItems().clear();
        cartRepository.save(cart);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerOrder> userOrders(Integer userId, int page) {
        return orderRepository.findByUser_IdOrderByOrderDateDesc(userId, PageRequest.of(validPage(page), 10));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerOrder> findOwnOrder(Integer orderId, Integer userId) {
        return orderRepository.findWithItemsByOrderIdAndUser_Id(orderId, userId);
    }

    @Override
    public void cancelOwnOrder(Integer orderId, Integer userId) {
        CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
                .filter(found -> found.getUser().getId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng của bạn."));
        if (!"PENDING".equals(order.getOrderStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng đang chờ xác nhận.");
        }
        cancelAndRestock(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerOrder> searchAdmin(String keyword, String status, int page) {
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        if (normalizedStatus != null && !ORDER_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        }
        Integer orderId = null;
        if (keyword != null && !keyword.isBlank()) {
            try {
                orderId = Integer.valueOf(keyword.trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Mã đơn hàng phải là số.");
            }
        }
        return orderRepository.searchAdmin(normalizedStatus, orderId, PageRequest.of(validPage(page), 20));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerOrder> findAdminOrder(Integer orderId) {
        return orderRepository.findWithItemsByOrderId(orderId);
    }

    @Override
    public void updateStatus(Integer orderId, String status) {
        String nextStatus = status == null ? "" : status.trim().toUpperCase();
        if (!ORDER_STATUSES.contains(nextStatus)) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        }
        CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        String current = order.getOrderStatus();
        if (current.equals(nextStatus)) {
            return;
        }
        boolean allowed = switch (current) {
            case "PENDING" -> List.of("CONFIRMED", "CANCELLED").contains(nextStatus);
            case "CONFIRMED" -> List.of("SHIPPING", "CANCELLED").contains(nextStatus);
            case "SHIPPING" -> List.of("COMPLETED", "CANCELLED").contains(nextStatus);
            default -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Không thể chuyển trạng thái từ " + current + " sang " + nextStatus + ".");
        }
        if (nextStatus.equals("CANCELLED")) {
            cancelAndRestock(order);
        } else {
            order.setOrderStatus(nextStatus);
        }
    }

    private void cancelAndRestock(CustomerOrder order) {
        if ("CANCELLED".equals(order.getOrderStatus())) {
            throw new IllegalStateException("Đơn hàng đã được hủy trước đó.");
        }
        List<Integer> ids = order.getItems().stream()
                .map(OrderItem::getProduct)
                .filter(product -> product != null)
                .map(Product::getProductId).distinct().sorted().toList();
        Map<Integer, Product> lockedProducts = new HashMap<>();
        for (Product product : productRepository.findAllByIdInForUpdate(ids)) {
            lockedProducts.put(product.getProductId(), product);
        }
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                Product product = lockedProducts.get(item.getProduct().getProductId());
                if (product != null) {
                    product.setQuantity(Math.addExact(product.getQuantity(), item.getQuantity()));
                }
            }
        }
        order.setOrderStatus("CANCELLED");
        if ("DEMO_NOT_CHARGED".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("CANCELLED");
        }
    }

    private String normalizeMethod(String method) {
        String value = method == null ? "" : method.trim().toUpperCase();
        if (!List.of("COD", "DEMO").contains(value)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }
        return value;
    }

    private int validPage(int page) {
        if (page < 0 || page > 100_000) {
            throw new IllegalArgumentException("Số trang không hợp lệ.");
        }
        return page;
    }
}
