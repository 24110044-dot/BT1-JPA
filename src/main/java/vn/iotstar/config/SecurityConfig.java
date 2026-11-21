package vn.iotstar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import vn.iotstar.security.CustomUserDetailsService;
import vn.iotstar.security.LoginFailureHandler;
import vn.iotstar.security.RoleAuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            CustomUserDetailsService userDetailsService,
            RoleAuthenticationSuccessHandler successHandler,
            LoginFailureHandler failureHandler,
            SecurityContextRepository securityContextRepository,
            @Value("${app.security.remember-me-key}") String rememberMeKey) throws Exception {
        AccessDeniedHandler deniedHandler = (request, response, exception) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            request.getRequestDispatcher("/error/403").forward(request, response);
        };

        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/error/**",
                        "/login", "/register", "/home", "/shop", "/products", "/products/**",
                        "/videos", "/videos/**", "/activate", "/activate/**", "/forgot-password",
                        "/forgot-password/**", "/reset-password").permitAll()
                .requestMatchers("/video/add", "/video/edit", "/video/suggest-category").authenticated()
                .requestMatchers(HttpMethod.GET, "/video/{id}").permitAll()
                .requestMatchers("/video", "/video/**").authenticated()
                .requestMatchers("/profile", "/profile/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/cart/**", "/checkout/**", "/member/orders/**").hasRole("USER")
                .requestMatchers("/manager/products/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/member/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .securityContext(context -> context.securityContextRepository(securityContextRepository))
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll())
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(14 * 24 * 60 * 60))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll())
            .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(deniedHandler));

        return http.build();
    }
}
