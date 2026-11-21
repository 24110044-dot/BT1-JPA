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

- Trước khi chạy ứng dụng, tạo file `.env` ở thư mục gốc từ mẫu `.env.example`:

  ```powershell
  Copy-Item .env.example .env
  notepad .env
  ```

- Điền giá trị thật vào `.env` trên máy chạy ứng dụng. `.env` chỉ dùng cho cấu hình riêng của máy; `.env.example` là mẫu để tạo file cấu hình khi thiết lập project.
- Các biến cấu hình:

  | Biến | Dùng cho | Hướng dẫn |
  |---|---|---|
  | `MAIL_HOST`, `MAIL_PORT` | Máy chủ SMTP | Gmail thường dùng `smtp.gmail.com` và cổng `587`. |
  | `MAIL_USERNAME` | Tài khoản gửi OTP | Địa chỉ Gmail của bạn. |
  | `MAIL_PASSWORD` | Xác thực SMTP | Dùng Google App Password; tài khoản Gmail cần bật xác minh 2 bước. Không dùng mật khẩu đăng nhập Gmail thông thường. |
  | `MAIL_FROM` | Địa chỉ người gửi | Thường đặt bằng `MAIL_USERNAME`. |
  | `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | Xác thực và mã hóa SMTP | Giữ `true` cho Gmail. |
  | `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Upload ảnh đại diện | Lấy từ Cloudinary Dashboard; API Secret chỉ lưu trong `.env`. |
  | `REMEMBER_ME_KEY` | Ký cookie Remember Me | Thay giá trị mẫu bằng một chuỗi ngẫu nhiên dài, riêng cho môi trường của bạn. |
  | `VIDEO_UPLOAD_DIR`, `APP_MAX_VIDEO_FILE_SIZE`, `APP_MAX_REQUEST_SIZE`, `APP_MAX_VIDEO_FILE_SIZE_BYTES` | Lưu video và giới hạn upload | Có thể giữ giá trị mẫu nếu không muốn thay đổi giới hạn mặc định. |

#### Chạy trong VS Code với file `.env`

Spring Boot không tự đọc `.env`. VS Code Java debug configuration cần chỉ định file đó bằng thuộc tính `envFile`. Sau khi tạo `.env`, chọn **Run and Debug → Spring Boot-CategoryManagementApplication** rồi nhấn **F5**. Cấu hình chạy cần có các thiết lập tương tự sau (điều chỉnh `projectName` nếu tên Maven project của bạn khác):

```json
{
  "type": "java",
  "name": "Spring Boot-CategoryManagementApplication",
  "request": "launch",
  "cwd": "${workspaceFolder}",
  "mainClass": "vn.iotstar.CategoryManagementApplication",
  "projectName": "categorymanagementjpa",
  "envFile": "${workspaceFolder}/.env",
  "env": {
    "PATH": "${env:PATH};${env:USERPROFILE}\\.m2\\repository\\com\\microsoft\\sqlserver\\mssql-jdbc_auth\\13.4.0.x64"
  }
}
```

`envFile` đưa các biến trong `.env` vào tiến trình Spring Boot; `application.properties` lấy chúng qua các placeholder như `${MAIL_HOST:}` và `${CLOUDINARY_API_SECRET:}`. `env.PATH` giúp JDBC Driver dùng Windows Authentication tìm Microsoft JDBC Auth DLL trong Maven cache. Nếu chưa có DLL ở đường dẫn trên, cài đúng phiên bản 13.4.0 và kiến trúc x64 tương ứng với JVM. Không chuyển sang SQL Server Authentication chỉ để xử lý thiếu DLL.

Khi thiết lập project trên máy khác, tạo `.env` từ `.env.example`, điền cấu hình của môi trường đó và bảo đảm launch configuration có `envFile` trỏ đến `${workspaceFolder}/.env`. Nếu chạy `.\mvnw.cmd spring-boot:run` trực tiếp trong terminal, Maven không tự nạp `.env`; cần cung cấp các biến môi trường cho tiến trình theo cách riêng của terminal đó.

#### Hồ sơ người dùng và Cloudinary

