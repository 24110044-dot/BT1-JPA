# CategoryManagementJPA

Ứng dụng Java 17 / Spring Boot 4.1.2 quản lý danh mục, sản phẩm, tài khoản, giỏ hàng và đơn hàng bằng Spring MVC, Spring Data JPA, Hibernate, Jakarta Validation, Spring Security, Thymeleaf và SQL Server.

## Kiến trúc

- `controller`: điều hướng MVC, bind form và validation; không chứa CRUD trực tiếp.
- `service`, `service.impl`: nghiệp vụ Category/User, BCrypt, trùng dữ liệu và kiểm soát role.
- `repository`: Spring Data JPA.
- `entity`: ánh xạ `Category`, `Product`, `Cart`, `CartItem`, `CustomerOrder`, `OrderItem`, `User` và `Video`.
- `security`: UserDetails, đăng nhập, Remember Me, ánh xạ role.
- `templates`: giao diện Thymeleaf, menu theo quyền.

## Chuẩn bị SQL Server

1. Chạy `database/create_database.sql` trong SSMS bằng Windows Authentication. Script tạo `CategoryJPA_DB`, `Category`, `Users` và bảng `videos` cần entity Video cũ.
2. Chạy tiếp toàn bộ `database/ecommerce_schema.sql` trong SSMS (mở file, chọn **Execute**). Đây là bước bắt buộc cho phiên bản bán hàng: script tạo bảng Products/Carts/CartItems/Orders/OrderItems, khóa ngoại, chỉ mục và dữ liệu mẫu Category/Product; có thể chạy lại an toàn mà không nhân bản các mẫu đã thêm.
3. Chạy `database/migration_account_otp_video_upload.sql`. Script thêm trạng thái kích hoạt, bảng OTP, đường dẫn file/thời điểm upload và khóa ngoại chủ sở hữu; nếu có schema danh mục cũ, script sao chép và ánh xạ dữ liệu sang `dbo.Category` rồi sửa khóa ngoại video.
4. Cấp tài khoản Windows đang chạy ứng dụng quyền đọc/ghi database. Kết nối ứng dụng dùng `MSI\MSSQLSERVER01`, không có SQL username/password.
5. Ứng dụng chỉ sử dụng `dbo.Category(cate_id)`. Nếu xác nhận dữ liệu đã chuyển sang `dbo.Category` và không còn cần bảng cũ, hãy sao lưu database rồi chạy `database/drop_legacy_categories.sql` để xóa `dbo.categories`. Script kiểm tra khóa ngoại video, các ID danh mục video và các bảng phụ thuộc trước khi xóa; nếu chưa an toàn, script sẽ dừng và báo lỗi. Không chạy script tạo schema legacy.
6. Chạy `database/sample_data.sql` nếu cần tài khoản mẫu, sau khi tạo BCrypt hash. Script mẫu chứa placeholder và cố ý dừng nếu chưa được thay thế.

`spring.jpa.hibernate.ddl-auto=validate`: Hibernate chỉ kiểm tra mapping, không tạo hoặc sửa database.

Nếu ứng dụng báo `Schema validation: missing table [CartItems]`, database hiện tại mới có schema nền. Không đổi `ddl-auto` sang `update`/`create`; chạy `database/ecommerce_schema.sql` trên đúng database `CategoryJPA_DB`, xác nhận script hoàn tất, rồi khởi động lại ứng dụng. Script không xóa dữ liệu hiện có.

Nếu ứng dụng báo `Schema validation: missing table [AccountOtps]`, SQL Server đã kết nối nhưng database đang dùng chưa có bảng OTP. Trong SSMS, mở và Execute `database/migration_account_otp_video_upload.sql`, rồi xác nhận bằng:

```sql
USE [CategoryJPA_DB];
SELECT DB_NAME() AS current_database,
       OBJECT_ID(N'dbo.AccountOtps', N'U') AS account_otps_table;
```

`account_otps_table` phải khác `NULL`. Nếu vẫn là `NULL`, kiểm tra `current_database` có đúng `CategoryJPA_DB` và cửa sổ SSMS có báo lỗi khi chạy migration không. Sau đó khởi động lại ứng dụng; giữ nguyên `ddl-auto=validate`, không dùng `update`/`create`.

### Tạo mật khẩu BCrypt cho dữ liệu mẫu

Trong VS Code, mở `src/main/java/vn/iotstar/util/PasswordHashGenerator.java`, chạy `Run Java` từ terminal và nhập mật khẩu mạnh (8–72 ký tự) tại dấu nhắc. Tạo hash cho các tài khoản mẫu và thay các placeholder trong `database/sample_data.sql`. Không đưa mật khẩu thuần vào SQL. Username mẫu là `admin`, `manager`, `member`; mật khẩu là mật khẩu do bạn chọn khi tạo hash.

