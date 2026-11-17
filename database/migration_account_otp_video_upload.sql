USE [CategoryJPA_DB];
GO

IF COL_LENGTH(N'dbo.Users', N'isActive') IS NULL
BEGIN
    ALTER TABLE dbo.Users ADD isActive BIT NOT NULL
        CONSTRAINT DF_Users_isActive DEFAULT (1) WITH VALUES;
END;
GO

IF OBJECT_ID(N'dbo.AccountOtps', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AccountOtps (
        otpId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_AccountOtps PRIMARY KEY,
        userId INT NOT NULL,
        purpose NVARCHAR(30) NOT NULL,
        codeHash NVARCHAR(100) NOT NULL,
        createdAt DATETIME2 NOT NULL,
        expiresAt DATETIME2 NOT NULL,
        lastSentAt DATETIME2 NOT NULL,
        attempts INT NOT NULL CONSTRAINT DF_AccountOtps_attempts DEFAULT (0),
        usedAt DATETIME2 NULL,
        CONSTRAINT FK_AccountOtps_Users FOREIGN KEY (userId) REFERENCES dbo.Users(id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
    WHERE name = N'IX_AccountOtps_User_Purpose_CreatedAt'
      AND object_id = OBJECT_ID(N'dbo.AccountOtps'))
    CREATE INDEX IX_AccountOtps_User_Purpose_CreatedAt
        ON dbo.AccountOtps(userId, purpose, createdAt DESC);
GO

IF OBJECT_ID(N'dbo.videos', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.videos', N'UserId') IS NULL
        ALTER TABLE dbo.videos ADD UserId INT NULL;

    IF COL_LENGTH(N'dbo.videos', N'VideoUrl') IS NULL
        ALTER TABLE dbo.videos ADD VideoUrl NVARCHAR(500) NULL;

    IF COL_LENGTH(N'dbo.videos', N'UploadedAt') IS NULL
        ALTER TABLE dbo.videos ADD UploadedAt DATETIME2 NULL;

    UPDATE dbo.videos SET UploadedAt = SYSDATETIME() WHERE UploadedAt IS NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.default_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.videos')
          AND parent_column_id = COLUMNPROPERTY(OBJECT_ID(N'dbo.videos'), N'UploadedAt', 'ColumnId'))
        ALTER TABLE dbo.videos ADD CONSTRAINT DF_videos_UploadedAt DEFAULT (SYSDATETIME()) FOR UploadedAt;

    ALTER TABLE dbo.videos ALTER COLUMN UploadedAt DATETIME2 NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_videos_Users')
        ALTER TABLE dbo.videos ADD CONSTRAINT FK_videos_Users
            FOREIGN KEY (UserId) REFERENCES dbo.Users(id);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes
        WHERE name = N'IX_videos_User_UploadedAt'
          AND object_id = OBJECT_ID(N'dbo.videos'))
        CREATE INDEX IX_videos_User_UploadedAt ON dbo.videos(UserId, UploadedAt DESC);
END;
GO

IF OBJECT_ID(N'dbo.videos', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.Category', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.videos', N'CategoryId') IS NOT NULL
BEGIN
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        IF OBJECT_ID(N'dbo.categories', N'U') IS NOT NULL
           AND COL_LENGTH(N'dbo.categories', N'CategoryName') IS NOT NULL
        BEGIN
            IF COL_LENGTH(N'dbo.categories', N'Images') IS NOT NULL
                EXEC(N'
                    INSERT INTO dbo.Category (cate_name, icons)
                    SELECT LEFT(LTRIM(RTRIM(old.CategoryName)), 255),
                           LEFT(old.Images, 255)
                    FROM dbo.categories AS old
                    WHERE NULLIF(LTRIM(RTRIM(old.CategoryName)), N'''') IS NOT NULL
                      AND NOT EXISTS (
                          SELECT 1
                          FROM dbo.Category AS currentCategory
                          WHERE currentCategory.cate_name = LEFT(LTRIM(RTRIM(old.CategoryName)), 255)
                      );
                ');
            ELSE
                EXEC(N'
                    INSERT INTO dbo.Category (cate_name, icons)
                    SELECT LEFT(LTRIM(RTRIM(old.CategoryName)), 255), NULL
                    FROM dbo.categories AS old
                    WHERE NULLIF(LTRIM(RTRIM(old.CategoryName)), N'''') IS NOT NULL
                      AND NOT EXISTS (
                          SELECT 1
                          FROM dbo.Category AS currentCategory
                          WHERE currentCategory.cate_name = LEFT(LTRIM(RTRIM(old.CategoryName)), 255)
                      );
                ');
        END;

        DECLARE @wrongForeignKey sysname;
        DECLARE @dropForeignKeySql nvarchar(max);
        SELECT TOP (1) @wrongForeignKey = foreignKey.name
        FROM sys.foreign_keys AS foreignKey
        INNER JOIN sys.foreign_key_columns AS foreignKeyColumn
            ON foreignKeyColumn.constraint_object_id = foreignKey.object_id
        WHERE foreignKey.parent_object_id = OBJECT_ID(N'dbo.videos')
          AND COL_NAME(foreignKeyColumn.parent_object_id, foreignKeyColumn.parent_column_id) = N'CategoryId'
          AND (
              foreignKey.referenced_object_id <> OBJECT_ID(N'dbo.Category')
              OR COL_NAME(foreignKeyColumn.referenced_object_id, foreignKeyColumn.referenced_column_id) <> N'cate_id'
          );

        WHILE @wrongForeignKey IS NOT NULL
        BEGIN
            SET @dropForeignKeySql = N'ALTER TABLE dbo.videos DROP CONSTRAINT ' + QUOTENAME(@wrongForeignKey);
            EXEC sys.sp_executesql @dropForeignKeySql;
            SET @wrongForeignKey = NULL;

            SELECT TOP (1) @wrongForeignKey = foreignKey.name
            FROM sys.foreign_keys AS foreignKey
            INNER JOIN sys.foreign_key_columns AS foreignKeyColumn
                ON foreignKeyColumn.constraint_object_id = foreignKey.object_id
            WHERE foreignKey.parent_object_id = OBJECT_ID(N'dbo.videos')
              AND COL_NAME(foreignKeyColumn.parent_object_id, foreignKeyColumn.parent_column_id) = N'CategoryId'
              AND (
                  foreignKey.referenced_object_id <> OBJECT_ID(N'dbo.Category')
                  OR COL_NAME(foreignKeyColumn.referenced_object_id, foreignKeyColumn.referenced_column_id) <> N'cate_id'
              );
        END;

        IF OBJECT_ID(N'dbo.categories', N'U') IS NOT NULL
           AND COL_LENGTH(N'dbo.categories', N'CategoryName') IS NOT NULL
        BEGIN
            EXEC(N'
                UPDATE video
                SET CategoryId = currentCategory.cate_id
                FROM dbo.videos AS video
                INNER JOIN dbo.categories AS oldCategory
                    ON oldCategory.CategoryId = video.CategoryId
                INNER JOIN dbo.Category AS currentCategory
                    ON currentCategory.cate_name = LEFT(LTRIM(RTRIM(oldCategory.CategoryName)), 255)
                WHERE video.CategoryId IS NOT NULL;
            ');
        END;

        IF EXISTS (
            SELECT 1
            FROM dbo.videos AS video
            LEFT JOIN dbo.Category AS category
                ON category.cate_id = video.CategoryId
            WHERE video.CategoryId IS NOT NULL
              AND category.cate_id IS NULL
        )
        BEGIN
            ;THROW 51001, 'Some videos have a CategoryId with no matching dbo.Category.cate_id. Map those categories before applying this migration.', 1;
        END;

        IF NOT EXISTS (
            SELECT 1
            FROM sys.foreign_keys AS foreignKey
            INNER JOIN sys.foreign_key_columns AS foreignKeyColumn
                ON foreignKeyColumn.constraint_object_id = foreignKey.object_id
            WHERE foreignKey.parent_object_id = OBJECT_ID(N'dbo.videos')
              AND foreignKey.referenced_object_id = OBJECT_ID(N'dbo.Category')
              AND COL_NAME(foreignKeyColumn.parent_object_id, foreignKeyColumn.parent_column_id) = N'CategoryId'
              AND COL_NAME(foreignKeyColumn.referenced_object_id, foreignKeyColumn.referenced_column_id) = N'cate_id'
        )
            ALTER TABLE dbo.videos ADD CONSTRAINT FK_videos_Category
                FOREIGN KEY (CategoryId) REFERENCES dbo.Category(cate_id);

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
