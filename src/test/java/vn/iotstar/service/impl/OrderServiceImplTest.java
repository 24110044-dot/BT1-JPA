package vn.iotstar.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.iotstar.dto.CheckoutForm;
import vn.iotstar.entity.CustomerOrder;
import vn.iotstar.entity.Product;
import vn.iotstar.entity.User;
import vn.iotstar.exception.InsufficientStockException;
import vn.iotstar.repository.CartRepository;
import vn.iotstar.repository.OrderRepository;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final Integer USER_ID = 17;
    private static final Integer PRODUCT_ID = 42;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Product product;
    private User user;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setProductId(PRODUCT_ID);
        product.setProductName("Keyboard");
        product.setPrice(new BigDecimal("125.50"));
        product.setQuantity(5);
        product.setStatus(true);

        user = new User();
        user.setId(USER_ID);
    }

    @Test
    void checkoutProductCreatesOrderForRequestedQuantityWithoutTouchingCart() {
        when(productRepository.findAllByIdInForUpdate(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orderRepository.saveAndFlush(any(CustomerOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerOrder order = orderService.checkoutProduct(USER_ID, PRODUCT_ID, 2, validForm());

        assertEquals(1, order.getItems().size());
        assertEquals(2, order.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("251.00"), order.getTotalAmount());
        assertEquals(3, product.getQuantity());
        verify(cartRepository, never()).findLockedByUser_Id(USER_ID);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void checkoutProductRejectsQuantityAboveStockWithoutCreatingOrderOrChangingCart() {
        when(productRepository.findAllByIdInForUpdate(List.of(PRODUCT_ID))).thenReturn(List.of(product));

        assertThrows(InsufficientStockException.class,
                () -> orderService.checkoutProduct(USER_ID, PRODUCT_ID, 6, validForm()));

        assertEquals(5, product.getQuantity());
        verify(orderRepository, never()).saveAndFlush(any());
        verify(cartRepository, never()).findLockedByUser_Id(USER_ID);
        verify(cartRepository, never()).save(any());
    }

    private CheckoutForm validForm() {
        CheckoutForm form = new CheckoutForm();
        form.setRecipientName("Buyer");
        form.setPhone("0123456789");
        form.setShippingAddress("1 Sample Street");
        form.setPaymentMethod("COD");
        return form;
    }
}
