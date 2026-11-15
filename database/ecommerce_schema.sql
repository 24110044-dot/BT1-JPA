USE [CategoryJPA_DB];
GO

IF OBJECT_ID(N'dbo.Products', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Products (
        productId INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Products PRIMARY KEY,
        productName NVARCHAR(200) NOT NULL,
        description NVARCHAR(4000) NULL,
        price DECIMAL(19,2) NOT NULL,
        quantity INT NOT NULL,
        image NVARCHAR(255) NULL,
        status BIT NOT NULL CONSTRAINT DF_Products_status DEFAULT (1),
        createdDate DATETIME2 NOT NULL CONSTRAINT DF_Products_createdDate DEFAULT (SYSDATETIME()),
        updatedDate DATETIME2 NOT NULL CONSTRAINT DF_Products_updatedDate DEFAULT (SYSDATETIME()),
        cate_id INT NOT NULL,
        CONSTRAINT CK_Products_price CHECK (price > 0),
        CONSTRAINT CK_Products_quantity CHECK (quantity >= 0),
        CONSTRAINT FK_Products_Category FOREIGN KEY (cate_id) REFERENCES dbo.Category(cate_id)
    );
END;
GO

IF OBJECT_ID(N'dbo.Carts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Carts (
        cartId INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Carts PRIMARY KEY,
        userId INT NOT NULL,
        createdDate DATETIME2 NOT NULL CONSTRAINT DF_Carts_createdDate DEFAULT (SYSDATETIME()),
        updatedDate DATETIME2 NOT NULL CONSTRAINT DF_Carts_updatedDate DEFAULT (SYSDATETIME()),
        CONSTRAINT UQ_Carts_userId UNIQUE (userId),
        CONSTRAINT FK_Carts_Users FOREIGN KEY (userId) REFERENCES dbo.Users(id)
    );
END;
GO

IF OBJECT_ID(N'dbo.CartItems', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CartItems (
        cartItemId INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CartItems PRIMARY KEY,
        cartId INT NOT NULL,
        productId INT NOT NULL,
        quantity INT NOT NULL,
        unitPrice DECIMAL(19,2) NOT NULL,
        CONSTRAINT CK_CartItems_quantity CHECK (quantity > 0),
        CONSTRAINT CK_CartItems_unitPrice CHECK (unitPrice > 0),
        CONSTRAINT UQ_CartItems_cart_product UNIQUE (cartId, productId),
        CONSTRAINT FK_CartItems_Carts FOREIGN KEY (cartId) REFERENCES dbo.Carts(cartId),
        CONSTRAINT FK_CartItems_Products FOREIGN KEY (productId) REFERENCES dbo.Products(productId)
    );
END;
GO

IF OBJECT_ID(N'dbo.[Orders]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[Orders] (
        orderId INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Orders PRIMARY KEY,
        userId INT NOT NULL,
        orderDate DATETIME2 NOT NULL CONSTRAINT DF_Orders_orderDate DEFAULT (SYSDATETIME()),
        totalAmount DECIMAL(19,2) NOT NULL,
        recipientName NVARCHAR(100) NOT NULL,
        phone NVARCHAR(20) NOT NULL,
        shippingAddress NVARCHAR(500) NOT NULL,
        note NVARCHAR(1000) NULL,
        orderStatus NVARCHAR(20) NOT NULL,
        paymentMethod NVARCHAR(20) NOT NULL,
        paymentStatus NVARCHAR(20) NOT NULL,
        CONSTRAINT CK_Orders_totalAmount CHECK (totalAmount >= 0),
        CONSTRAINT FK_Orders_Users FOREIGN KEY (userId) REFERENCES dbo.Users(id)
    );
END;
GO

IF OBJECT_ID(N'dbo.OrderItems', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OrderItems (
        orderItemId INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_OrderItems PRIMARY KEY,
        orderId INT NOT NULL,
        productId INT NULL,
        productName NVARCHAR(200) NOT NULL,
        productImage NVARCHAR(255) NULL,
        quantity INT NOT NULL,
        unitPrice DECIMAL(19,2) NOT NULL,
        subtotal DECIMAL(19,2) NOT NULL,
        CONSTRAINT CK_OrderItems_quantity CHECK (quantity > 0),
        CONSTRAINT CK_OrderItems_unitPrice CHECK (unitPrice > 0),
        CONSTRAINT CK_OrderItems_subtotal CHECK (subtotal >= 0),
        CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (orderId) REFERENCES dbo.[Orders](orderId),
        CONSTRAINT FK_OrderItems_Products FOREIGN KEY (productId) REFERENCES dbo.Products(productId)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Products_Category_Status' AND object_id = OBJECT_ID(N'dbo.Products'))
    CREATE INDEX IX_Products_Category_Status ON dbo.Products(cate_id, status) INCLUDE (productName, price, quantity);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Products_Name' AND object_id = OBJECT_ID(N'dbo.Products'))
    CREATE INDEX IX_Products_Name ON dbo.Products(productName);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Orders_User_Date' AND object_id = OBJECT_ID(N'dbo.Orders'))
    CREATE INDEX IX_Orders_User_Date ON dbo.[Orders](userId, orderDate DESC);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Orders_Status_Date' AND object_id = OBJECT_ID(N'dbo.Orders'))
    CREATE INDEX IX_Orders_Status_Date ON dbo.[Orders](orderStatus, orderDate DESC);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_OrderItems_Order' AND object_id = OBJECT_ID(N'dbo.OrderItems'))
    CREATE INDEX IX_OrderItems_Order ON dbo.OrderItems(orderId);
GO

INSERT INTO dbo.Category (cate_name, icons)
SELECT seed.cate_name, NULL
FROM (VALUES
    (N'Điện thoại'),
    (N'Laptop'),
    (N'Máy tính bảng'),
    (N'Phụ kiện'),
    (N'Thiết bị âm thanh')
) AS seed(cate_name)
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.Category existing WHERE existing.cate_name = seed.cate_name
);
GO

INSERT INTO dbo.Products (productName, description, price, quantity, image, status, cate_id)
SELECT seed.productName, seed.description, seed.price, seed.quantity, NULL, 1, category.cate_id
FROM (VALUES
    (N'Smartphone 128GB', N'Điện thoại thông minh, bộ nhớ trong 128GB.', CAST(7990000.00 AS DECIMAL(19,2)), 20, N'Điện thoại'),
    (N'Laptop 15 inch', N'Laptop màn hình 15 inch dành cho học tập và văn phòng.', CAST(15990000.00 AS DECIMAL(19,2)), 10, N'Laptop'),
    (N'Tai nghe Bluetooth', N'Tai nghe không dây có hộp sạc.', CAST(1290000.00 AS DECIMAL(19,2)), 35, N'Thiết bị âm thanh'),
    (N'Ốp lưng chống sốc', N'Ốp lưng bảo vệ điện thoại, thiết kế chống sốc.', CAST(190000.00 AS DECIMAL(19,2)), 50, N'Phụ kiện')
) AS seed(productName, description, price, quantity, categoryName)
JOIN dbo.Category category ON category.cate_name = seed.categoryName
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.Products existing
    WHERE existing.productName = seed.productName AND existing.cate_id = category.cate_id
);
GO
