package vn.iotstar.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.entity.User;
import vn.iotstar.entity.Video;
import org.springframework.data.domain.PageImpl;
import java.util.List;

import vn.iotstar.config.GlobalModelAdvice;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.config.ImageStorageService;
import vn.iotstar.controller.CategoryController;
import vn.iotstar.controller.DashboardController;
import vn.iotstar.controller.AuthController;
import vn.iotstar.controller.UserController;
import vn.iotstar.controller.UserVideoController;
import vn.iotstar.security.CustomUserDetailsService;
import vn.iotstar.security.RoleAuthenticationSuccessHandler;
import vn.iotstar.service.ICategoryService;
import vn.iotstar.service.ICartService;
import vn.iotstar.service.IProductService;
import vn.iotstar.service.IUserService;
import vn.iotstar.service.IVideoService;
import vn.iotstar.service.AccountAuthService;
import vn.iotstar.service.UserVideoService;

@WebMvcTest({ DashboardController.class, AuthController.class, CategoryController.class, UserController.class,
        UserVideoController.class })
@ImportAutoConfiguration({
        org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration.class
})
@Import({ SecurityConfig.class, GlobalModelAdvice.class, RoleAuthenticationSuccessHandler.class,
        LoginFailureHandler.class })
@TestPropertySource(properties = "app.security.remember-me-key=test-remember-key")
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICategoryService categoryService;

    @MockitoBean
    private IUserService userService;

    @MockitoBean
    private AccountAuthService accountAuthService;

    @MockitoBean
    private UserVideoService userVideoService;

    @MockitoBean
    private ICartService cartService;

    @MockitoBean
    private IProductService productService;

    @MockitoBean
    private IVideoService videoService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginWithValidCredentialsCreatesSessionAndRedirectsByRole() throws Exception {
        User user = new User();
        user.setId(11);
        user.setUserName("admin");
        user.setPassWord(passwordEncoder.encode("StrongPass123"));
        user.setFullName("Test Admin");
        user.setRoleid(1);
        UserDetails details = new CustomUserDetails(user);
        org.mockito.Mockito.when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(details);

        mockMvc.perform(post("/login")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .param("username", "admin")
                .param("password", "StrongPass123")
                .param("remember-me", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/home"))
                .andExpect(cookie().exists("remember-me"));
    }

    @Test
    void loginWithUnknownUsernameShowsGenericFailure() throws Exception {
        org.mockito.Mockito.when(customUserDetailsService.loadUserByUsername("unknown"))
                .thenThrow(new UsernameNotFoundException("not found"));

        mockMvc.perform(post("/login")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .param("username", "unknown")
                .param("password", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=invalid"));
    }

    @Test
    void loginWithMissingCredentialsShowsSpecificValidationError() throws Exception {
        mockMvc.perform(post("/login")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .param("username", " ")
                .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=username"));
    }

    @Test
    void managerCannotOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/home")
                .with(SecurityMockMvcRequestPostProcessors.user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotOpenManagerDashboard() throws Exception {
        mockMvc.perform(get("/manager/home")
                .with(SecurityMockMvcRequestPostProcessors.user("member").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/home")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminAndManagerCategoryAndUserPagesRender() throws Exception {
        org.mockito.Mockito.when(categoryService.findAll(0, 10)).thenReturn(new PageImpl<>(List.of()));
        org.mockito.Mockito.when(categoryService.count()).thenReturn(0L);
        org.mockito.Mockito.when(userService.search("", 0, 10)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/category/list")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/users/add")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/manager/category/list")
                .with(SecurityMockMvcRequestPostProcessors.user("manager").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void loginAndRegistrationPagesRenderForAnonymousVisitors() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
        mockMvc.perform(get("/activate").param("email", "student@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chữ số thứ sáu")));
        mockMvc.perform(get("/forgot-password")).andExpect(status().isOk());
        mockMvc.perform(get("/forgot-password/verify").param("email", "student@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chữ số thứ nhất")));
        mockMvc.perform(get("/video")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/video/add")).andExpect(status().is3xxRedirection());
    }

    @Test
    void inactiveAccountIsRedirectedToActivationMessage() throws Exception {
        User user = new User();
        user.setUserName("pending");
        user.setPassWord(passwordEncoder.encode("StrongPass123"));
        user.setRoleid(5);
        user.setActive(false);
        org.mockito.Mockito.when(customUserDetailsService.loadUserByUsername("pending"))
                .thenReturn(new CustomUserDetails(user));

        mockMvc.perform(post("/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("username", "pending")
                .param("password", "StrongPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=inactive"));
    }

    @Test
    void ownerVideoPagesRenderAndDetailsArePublic() throws Exception {
        User user = new User();
        user.setId(12);
        user.setUserName("video-owner");
        user.setFullName("Video Owner");
        user.setRoleid(5);
        user.setPassWord("hash");
        CustomUserDetails principal = new CustomUserDetails(user);
        org.mockito.Mockito.when(userVideoService.listForUser(12, 0))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/video").with(SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("video/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Video của tôi")));

        mockMvc.perform(get("/video/add").with(SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("video/form"));

        User uploadedBy = new User();
        uploadedBy.setId(12);
        uploadedBy.setFullName("Video Owner");
        Video video = new Video();
        video.setVideoId("public-video");
        video.setTitle("Public video");
        video.setVideoUrl("/uploads/videos/test.mp4");
        video.setUser(uploadedBy);
        org.mockito.Mockito.when(userVideoService.getPublished("public-video")).thenReturn(video);
        org.mockito.Mockito.when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/video/public-video"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/video-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Public video")));
    }
}
