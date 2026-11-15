package vn.iotstar.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class RoleAuthenticationSuccessHandlerTest {

    @Test
    void redirectsAdminManagerAndUserToTheirRoleHome() throws Exception {
        assertDestination("ROLE_ADMIN", "/admin/home");
        assertDestination("ROLE_MANAGER", "/manager/home");
        assertDestination("ROLE_USER", "/home");
    }

    private void assertDestination(String role, String expected) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var authentication = new UsernamePasswordAuthenticationToken(
                "test", "n/a", List.of(new SimpleGrantedAuthority(role)));

        new RoleAuthenticationSuccessHandler().onAuthenticationSuccess(request, response, authentication);

        assertEquals(expected, response.getRedirectedUrl());
    }
}
