package vn.iotstar.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AdminUserForm {

    @NotBlank(message = "Vui lòng nhập username.")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username không hợp lệ.")
    private String userName;

    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự.")
    private String password;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không hợp lệ.")
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "^[0-9+() .-]{8,20}$", message = "Số điện thoại không hợp lệ.")
    private String phone;

    private Integer roleid = 5;

    private String avatar;

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getRoleid() { return roleid; }
    public void setRoleid(Integer roleid) { this.roleid = roleid; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
