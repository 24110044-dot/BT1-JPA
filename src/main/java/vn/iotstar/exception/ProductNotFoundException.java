package vn.iotstar.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Integer id) {
        super("Không tìm thấy sản phẩm " + id + ".");
    }
}
