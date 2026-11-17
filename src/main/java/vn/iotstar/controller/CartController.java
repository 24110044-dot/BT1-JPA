package vn.iotstar.controller;

import jakarta.validation.constraints.Min;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.iotstar.entity.Cart;
import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.service.ICartService;

import org.springframework.security.core.Authentication;

@Controller
@Validated
public class CartController {

    private final ICartService cartService;

    public CartController(ICartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String cart(Authentication authentication, Model model) {
        model.addAttribute("cart", cartService.getCart(userId(authentication)));
        return "shop/cart";
    }

    @PostMapping("/cart/add/{productId}")
    public String add(@PathVariable Integer productId,
            @RequestParam(defaultValue = "1") @Min(1) int quantity,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        cartService.add(userId(authentication), productId, quantity);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/buy/{productId}")
    public String buyNow(@PathVariable Integer productId,
            @RequestParam(defaultValue = "1") @Min(1) int quantity) {
        return "redirect:/checkout/buy/" + productId + "?quantity=" + quantity;
    }

    @PostMapping("/cart/update/{cartItemId}")
    public String update(@PathVariable Integer cartItemId,
            @RequestParam @Min(1) int quantity, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        cartService.update(userId(authentication), cartItemId, quantity);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật giỏ hàng.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove/{cartItemId}")
    public String remove(@PathVariable Integer cartItemId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        cartService.remove(userId(authentication), cartItemId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clear(Authentication authentication, RedirectAttributes redirectAttributes) {
        cartService.clear(userId(authentication));
        redirectAttributes.addFlashAttribute("successMessage", "Đã làm trống giỏ hàng.");
        return "redirect:/cart";
    }

    private Integer userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }
}
