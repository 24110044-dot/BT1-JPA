-- Legacy schema from the first CategoryManagementJPA version.
-- Do not run this for the new account/category schema. See create_database.sql.
USE [CategoryJPA_DB];
GO

IF OBJECT_ID(N'dbo.categories', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.categories (
        CategoryId INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_categories PRIMARY KEY,
        CategoryName NVARCHAR(50) NOT NULL,
        Images NVARCHAR(500) NULL,
        Status INT NULL
    );
END;
GO

IF OBJECT_ID(N'dbo.videos', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.videos (
        VideoId NVARCHAR(50) NOT NULL CONSTRAINT PK_videos PRIMARY KEY,
        Active BIT NULL,
        Description NVARCHAR(4000) NULL,
        Poster NVARCHAR(500) NULL,
        Title NVARCHAR(255) NULL,
        Views INT NULL,
        CategoryId INT NULL,
        CONSTRAINT FK_videos_categories FOREIGN KEY (CategoryId)
            REFERENCES dbo.categories(CategoryId)
    );
END;
GO
