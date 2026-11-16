package vn.iotstar.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import vn.iotstar.entity.User;

class CustomUserDetailsTest {

    @Test
    void mapsAdminManagerAndOtherRolesToAuthorities() {
        assertEquals("ROLE_ADMIN", detailsWithRole(1).getAuthorities().iterator().next().getAuthority());
        assertEquals("ROLE_MANAGER", detailsWithRole(2).getAuthorities().iterator().next().getAuthority());
        assertEquals("ROLE_USER", detailsWithRole(5).getAuthorities().iterator().next().getAuthority());
        assertEquals("ROLE_USER", detailsWithRole(99).getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void inactiveUserIsDisabledForAuthentication() {
        User user = new User();
        user.setActive(false);

        assertFalse(new CustomUserDetails(user).isEnabled());
    }

    private CustomUserDetails detailsWithRole(int roleId) {
        User user = new User();
        user.setRoleid(roleId);
        user.setUserName("tester");
        user.setPassWord("hash");
        return new CustomUserDetails(user);
    }
}
