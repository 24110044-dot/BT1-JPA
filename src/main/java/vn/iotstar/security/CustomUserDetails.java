package vn.iotstar.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import vn.iotstar.entity.User;

public class CustomUserDetails implements UserDetails {

    private final Integer id;
    private final String username;
    private final String password;
    private final String fullName;
    private final String avatar;
    private final Integer roleId;
    private final String email;
    private final String phone;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUserName();
        this.password = user.getPassWord();
        this.fullName = user.getFullName();
        this.avatar = user.getAvatar();
        this.roleId = user.getRoleid();
        this.email = user.getEmail();
        this.phone = user.getPhone();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = Integer.valueOf(1).equals(roleId) ? "ROLE_ADMIN"
                : Integer.valueOf(2).equals(roleId) ? "ROLE_MANAGER" : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    public Integer getId() { return id; }
    public String getFullName() { return fullName; }
    public String getAvatar() { return avatar; }
    public Integer getRoleId() { return roleId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
