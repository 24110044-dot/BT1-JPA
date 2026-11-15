package vn.iotstar.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductForm {

    @NotBlank(message = "Tên sản phẩm không được để trống.")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự.")
    private String productName;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự.")
    private String description;

    @NotNull(message = "Vui lòng nhập giá.")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0.")
    private BigDecimal price;

    @NotNull(message = "Vui lòng nhập số lượng.")
    @Min(value = 0, message = "Tồn kho không được âm.")
    private Integer quantity;

    @NotNull(message = "Vui lòng chọn danh mục.")
    private Integer categoryId;

    private Boolean status = true;

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
}
