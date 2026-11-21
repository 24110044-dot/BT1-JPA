package vn.iotstar.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.iotstar.dto.ProfileEmailForm;
import vn.iotstar.dto.ProfileOtpForm;
import vn.iotstar.dto.ProfileUpdateForm;
import vn.iotstar.entity.User;
import vn.iotstar.exception.DuplicateUserFieldException;
import vn.iotstar.exception.EmailDeliveryException;
import vn.iotstar.exception.InvalidImageException;
import vn.iotstar.exception.OtpVerificationException;
import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.service.AccountAuthService;
import vn.iotstar.service.ProfileService;

@Controller
public class ProfileController {

    private final ProfileService profileService;
    private final AccountAuthService accountAuthService;
    private final SecurityContextRepository securityContextRepository;

    public ProfileController(ProfileService profileService, AccountAuthService accountAuthService,
            SecurityContextRepository securityContextRepository) {
        this.profileService = profileService;
        this.accountAuthService = accountAuthService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping({"/profile", "/member/myaccount"})
    public String profile(Authentication authentication, Model model) {
        User user = prepareProfileModel(authentication, model);
        ProfileUpdateForm updateForm = new ProfileUpdateForm();
        updateForm.setFullName(user.getFullName());
        updateForm.setPhone(user.getPhone());

        model.addAttribute("updateForm", updateForm);
        model.addAttribute("emailForm", new ProfileEmailForm());
        model.addAttribute("otpForm", new ProfileOtpForm());
        return "profile/index";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("updateForm") ProfileUpdateForm form,
            BindingResult bindingResult, Authentication authentication, Model model,
            RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            prepareProfileModel(authentication, model);
            model.addAttribute("emailForm", new ProfileEmailForm());
            model.addAttribute("otpForm", new ProfileOtpForm());
            return "profile/index";
        }
        try {
            Integer userId = currentUserId(authentication);
            User user = profileService.updateProfile(userId, form);
            refreshPrincipal(user, request, response);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cá nhân thành công.");
        } catch (DuplicateUserFieldException | IllegalArgumentException exception) {
            bindingResult.rejectValue(exception instanceof DuplicateUserFieldException duplicate
                    ? duplicate.getField() : "phone", "profile.invalid", exception.getMessage());
            prepareProfileModel(authentication, model);
            model.addAttribute("emailForm", new ProfileEmailForm());
            model.addAttribute("otpForm", new ProfileOtpForm());
            return "profile/index";
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/avatar")
    public String updateAvatar(@RequestParam("avatar") MultipartFile avatar,
            Authentication authentication, RedirectAttributes redirectAttributes,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            User user = profileService.updateAvatar(currentUserId(authentication), avatar);
            refreshPrincipal(user, request, response);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật ảnh đại diện thành công.");
        } catch (InvalidImageException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/email/send-otp")
    public String sendEmailOtp(@Valid @ModelAttribute("emailForm") ProfileEmailForm form,
            BindingResult bindingResult, Authentication authentication, Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareProfileModel(authentication, model);
            model.addAttribute("updateForm", new ProfileUpdateForm());
            model.addAttribute("otpForm", new ProfileOtpForm());
            return "profile/index";
        }
        try {
            accountAuthService.sendEmailChangeOtp(currentUserId(authentication), form.getEmail());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Mã OTP đã được gửi đến email mới.");
            redirectAttributes.addFlashAttribute("pendingEmail", form.getEmail().trim());
        } catch (OtpVerificationException | IllegalArgumentException | EmailDeliveryException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/email/verify-otp")
    public String verifyEmailOtp(@Valid @ModelAttribute("otpForm") ProfileOtpForm form,
            BindingResult bindingResult, Authentication authentication, Model model,
            RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            prepareProfileModel(authentication, model);
            model.addAttribute("updateForm", new ProfileUpdateForm());
            model.addAttribute("emailForm", new ProfileEmailForm());
            return "profile/index";
        }
        try {
            User user = accountAuthService.verifyEmailChangeOtp(currentUserId(authentication), form.getCode());
            refreshPrincipal(user, request, response);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi email thành công.");
        } catch (OtpVerificationException | IllegalArgumentException | DuplicateUserFieldException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/profile";
    }

    private Integer currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getId();
        }
        throw new IllegalStateException("Người dùng chưa đăng nhập.");
    }

    private User prepareProfileModel(Authentication authentication, Model model) {
        Integer userId = currentUserId(authentication);
        User user = profileService.getProfile(userId);
        model.addAttribute("profile", user);
        accountAuthService.pendingEmailChange(userId)
                .ifPresent(email -> model.addAttribute("pendingEmail", email));
        return user;
    }

    private void refreshPrincipal(User user, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return;
        }
        CustomUserDetails details = new CustomUserDetails(user);
        Authentication refreshed = new UsernamePasswordAuthenticationToken(
                details, authentication.getCredentials(), details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(refreshed);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
