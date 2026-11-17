USE [CategoryJPA_DB];
GO

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @legacyCategoryId INT = OBJECT_ID(N'dbo.categories', N'U');

    IF @legacyCategoryId IS NULL
    BEGIN
        PRINT N'dbo.categories does not exist; nothing to remove.';
    END
    ELSE
    BEGIN
        IF OBJECT_ID(N'dbo.Category', N'U') IS NULL
        BEGIN
            ;THROW 51101, 'Cannot remove dbo.categories because dbo.Category does not exist.', 1;
        END;

        IF OBJECT_ID(N'dbo.videos', N'U') IS NULL
           OR COL_LENGTH(N'dbo.videos', N'CategoryId') IS NULL
        BEGIN
            ;THROW 51102, 'Cannot verify video categories because dbo.videos.CategoryId does not exist.', 1;
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
        BEGIN
            ;THROW 51103, 'Cannot remove dbo.categories until dbo.videos.CategoryId references dbo.Category(cate_id).', 1;
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
            ;THROW 51104, 'Cannot remove dbo.categories because some videos have no matching dbo.Category row.', 1;
        END;

        IF EXISTS (
            SELECT 1
            FROM sys.foreign_keys
            WHERE referenced_object_id = @legacyCategoryId
        )
        BEGIN
            ;THROW 51105, 'Cannot remove dbo.categories because another table still has a foreign key to it.', 1;
        END;

        DROP TABLE dbo.categories;
        PRINT N'Removed legacy dbo.categories. The application uses dbo.Category.';
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
