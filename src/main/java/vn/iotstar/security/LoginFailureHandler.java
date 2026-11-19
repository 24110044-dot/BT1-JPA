package vn.iotstar.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String reason = exception instanceof DisabledException ? "inactive"
                : username == null || username.isBlank() ? "username"
                : password == null || password.isBlank() ? "password" : "invalid";
        String redirect = request.getContextPath() + "/login?error=" + reason;
        if (username != null && !username.isBlank()) {
            redirect += "&username=" + URLEncoder.encode(username.trim(), StandardCharsets.UTF_8);
        }
        response.sendRedirect(redirect);
    }
}
