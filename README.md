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
3. Cấp tài khoản Windows đang chạy ứng dụng quyền đọc/ghi database. Kết nối ứng dụng dùng `MSI\MSSQLSERVER01`, không có SQL username/password.
4. Nếu database đã có bảng `dbo.categories` của phiên bản trước, dữ liệu cũ được giữ nguyên. Có thể chạy `database/migrate_legacy_categories.sql` để chép Category cũ sang `dbo.Category`; kiểm tra kết quả trước khi dùng ứng dụng.
5. Chạy `database/sample_data.sql` nếu cần tài khoản mẫu, sau khi tạo BCrypt hash. Script mẫu chứa placeholder và cố ý dừng nếu chưa được thay thế.

`spring.jpa.hibernate.ddl-auto=validate`: Hibernate chỉ kiểm tra mapping, không tạo hoặc sửa database.

Nếu ứng dụng báo `Schema validation: missing table [CartItems]`, database hiện tại mới có schema nền. Không đổi `ddl-auto` sang `update`/`create`; chạy `database/ecommerce_schema.sql` trên đúng database `CategoryJPA_DB`, xác nhận script hoàn tất, rồi khởi động lại ứng dụng. Script không xóa dữ liệu hiện có.

### Tạo mật khẩu BCrypt cho dữ liệu mẫu

Trong VS Code, mở `src/main/java/vn/iotstar/util/PasswordHashGenerator.java`, chạy `Run Java` từ terminal và nhập mật khẩu mạnh (8–72 ký tự) tại dấu nhắc. Tạo hash cho các tài khoản mẫu và thay các placeholder trong `database/sample_data.sql`. Không đưa mật khẩu thuần vào SQL. Username mẫu là `admin`, `manager`, `member`; mật khẩu là mật khẩu do bạn chọn khi tạo hash.

Đặt `REMEMBER_ME_KEY` thành một giá trị bí mật dài, ngẫu nhiên trước khi dùng ngoài môi trường học tập. Giá trị mặc định trong `application.properties` chỉ dùng local.

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
- Role khác: `/home` (trang mua sắm), `/products`, `/videos`, `/cart`, `/checkout`, `/member/orders`. Video được hiển thị công khai khi trạng thái đang bật và có thể lọc theo danh mục.
- Đăng ký công khai tạo tài khoản `roleid=5`; form đăng ký không nhận role từ trình duyệt.
- Spring Security bảo vệ URL ở backend, CSRF bật mặc định, logout dùng POST. Giỏ hàng và đơn hàng chỉ truy cập được bởi tài khoản sở hữu. Remember Me là cookie chữ ký của Spring Security, không chứa mật khẩu.
- Thanh toán COD và DEMO đều không thu tiền trực tuyến; DEMO hiển thị rõ trạng thái `DEMO_NOT_CHARGED`. Tồn kho được khóa và cập nhật cùng lúc với đơn hàng trong transaction.

## Kiểm tra dữ liệu

Trong SSMS, chọn `CategoryJPA_DB`:

```sql
SELECT cate_id, cate_name, icons FROM dbo.Category;
SELECT id, username, fullname, email, phone, roleid, createdDate FROM dbo.Users;
SELECT VideoId, Title, CategoryId FROM dbo.videos;
```

Ảnh Category nằm trong `uploads/categories` và được phục vụ tại `/uploads/categories/...`.

## Test

`mvnw.cmd clean test` chạy unit test dịch vụ, BCrypt/role mapping và MockMvc authorization cho Admin/Manager/User. Các test không phụ thuộc SQL Server. Việc khởi động ứng dụng, xác thực Windows Authentication và kiểm tra schema thật cần SQL Server/JDBC Auth DLL trên máy chạy ứng dụng.

Tai khoan thu:
admin pass: 123456789
phat  pass: 123456789