Sau khi đăng nhập, truy cập `http://localhost:8080/profile`. Trang hồ sơ cho phép cập nhật họ tên, số điện thoại, ảnh đại diện và thay đổi email bằng OTP. `/member/myaccount` cũng được chuyển về trang hồ sơ chung.

Chỉ ảnh JPEG, PNG hoặc WEBP, dung lượng tối đa 5 MB, được gửi lên Cloudinary từ backend. Không commit `CLOUDINARY_API_SECRET`. Khi thay đổi email, OTP 6 số có hiệu lực 5 phút, tối đa 5 lần nhập sai và thời gian chờ gửi lại 60 giây.

- Đăng ký tài khoản sẽ gửi OTP kích hoạt 6 số (5 phút, tối đa 5 lần thử, gửi lại sau 60 giây). Quên mật khẩu cũng dùng OTP và chỉ cấp quyền đặt mật khẩu trong session sau khi xác minh.
- Chạy `database/migration_account_otp_video_upload.sql` trước khi khởi động để tránh lỗi Hibernate `ddl-auto=validate`. Migration cũng chuyển khóa ngoại `videos.CategoryId` sang `Category.cate_id`; với database cũ, video được ánh xạ danh mục cũ sang danh mục mới theo tên.
- Tải video tại `/video`; danh sách của tài khoản được phân trang 6 video/trang. Khi tải lên, người dùng có thể yêu cầu gợi ý danh mục từ tiêu đề/mô tả đối chiếu tên sản phẩm và danh mục; cần xác nhận hoặc chọn lại danh mục trước khi lưu. Video không khớp rõ ràng sẽ yêu cầu chọn danh mục thủ công để xuất hiện đúng khi lọc. Hỗ trợ MP4, WebM, Ogg; thumbnail có thể nhập URL hoặc chọn ảnh PNG/JPG/GIF/BMP từ máy (tối đa 5 MB). Video lưu mặc định trong `uploads/videos`, thumbnail trong `uploads/video-thumbnails`; cả hai được phục vụ dưới `/uploads/...`. Có thể cấu hình `VIDEO_UPLOAD_DIR`, `APP_MAX_VIDEO_FILE_SIZE`, `APP_MAX_REQUEST_SIZE` và `APP_MAX_VIDEO_FILE_SIZE_BYTES`.
- Video cũ dùng YouTube ID và các trang `/videos` vẫn được giữ. Trang `/home` hiển thị tối đa 10 video đang bật mới nhất theo `UploadedAt`.

Nếu chạy Maven trực tiếp và driver báo thiếu DLL Windows Authentication, cần Microsoft JDBC Auth DLL phiên bản 13.4.0, kiến trúc x64/x86 đúng với JVM, trong `PATH`. Khi chạy từ VS Code, launch configuration ở trên thêm thư mục DLL x64 trong Maven cache vào `PATH`. Nếu cần chạy thủ công trên máy x64, thêm thư mục DLL trong Maven cache vào `PATH`:

```powershell
$authDllDirectory = Join-Path $env:USERPROFILE ".m2\repository\com\microsoft\sqlserver\mssql-jdbc_auth\13.4.0.x64"
$env:PATH = "$authDllDirectory;$env:PATH"
.\mvnw.cmd spring-boot:run
```

Thư mục trên chỉ dùng được nếu `mssql-jdbc_auth-13.4.0.x64.dll` đã có trong cache. Nếu thiếu, cài Microsoft JDBC Auth DLL đúng phiên bản/kiến trúc từ gói phát hành chính thức của Microsoft hoặc Maven Central vào thư mục nằm trong `PATH`. Không tải DLL từ trang không đáng tin cậy và không chuyển sang SQL Server Authentication.

## Chạy trong VS Code

1. Mở workspace và cài Extension Pack for Java.
2. Chạy kiểm thử/build:

   ```powershell
   .\mvnw.cmd clean test package
   ```

3. Tạo `.env` từ `.env.example`, điền cấu hình local, sau đó chạy cấu hình `Spring Boot-CategoryManagementApplication` bằng **Run and Debug / F5**. Đảm bảo cấu hình có `envFile` trỏ đến `${workspaceFolder}/.env`.

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