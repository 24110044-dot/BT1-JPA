package vn.iotstar.util;

import java.io.Console;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordHashGenerator {

    private PasswordHashGenerator() {
    }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Chạy PasswordHashGenerator trong terminal để nhập mật khẩu an toàn.");
        }
        char[] password = console.readPassword("Mật khẩu cần mã hóa (tối đa 72 ký tự): ");
        if (password == null || password.length < 8 || password.length > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 72 ký tự.");
        }
        try {
            System.out.println(new BCryptPasswordEncoder().encode(new String(password)));
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
}
