package vn.iotstar.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Integer id) {
        super("Không tìm thấy danh mục có mã " + id + ".");
    }
}
