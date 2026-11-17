package vn.iotstar.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.iotstar.service.ICartService;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private ICartService cartService;

    @Test
    void buyNowRedirectsToIndependentCheckoutWithoutChangingCart() {
        CartController controller = new CartController(cartService);

        String result = controller.buyNow(3, 2);

        assertEquals("redirect:/checkout/buy/3?quantity=2", result);
        verifyNoInteractions(cartService);
    }
}
