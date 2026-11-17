package vn.iotstar.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.iotstar.dto.CheckoutForm;
import vn.iotstar.entity.Cart;
import vn.iotstar.entity.CartItem;
import vn.iotstar.entity.Product;
import vn.iotstar.exception.InsufficientStockException;
import vn.iotstar.exception.ProductNotFoundException;
import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.service.ICartService;
import vn.iotstar.service.IOrderService;
import vn.iotstar.service.IProductService;
import vn.iotstar.service.IUserService;

@Controller
public class OrderController {

    private final ICartService cartService;
    private final IOrderService orderService;
    private final IProductService productService;
    private final IUserService userService;

    public OrderController(ICartService cartService, IOrderService orderService,
            IProductService productService, IUserService userService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping("/checkout")
    public String checkoutForm(Authentication authentication, Model model) {
        Integer userId = userId(authentication);
        Cart cart = cartService.getCart(userId);
        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }
        CheckoutForm form = newCheckoutForm(userId);
        model.addAttribute("cart", cart);
        model.addAttribute("form", form);
        model.addAttribute("buyNow", false);
        return "shop/checkout";
    }

    @PostMapping("/checkout/buy/{productId}")
    public String prepareBuyNow(@PathVariable Integer productId, @RequestParam int quantity,
            RedirectAttributes redirectAttributes) {
        Product product = productService.findActiveById(productId).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm hiện không còn kinh doanh.");
            return "redirect:/products";
        }
        if (quantity < 1 || quantity > product.getQuantity()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Số lượng không hợp lệ hoặc vượt quá tồn kho hiện tại.");
            return "redirect:/products/detail/" + productId;
        }
        return "redirect:/checkout/buy/" + productId + "?quantity=" + quantity;
    }

    @GetMapping("/checkout/buy/{productId}")
    public String buyNowCheckoutForm(@PathVariable Integer productId,
            @RequestParam(defaultValue = "1") int quantity, Authentication authentication,
            Model model, RedirectAttributes redirectAttributes) {
        Product product = productService.findActiveById(productId).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm hiện không còn kinh doanh.");
            return "redirect:/products";
        }
        if (quantity < 1 || quantity > product.getQuantity()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Số lượng không hợp lệ hoặc vượt quá tồn kho hiện tại.");
            return "redirect:/products/detail/" + productId;
        }

        return renderBuyNowCheckout(product, quantity, newCheckoutForm(userId(authentication)), model);
    }

    @PostMapping("/checkout/buy/{productId}/place")
    public String placeBuyNow(@PathVariable Integer productId, @RequestParam int quantity,
            @Valid @ModelAttribute("form") CheckoutForm form, BindingResult result,
            Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        Integer userId = userId(authentication);
        Product product = productService.findActiveById(productId).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm hiện không còn kinh doanh.");
            return "redirect:/products";
        }
        if (quantity < 1 || quantity > product.getQuantity()) {
            model.addAttribute("errorMessage", "Số lượng đã vượt quá tồn kho hiện tại. Vui lòng chọn lại.");
            return renderBuyNowCheckout(product, Math.max(1, Math.min(quantity, product.getQuantity())),
                    form, model);
        }
        if (result.hasErrors()) {
            return renderBuyNowCheckout(product, quantity, form, model);
        }

        try {
            var order = orderService.checkoutProduct(userId, productId, quantity, form);
            return "redirect:/checkout/success/" + order.getOrderId();
        } catch (IllegalArgumentException | IllegalStateException | InsufficientStockException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return renderBuyNowCheckout(product, quantity, form, model);
        } catch (ProductNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm hiện không còn kinh doanh.");
            return "redirect:/products";
        }
    }

    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult result,
            Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        Integer userId = userId(authentication);
        if (result.hasErrors()) {
            model.addAttribute("cart", cartService.getCart(userId));
            model.addAttribute("buyNow", false);
            return "shop/checkout";
        }
        try {
            var order = orderService.checkout(userId, form);
            return "redirect:/checkout/success/" + order.getOrderId();
        } catch (IllegalArgumentException | IllegalStateException | InsufficientStockException exception) {
            model.addAttribute("cart", cartService.getCart(userId));
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("buyNow", false);
            return "shop/checkout";
        }
    }

    @GetMapping("/checkout/success/{orderId}")
    public String checkoutSuccess(@PathVariable Integer orderId, Authentication authentication, Model model) {
        var order = orderService.findOwnOrder(orderId, userId(authentication))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        model.addAttribute("order", order);
        return "shop/checkout-success";
    }

    @GetMapping("/member/orders")
    public String myOrders(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("orders", orderService.userOrders(userId(authentication), page));
        return "shop/my-orders";
    }

    @GetMapping("/member/orders/{orderId}")
    public String myOrderDetail(@PathVariable Integer orderId, Authentication authentication, Model model) {
        var order = orderService.findOwnOrder(orderId, userId(authentication))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng của bạn."));
        model.addAttribute("order", order);
        return "shop/order-detail";
    }

    @PostMapping("/member/orders/{orderId}/cancel")
    public String cancelMyOrder(@PathVariable Integer orderId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOwnOrder(orderId, userId(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng và hoàn lại tồn kho.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/member/orders";
    }

    @GetMapping("/admin/orders")
    public String adminOrders(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("orders", orderService.searchAdmin(keyword, status, page));
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", java.util.List.of("PENDING", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELLED"));
        return "admin/orders/order-list";
    }

    @GetMapping("/admin/orders/{orderId}")
    public String adminOrderDetail(@PathVariable Integer orderId, Model model) {
        var order = orderService.findAdminOrder(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        model.addAttribute("order", order);
        model.addAttribute("statuses", java.util.List.of("PENDING", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELLED"));
        return "admin/orders/order-detail";
    }

    @PostMapping("/admin/orders/{orderId}/status")
    public String updateStatus(@PathVariable Integer orderId, @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.updateStatus(orderId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/orders/" + orderId;
    }

    private Integer userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }

    private CheckoutForm newCheckoutForm(Integer userId) {
        CheckoutForm form = new CheckoutForm();
        userService.findById(userId).ifPresent(user -> {
            form.setRecipientName(user.getFullName());
            form.setPhone(user.getPhone());
        });
        return form;
    }

    private String renderBuyNowCheckout(Product product, int quantity, CheckoutForm form, Model model) {
        Cart checkoutCart = new Cart();
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        checkoutCart.addItem(item);

        model.addAttribute("cart", checkoutCart);
        model.addAttribute("form", form);
        model.addAttribute("buyNow", true);
        model.addAttribute("buyNowProductId", product.getProductId());
        model.addAttribute("buyNowQuantity", quantity);
        return "shop/checkout";
    }
}
