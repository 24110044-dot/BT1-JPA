USE [CategoryJPA_DB];
GO

-- Copies the original categories(CategoryId, CategoryName, Images, Status) table
-- into the new Category(cate_id, cate_name, icons) table without deleting old data.
IF OBJECT_ID(N'dbo.categories', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.categories', N'CategoryName') IS NOT NULL
   AND COL_LENGTH(N'dbo.categories', N'Images') IS NOT NULL
BEGIN
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
END;
GO