Đặt `REMEMBER_ME_KEY` thành một giá trị bí mật dài, ngẫu nhiên trước khi dùng ngoài môi trường học tập. Giá trị mặc định trong `application.properties` chỉ dùng local.

### Email OTP và tải video

- Sao chép `.env.example` để tham khảo các biến cần cấu hình. Spring Boot không tự đọc file `.env`; đặt các biến môi trường trong VS Code `launch.json`/terminal hoặc cấu hình môi trường của hệ điều hành. Không commit file `.env`.
- Cấu hình `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (mật khẩu ứng dụng SMTP), `MAIL_FROM`, `MAIL_SMTP_AUTH` và `MAIL_SMTP_STARTTLS` bằng thông tin nhà cung cấp email của bạn. Không có thông tin SMTP mặc định trong source.

#### Chạy Gmail SMTP trong PowerShell

Thay `your-gmail@gmail.com` bên dưới bằng địa chỉ Gmail của bạn (cả `MAIL_USERNAME` và `MAIL_FROM`). Gmail yêu cầu bật xác minh 2 bước và tạo **Google App Password**; không nhập mật khẩu đăng nhập Gmail thông thường. App Password được nhập kín tại dấu nhắc, khoảng trắng sẽ được loại bỏ. Các biến chỉ tồn tại trong cửa sổ PowerShell hiện tại:

```powershell
$env:MAIL_HOST = "smtp.gmail.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "your-gmail@gmail.com"
$env:MAIL_FROM = $env:MAIL_USERNAME
$env:MAIL_SMTP_AUTH = "true"
$env:MAIL_SMTP_STARTTLS = "true"

$secure = Read-Host "Nhập Google App Password" -AsSecureString
$env:MAIL_PASSWORD = (New-Object System.Net.NetworkCredential("", $secure)).Password -replace '\s', ''

Test-NetConnection smtp.gmail.com -Port 587
.\mvnw.cmd spring-boot:run
```

Tiếp tục chỉ khi `Test-NetConnection` hiển thị `TcpTestSucceeded : True`.

- Đăng ký tài khoản sẽ gửi OTP kích hoạt 6 số (5 phút, tối đa 5 lần thử, gửi lại sau 60 giây). Quên mật khẩu cũng dùng OTP và chỉ cấp quyền đặt mật khẩu trong session sau khi xác minh.
- Chạy `database/migration_account_otp_video_upload.sql` trước khi khởi động để tránh lỗi Hibernate `ddl-auto=validate`. Migration cũng chuyển khóa ngoại `videos.CategoryId` sang `Category.cate_id`; với database cũ, video được ánh xạ danh mục cũ sang danh mục mới theo tên.
- Tải video tại `/video`; danh sách của tài khoản được phân trang 6 video/trang. Khi tải lên, người dùng có thể yêu cầu gợi ý danh mục từ tiêu đề/mô tả đối chiếu tên sản phẩm và danh mục; cần xác nhận hoặc chọn lại danh mục trước khi lưu. Video không khớp rõ ràng sẽ yêu cầu chọn danh mục thủ công để xuất hiện đúng khi lọc. Hỗ trợ MP4, WebM, Ogg; thumbnail có thể nhập URL hoặc chọn ảnh PNG/JPG/GIF/BMP từ máy (tối đa 5 MB). Video lưu mặc định trong `uploads/videos`, thumbnail trong `uploads/video-thumbnails`; cả hai được phục vụ dưới `/uploads/...`. Có thể cấu hình `VIDEO_UPLOAD_DIR`, `APP_MAX_VIDEO_FILE_SIZE`, `APP_MAX_REQUEST_SIZE` và `APP_MAX_VIDEO_FILE_SIZE_BYTES`.
- Video cũ dùng YouTube ID và các trang `/videos` vẫn được giữ. Trang `/home` hiển thị tối đa 10 video đang bật mới nhất theo `UploadedAt`.

Nếu driver báo thiếu DLL Windows Authentication, cần Microsoft JDBC Auth DLL phiên bản 13.4.0, kiến trúc x64/x86 đúng với JVM, trong `PATH`. Không chuyển sang SQL Authentication.

## Chạy trong VS Code

1. Mở workspace và cài Extension Pack for Java.
2. Chạy kiểm thử/build:

   ```powershell
   .\mvnw.cmd clean test package
   ```

3. Chạy cấu hình `Spring Boot-CategoryManagementApplication` bằng **Run and Debug / F5**, hoặc:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Truy cập `http://localhost:8080/login`.

## Vai trò và URL

