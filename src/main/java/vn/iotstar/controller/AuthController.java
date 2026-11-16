package vn.iotstar.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.iotstar.dto.RegisterForm;
import vn.iotstar.exception.DuplicateUserFieldException;
import vn.iotstar.exception.EmailDeliveryException;
import vn.iotstar.exception.OtpVerificationException;
import vn.iotstar.service.AccountAuthService;

@Controller
public class AuthController {

    private static final String RESET_USER_ID = "passwordResetUserId";
    private static final String RESET_VERIFIED_AT = "passwordResetVerifiedAt";
    private static final long RESET_GRANT_MILLIS = 10 * 60 * 1000L;

    private final AccountAuthService accountAuthService;

    public AuthController(AccountAuthService accountAuthService) {
        this.accountAuthService = accountAuthService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/home";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:/home";
        }
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/home";
        }
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp.");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            accountAuthService.register(form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi mã kích hoạt đến email của bạn.");
            return "redirect:/activate?email=" + java.net.URLEncoder.encode(form.getEmail().trim(),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (DuplicateUserFieldException exception) {
            bindingResult.rejectValue(exception.getField(), "user.duplicate", exception.getMessage());
            return "auth/register";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "auth/register";
        } catch (EmailDeliveryException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/activate")
    public String activationForm(@RequestParam(required = false) String email, Model model,
            Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/home";
        }
        model.addAttribute("email", email == null ? "" : email);
        return "auth/activate";
    }

    @PostMapping("/activate")
    public String activate(@RequestParam String email, @RequestParam String code, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            accountAuthService.activate(email, code);
            redirectAttributes.addFlashAttribute("successMessage", "Kích hoạt thành công. Bạn có thể đăng nhập.");
            return "redirect:/login";
        } catch (OtpVerificationException exception) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", exception.getMessage());
            return "auth/activate";
        }
    }

    @PostMapping("/activate/resend")
    public String resendActivation(@RequestParam String email, Model model) {
        try {
            accountAuthService.resendActivation(email);
            model.addAttribute("successMessage", "Mã kích hoạt mới đã được gửi.");
        } catch (OtpVerificationException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
        } catch (EmailDeliveryException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
        }
        model.addAttribute("email", email);
        return "auth/activate";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(Authentication authentication) {
        return isAuthenticated(authentication) ? "redirect:/home" : "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            accountAuthService.requestPasswordReset(email);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Nếu email có tài khoản hợp lệ, mã xác minh sẽ được gửi trong ít phút.");
            redirectAttributes.addFlashAttribute("recoveryEmail", email);
            return "redirect:/forgot-password";
        } catch (OtpVerificationException exception) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Nếu email có tài khoản hợp lệ, mã xác minh sẽ được gửi trong ít phút.");
            redirectAttributes.addFlashAttribute("recoveryEmail", email);
            return "redirect:/forgot-password";
        } catch (EmailDeliveryException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("recoveryEmail", email);
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/forgot-password/verify")
    public String passwordOtpForm(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email == null ? "" : email);
        return "auth/password-otp";
    }

    @PostMapping("/forgot-password/verify")
    public String verifyPasswordOtp(@RequestParam String email, @RequestParam String code, HttpSession session,
            Model model) {
        try {
            Integer userId = accountAuthService.verifyPasswordReset(email, code);
            session.setAttribute(RESET_USER_ID, userId);
            session.setAttribute(RESET_VERIFIED_AT, System.currentTimeMillis());
            return "redirect:/reset-password";
        } catch (OtpVerificationException exception) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", exception.getMessage());
            return "auth/password-otp";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(HttpSession session) {
        return hasValidResetGrant(session) ? "auth/reset-password" : "redirect:/forgot-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String password, @RequestParam String confirmPassword,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!hasValidResetGrant(session)) {
            clearResetGrant(session);
            return "redirect:/forgot-password";
        }
        try {
            Integer userId = (Integer) session.getAttribute(RESET_USER_ID);
            accountAuthService.resetPassword(userId, password, confirmPassword);
            clearResetGrant(session);
            redirectAttributes.addFlashAttribute("successMessage", "Mật khẩu đã được cập nhật. Hãy đăng nhập.");
            return "redirect:/login";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "auth/reset-password";
        }
    }

    private boolean hasValidResetGrant(HttpSession session) {
        Object verifiedAt = session.getAttribute(RESET_VERIFIED_AT);
        return session.getAttribute(RESET_USER_ID) instanceof Integer
                && verifiedAt instanceof Long
                && System.currentTimeMillis() - (Long) verifiedAt <= RESET_GRANT_MILLIS;
    }

    private void clearResetGrant(HttpSession session) {
        session.removeAttribute(RESET_USER_ID);
        session.removeAttribute(RESET_VERIFIED_AT);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
