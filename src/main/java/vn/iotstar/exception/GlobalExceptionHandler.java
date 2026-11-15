package vn.iotstar.exception;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatusCode.valueOf(413))
                .body("Dung lượng tệp tải lên không được vượt quá 5 MB.");
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleImageStorageFailure(IOException exception) {
        LOGGER.error("Không thể đọc hoặc lưu hình ảnh tải lên.", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Không thể lưu hình ảnh. Hãy kiểm tra quyền ghi thư mục uploads/categories.");
    }

    @ExceptionHandler({ CategoryNotFoundException.class, UserNotFoundException.class, VideoNotFoundException.class })
    public ModelAndView handleNotFound(RuntimeException exception) {
        ModelAndView view = new ModelAndView("error/404");
        view.setStatus(HttpStatus.NOT_FOUND);
        view.addObject("errorMessage", exception.getMessage());
        return view;
    }
}
