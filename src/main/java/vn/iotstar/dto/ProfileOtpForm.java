package vn.iotstar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ProfileOtpForm {

    @NotBlank(message = "Vui lòng nhập mã OTP.")
    @Pattern(regexp = "^\\d{6}$", message = "OTP phải gồm đúng 6 chữ số.")
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
