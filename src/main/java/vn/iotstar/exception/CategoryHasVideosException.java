package vn.iotstar.exception;

public class CategoryHasVideosException extends RuntimeException {

    public CategoryHasVideosException(Integer id) {
        super("Không thể xóa danh mục " + id + " vì vẫn còn video liên kết.");
    }
}
