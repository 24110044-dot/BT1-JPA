package vn.iotstar.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Integer id) {
        super("Không tìm thấy tài khoản có mã " + id + ".");
    }
}
