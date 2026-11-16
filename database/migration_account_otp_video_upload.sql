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
