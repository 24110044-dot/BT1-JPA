package vn.iotstar.exception;

public class VideoNotFoundException extends RuntimeException {

    public VideoNotFoundException(String videoId) {
        super("Không tìm thấy video: " + videoId);
    }
}
