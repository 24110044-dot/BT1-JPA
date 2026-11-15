package vn.iotstar.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
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

import vn.iotstar.dto.AdminUserForm;
import vn.iotstar.entity.User;
import vn.iotstar.exception.DuplicateUserFieldException;
import vn.iotstar.exception.UserNotFoundException;
import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.service.IUserService;

@Controller
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin/users")
    public String list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword, Model model) {
        String query = keyword == null ? "" : keyword.trim();
        Page<User> users = userService.search(query, page, 10);
        model.addAttribute("users", users);
        model.addAttribute("keyword", query);
        return "admin/users/user-list";
    }

    @GetMapping("/admin/users/add")
    public String addForm(Model model) {
        AdminUserForm form = new AdminUserForm();
        form.setRoleid(5);
        model.addAttribute("form", form);
        model.addAttribute("isNew", true);
        return "admin/users/user-form";
    }

    @PostMapping("/admin/users/add")
    public String add(@Valid @ModelAttribute("form") AdminUserForm form, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes) {
        model.addAttribute("isNew", true);
        if (form.getPassword() == null || form.getPassword().length() < 8 || form.getPassword().length() > 72) {
            bindingResult.rejectValue("password", "password.invalid", "Mật khẩu phải từ 8 đến 72 ký tự.");
        }
        if (bindingResult.hasErrors()) {
            return "admin/users/user-form";
        }
        try {
            userService.createByAdmin(form);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công.");
            return "redirect:/admin/users";
        } catch (DuplicateUserFieldException exception) {
            bindingResult.rejectValue(exception.getField(), "user.duplicate", exception.getMessage());
            return "admin/users/user-form";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "admin/users/user-form";
        }
    }

    @GetMapping("/admin/users/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        var user = userService.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        AdminUserForm form = new AdminUserForm();
        form.setUserName(user.getUserName());
        form.setEmail(user.getEmail());
        form.setFullName(user.getFullName());
        form.setPhone(user.getPhone());
        form.setRoleid(user.getRoleid());
        form.setAvatar(user.getAvatar());
        model.addAttribute("form", form);
        model.addAttribute("userId", id);
        model.addAttribute("isNew", false);
        return "admin/users/user-form";
    }

    @GetMapping("/admin/users/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("account", userService.findById(id).orElseThrow(() -> new UserNotFoundException(id)));
        return "admin/users/user-detail";
    }

    @PostMapping("/admin/users/edit/{id}")
    public String edit(@PathVariable Integer id, @Valid @ModelAttribute("form") AdminUserForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes,
            Authentication authentication) {
        model.addAttribute("userId", id);
        model.addAttribute("isNew", false);
        if (bindingResult.hasErrors()) {
            return "admin/users/user-form";
        }
        try {
            Integer currentId = ((CustomUserDetails) authentication.getPrincipal()).getId();
            userService.updateByAdmin(id, form, currentId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công.");
            return "redirect:/admin/users";
        } catch (DuplicateUserFieldException exception) {
            bindingResult.rejectValue(exception.getField(), "user.duplicate", exception.getMessage());
            return "admin/users/user-form";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "admin/users/user-form";
        }
    }

    @PostMapping("/admin/users/delete/{id}")
    public String delete(@PathVariable Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        Integer currentId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        try {
            userService.deleteByAdmin(id, currentId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài khoản thành công.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }
}
