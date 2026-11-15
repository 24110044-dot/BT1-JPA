package vn.iotstar.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.service.ICartService;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {

    private final ICartService cartService;

    public GlobalModelAdvice(ICartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("currentUser")
    public CustomUserDetails currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        return null;
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    @ModelAttribute("isManager")
    public boolean isManager() {
        return hasRole("ROLE_MANAGER");
    }

    @ModelAttribute("activePath")
    public String activePath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("cartCount")
    public int cartCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details
                && details.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER"))) {
            return cartService.itemCount(details.getId());
        }
        return 0;
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