- `roleid=1` Admin: `/admin/home`, quản lý Category/Product/Video/User/Order. Quản lý video tại `/admin/videos`.
- `roleid=2` Manager: `/manager/home`, xem Category và các sản phẩm đang bán.
- Role khác: `/home` (trang mua sắm), `/products`, `/videos`, `/video`, `/cart`, `/checkout`, `/member/orders`. Video được hiển thị công khai khi trạng thái đang bật và có thể lọc theo danh mục; `/video` chỉ quản lý video của tài khoản hiện tại (Admin có quyền quản lý cao hơn).
- Đăng ký công khai tạo tài khoản `roleid=5` chưa kích hoạt; đăng nhập bị chặn tới khi OTP email được xác minh. Form đăng ký không nhận role từ trình duyệt.
- Spring Security bảo vệ URL ở backend, CSRF bật mặc định, logout dùng POST. Giỏ hàng và đơn hàng chỉ truy cập được bởi tài khoản sở hữu. Remember Me là cookie chữ ký của Spring Security, không chứa mật khẩu.
- Thanh toán COD và DEMO đều không thu tiền trực tuyến; DEMO hiển thị rõ trạng thái `DEMO_NOT_CHARGED`. Tồn kho được khóa và cập nhật cùng lúc với đơn hàng trong transaction.

## Kiểm tra dữ liệu

Trong SSMS, chọn `CategoryJPA_DB`:

```sql
SELECT cate_id, cate_name, icons FROM dbo.Category;
SELECT id, username, fullname, email, phone, roleid, createdDate FROM dbo.Users;
SELECT VideoId, Title, CategoryId, UserId, VideoUrl, UploadedAt FROM dbo.videos;
SELECT otpId, userId, purpose, createdAt, expiresAt, attempts, usedAt FROM dbo.AccountOtps;
```

Ảnh Category nằm trong `uploads/categories` và được phục vụ tại `/uploads/categories/...`. Video upload nằm ngoài source ở `uploads/videos`; xóa user sẽ gỡ liên kết chủ sở hữu nhưng giữ lại bản ghi video và file.

## Test

`mvnw.cmd clean test` chạy unit test dịch vụ, BCrypt/OTP, upload, role mapping và MockMvc authorization. Các test không phụ thuộc SQL Server/SMTP. Việc khởi động ứng dụng, gửi email thật, xác thực Windows Authentication và kiểm tra schema thật cần SMTP/SQL Server/JDBC Auth DLL đã cấu hình trên máy chạy ứng dụng.

## SiteMesh và validation form

- Project dùng Spring MVC + Thymeleaf. SiteMesh Decorator 3 được tích hợp bằng chính thức `org.sitemesh:spring-boot-starter-sitemesh:3.3.0` với integration `view-resolver`, tương thích Spring Boot 4.x, Java 17+ và Jakarta Servlet 6.1; không đăng ký Filter cũ, không dùng `javax.*`, không chuyển sang JSP.
- Bootstrap Decorator dùng chung nằm tại `src/main/resources/templates/decorators/default.html` và được SiteMesh chọn làm view Thymeleaf. Cấu hình trong `application.properties` áp dụng cho các Spring MVC view. Decorator cung cấp HTML shell, Bootstrap CSS/JS và form-validation JS; nội dung trang tiếp tục giữ header, navbar, sidebar, footer và CSS riêng hiện có để không nhân đôi hoặc làm đổi giao diện.
- Spring Security tiếp tục kiểm tra role ở backend cho `/admin/**`, `/manager/**`, `/cart/**`, `/checkout/**`, `/member/**`; frontend chỉ ẩn menu bổ sung, không thay quyền truy cập.
- Các DTO form có Jakarta Validation cho đăng ký, quên mật khẩu, OTP, đặt lại mật khẩu, Category, Product, User, User Video và Checkout. Controller dùng `@Valid` và `BindingResult`; lỗi nghiệp vụ trùng dữ liệu hoặc bản ghi không tồn tại được xử lý riêng.
- Spring Security vẫn kiểm tra role ở backend cho `/admin/**`, `/manager/**`, `/cart/**`, `/checkout/**`, `/member/**`; frontend chỉ ẩn menu bổ sung, không thay quyền truy cập.
- Các DTO form có Jakarta Validation cho đăng ký, quên mật khẩu, OTP, đặt lại mật khẩu, Category, Product, User, User Video và Checkout. Controller dùng `@Valid` và `BindingResult`; lỗi nghiệp vụ trùng dữ liệu hoặc bản ghi không tồn tại được xử lý riêng.
- Form HTML dùng `required`, `min`, `max`, `minlength`, `maxlength`, `pattern`, `type=email/number/password/file` và các class `is-invalid`/`invalid-feedback` của Bootstrap.

## chay test
admin pass: 123456789
phat pass: 123456789