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
import vn.iotstar.entity.Category;
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
import vn.iotstar.service.VideoCategorySuggestionService;

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
    private VideoCategorySuggestionService videoCategorySuggestionService;

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
        Video listedVideo = new Video();
        listedVideo.setVideoId("listed-video");
        listedVideo.setTitle("Listed video");
        listedVideo.setViews(12345);
        org.mockito.Mockito.when(userVideoService.listForUser(12, 0))
                .thenReturn(new PageImpl<>(List.of(listedVideo)));

        mockMvc.perform(get("/video").with(SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("video/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/video\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Video của tôi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("upload-video-cta")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chia sẻ video mới")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("12345 lượt xem")));

        mockMvc.perform(get("/video/add").with(SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("video/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"thumbnailFile\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gợi ý danh mục")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"categoryId\"")));

        Category suggestedCategory = new Category(3, "Điện thoại", null);
        org.mockito.Mockito.when(videoCategorySuggestionService.suggest("Samsung S24", "Đánh giá sản phẩm"))
                .thenReturn(java.util.Optional.of(suggestedCategory));
        mockMvc.perform(get("/video/suggest-category")
                        .param("title", "Samsung S24")
                        .param("description", "Đánh giá sản phẩm")
                        .with(SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"categoryId":3,"categoryName":"Điện thoại","message":"Gợi ý từ tiêu đề/mô tả và sản phẩm. Hãy xác nhận hoặc đổi danh mục trước khi tải lên."}
                        """));

        Video ownedVideo = new Video();
        ownedVideo.setVideoId("owned-video");
        ownedVideo.setTitle("Owned video");
        ownedVideo.setVideoUrl("/uploads/videos/owned.mp4");
        ownedVideo.setPoster("/uploads/video-thumbnails/owned.png");
        ownedVideo.setUser(user);
        org.mockito.Mockito.when(userVideoService.getForOwner("owned-video", 12, 5)).thenReturn(ownedVideo);
        mockMvc.perform(get("/video/edit")
                        .param("id", "owned-video")
                        .with(SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("video/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/uploads/videos/owned.mp4")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/uploads/video-thumbnails/owned.png")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gỡ thumbnail")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đây là video đang lưu")));

        mockMvc.perform(post("/video/thumbnail/delete")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(principal))
                        .param("id", "owned-video"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/video/edit?id=owned-video"));
        org.mockito.Mockito.verify(userVideoService).removeThumbnail("owned-video", 12, 5);

        User uploadedBy = new User();
        uploadedBy.setId(12);
        uploadedBy.setFullName("Video Owner");
        Video video = new Video();
        video.setVideoId("public-video");
        video.setTitle("Public video");
        video.setVideoUrl("/uploads/videos/test.mp4");
        video.setUser(uploadedBy);
        video.setViews(9876);
        org.mockito.Mockito.when(userVideoService.recordViewAndGetPublished("public-video")).thenReturn(video);
        org.mockito.Mockito.when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/video/public-video"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/video-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Public video")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("9876 lượt xem")));
    }
}
