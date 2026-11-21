package vn.iotstar.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileEmailForm {

    @NotBlank(message = "Vui lòng nhập email mới.")
    @Email(message = "Email mới không hợp lệ.")
    @Size(max = 254, message = "Email tối đa 254 ký tự.")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
