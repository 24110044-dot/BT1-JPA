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
import vn.iotstar.exception.InsufficientStockException;
import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.service.ICartService;
import vn.iotstar.service.IOrderService;
import vn.iotstar.service.IUserService;

@Controller
public class OrderController {

    private final ICartService cartService;
    private final IOrderService orderService;
    private final IUserService userService;

    public OrderController(ICartService cartService, IOrderService orderService, IUserService userService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/checkout")
    public String checkoutForm(Authentication authentication, Model model) {
        Integer userId = userId(authentication);
        Cart cart = cartService.getCart(userId);
        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }
        CheckoutForm form = new CheckoutForm();
        userService.findById(userId).ifPresent(user -> {
            form.setRecipientName(user.getFullName());
            form.setPhone(user.getPhone());
        });
        model.addAttribute("cart", cart);
        model.addAttribute("form", form);
        return "shop/checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult result,
            Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        Integer userId = userId(authentication);
        if (result.hasErrors()) {
            model.addAttribute("cart", cartService.getCart(userId));
            return "shop/checkout";
        }
        try {
            var order = orderService.checkout(userId, form);
            return "redirect:/checkout/success/" + order.getOrderId();
        } catch (IllegalArgumentException | IllegalStateException | InsufficientStockException exception) {
            model.addAttribute("cart", cartService.getCart(userId));
            model.addAttribute("errorMessage", exception.getMessage());
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
}
