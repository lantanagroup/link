IF
NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[user]') AND [type] in (N'U'))
BEGIN
CREATE TABLE dbo.[user]
(
    id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID
(
),
    email NVARCHAR
(
    256
) NOT NULL UNIQUE,
    [enabled] BIT NOT NULL DEFAULT 1,
    [name] NVARCHAR
(
    128
),
    passwordHash NVARCHAR
(
    256
),
    passwordSalt VARBINARY
(
    256
)
    );
END
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[tenantConfig]') AND [type] in (N'U'))
BEGIN
CREATE TABLE dbo.tenantConfig
(
    id NVARCHAR(128) NOT NULL PRIMARY KEY,
    -- Store the config itself as an opaque JSON string
    -- That way, we don't have to alter this table every time its structure changes
    -- But worthwhile to call out the database name in an explicit column as follows?
    -- [database] NVARCHAR(128) NOT NULL UNIQUE
    json NVARCHAR(MAX) NOT NULL
    );
END
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[measureDef]') AND [type] in (N'U'))
BEGIN
CREATE TABLE dbo.measureDef
(
    -- Use measureId as the primary key instead?
    id          UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
    bundle      NVARCHAR( max) NOT NULL,
    lastUpdated DATETIME2        NOT NULL             DEFAULT GETDATE(),
    -- Could technically use varchar rather than NVARCHAR, since this should be a FHIR ID ([A-Za-z0-9\-\.]{1,64})
    -- But better to use NVARCHAR everywhere and not have to think about it?
    measureId   NVARCHAR(64) NOT NULL UNIQUE
);
END
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[measurePackage]') AND [type] in (N'U'))
BEGIN
CREATE TABLE dbo.measurePackage
(
    -- Use measureId as the primary key instead?
    id        UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
    packageId NVARCHAR(64) NOT NULL UNIQUE,
    -- comma separated list of measure ids (currently), or a separate table?
    measures  NVARCHAR( max) NOT NULL
);
END
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[audit]') AND [type] in (N'U'))
BEGIN
	-- Note that most of these columns allow null
CREATE TABLE dbo.audit
(
    id       UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
    network  NVARCHAR(128),
    -- Is NVARCHAR(max) overkill?
    -- In practice, what's the longest value we expect here?
    notes    NVARCHAR( max),
    tenantId NVARCHAR(128) REFERENCES dbo.tenantConfig (id), [
    timestamp]
    DATETIME2
    NOT
    NULL
    DEFAULT
    GETDATE
(
),
    -- Normalize to refer to a table mirroring the AuditTypes enum?
    -- Probably not worth the effort
    [type] NVARCHAR(64), userId UNIQUEIDENTIFIER NOT NULL REFERENCES dbo.[user] (id)
    );
END
GO

DROP PROCEDURE IF EXISTS saveUser
    GO

CREATE PROCEDURE saveUser
    -- Add the parameters for the stored procedure here
    @id UNIQUEIDENTIFIER,
	@email NVARCHAR(256),
	@enabled BIT = 1,
	@name NVARCHAR(128),
	@passwordHash NVARCHAR(256),
	@passwordSalt VARBINARY(256)
AS
BEGIN
	IF
(
SELECT COUNT(*)
FROM [user]
WHERE id = @id) > 0
BEGIN
		DECLARE
@currentEmail NVARCHAR(256)
		SET @currentEmail = (SELECT email FROM [user] WHERE id = @id)

		IF (@currentEmail != @email) AND (SELECT COUNT(*) FROM [user] WHERE email = @email) > 0
			THROW 50005, 'Another user with that email address already exists', 1

UPDATE [user]
SET
    email = @email, [enabled] = @enabled, [name] = @name, passwordHash = @passwordHash, passwordSalt = @passwordSalt
WHERE id = @id
SELECT @id
END
ELSE
BEGIN
		IF
(
SELECT COUNT(*)
FROM [user]
WHERE email = @email)
    > 0
    THROW 50005
    , 'A user with that email address already exists'
    , 1

INSERT
INTO [user] (email, [enabled], [name], passwordHash, passwordSalt)
VALUES (@email, @enabled, @name, @passwordHash, @passwordSalt)
SELECT SCOPE_IDENTITY()
END
END
GO

DROP PROCEDURE IF EXISTS saveTenant
GO

CREATE PROCEDURE [dbo].[saveTenant]
    @tenantId nvarchar(128),
	@json NVARCHAR(MAX)
AS
BEGIN

	IF NOT EXISTS(SELECT * FROM dbo.tenantConfig t WHERE t.id = @tenantId)
    BEGIN
        INSERT INTO dbo.tenantConfig (id, json)
        VALUES(@tenantId, @json)
    END
    ELSE
    BEGIN
        update tenantConfig
        set json = @json
        where id = @tenantId
    END
END
GO
