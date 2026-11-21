package vn.iotstar.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import vn.iotstar.service.ICategoryService;
import vn.iotstar.service.IProductService;
import vn.iotstar.service.IUserService;
import vn.iotstar.service.IVideoService;

@Controller
public class DashboardController {

    private final ICategoryService categoryService;
    private final IUserService userService;
    private final IProductService productService;
    private final IVideoService videoService;

    public DashboardController(ICategoryService categoryService, IUserService userService,
            IProductService productService, IVideoService videoService) {
        this.categoryService = categoryService;
        this.userService = userService;
        this.productService = productService;
        this.videoService = videoService;
    }

    @GetMapping({ "/", "/home" })
    public String home(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/home";
        }
        if (authentication != null && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MANAGER"))) {
            return "redirect:/manager/home";
        }
        return "redirect:/shop";
    }

    @GetMapping("/shop")
    public String shopHome(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("newestProducts", productService.newest());
        model.addAttribute("featuredVideos", videoService.featuredActive());
        model.addAttribute("totalProducts", productService.count());
        return "home";
    }

    @GetMapping("/admin/home")
    public String adminHome(Model model) {
        model.addAttribute("totalCategories", categoryService.count());
        model.addAttribute("totalUsers", userService.count());
        model.addAttribute("totalProducts", productService.count());
        return "admin/dashboard";
    }

    @GetMapping("/manager/home")
    public String managerHome(Model model) {
        model.addAttribute("totalCategories", categoryService.count());
        model.addAttribute("totalProducts", productService.count());
        return "manager/dashboard";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
