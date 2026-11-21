package vn.iotstar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileUpdateForm {

    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "^(?=(?:.*\\d){8,})[0-9+() .-]{8,20}$", message = "Số điện thoại phải có ít nhất 8 chữ số.")
    private String phone;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
