USE [CategoryJPA_DB];
GO

-- Generate three real BCrypt values by running vn.iotstar.util.PasswordHashGenerator.
-- Replace each placeholder below before executing this script. Never use plain-text passwords.
DECLARE @AdminHash NVARCHAR(100) = N'REPLACE_WITH_ADMIN_BCRYPT_HASH';
DECLARE @ManagerHash NVARCHAR(100) = N'REPLACE_WITH_MANAGER_BCRYPT_HASH';
DECLARE @UserHash NVARCHAR(100) = N'REPLACE_WITH_USER_BCRYPT_HASH';

IF @AdminHash NOT LIKE N'$2%' OR @ManagerHash NOT LIKE N'$2%' OR @UserHash NOT LIKE N'$2%'
BEGIN
    ;THROW 50001, 'Replace password placeholders with BCrypt hashes before running this script.', 1;
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Category WHERE cate_name = N'Công nghệ')
    INSERT INTO dbo.Category (cate_name, icons) VALUES (N'Công nghệ', N'/images/avatar.png');
IF NOT EXISTS (SELECT 1 FROM dbo.Category WHERE cate_name = N'Giải trí')
    INSERT INTO dbo.Category (cate_name, icons) VALUES (N'Giải trí', N'/images/avatar.png');

-- Provide the three BCrypt hashes in the variables above, then insert accounts.
-- Keep the role IDs: 1=Admin, 2=Manager, 5=User.
IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE username = N'admin')
    INSERT INTO dbo.Users (email, username, fullname, [password], avatar, roleid, phone)
    VALUES (N'admin@example.com', N'admin', N'Administrator', @AdminHash, N'/images/avatar.png', 1, N'+10000000001');
IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE username = N'manager')
    INSERT INTO dbo.Users (email, username, fullname, [password], avatar, roleid, phone)
    VALUES (N'manager@example.com', N'manager', N'Category Manager', @ManagerHash, N'/images/avatar.png', 2, N'+10000000002');
IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE username = N'member')
    INSERT INTO dbo.Users (email, username, fullname, [password], avatar, roleid, phone)
    VALUES (N'member@example.com', N'member', N'Sample Member', @UserHash, N'/images/avatar.png', 5, N'+10000000005');
