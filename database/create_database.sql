IF DB_ID(N'CategoryJPA_DB') IS NULL
BEGIN
    CREATE DATABASE [CategoryJPA_DB];
END;
GO

USE [CategoryJPA_DB];
GO

IF OBJECT_ID(N'dbo.Category', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Category (
        cate_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Category PRIMARY KEY,
        cate_name NVARCHAR(255) NOT NULL,
        icons NVARCHAR(255) NULL,
        CONSTRAINT UQ_Category_cate_name UNIQUE (cate_name)
    );
END;
GO

IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Users PRIMARY KEY,
        email NVARCHAR(254) NOT NULL,
        username NVARCHAR(50) NOT NULL,
        fullname NVARCHAR(100) NOT NULL,
        [password] NVARCHAR(100) NOT NULL,
        avatar NVARCHAR(255) NULL,
        roleid INT NOT NULL CONSTRAINT DF_Users_roleid DEFAULT (5),
        phone NVARCHAR(20) NOT NULL,
        createdDate DATETIME2 NOT NULL CONSTRAINT DF_Users_createdDate DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT UQ_Users_email UNIQUE (email),
        CONSTRAINT UQ_Users_username UNIQUE (username),
        CONSTRAINT UQ_Users_phone UNIQUE (phone)
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
        CONSTRAINT FK_videos_Category FOREIGN KEY (CategoryId)
            REFERENCES dbo.Category(cate_id)
    );
END;
GO
