package vn.iotstar.exception;

public class DuplicateCategoryNameException extends RuntimeException {

    public DuplicateCategoryNameException(String name) {
        super("Tên danh mục \"" + name + "\" đã tồn tại.");
    }
}
