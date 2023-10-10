IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'user')
    BEGIN
        CREATE TABLE dbo.[user]
        (
            id           uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
            email        nvarchar(256)    NOT NULL UNIQUE,
            [enabled]    bit              NOT NULL             DEFAULT 1,
            [name]       nvarchar(128)    NULL,
            passwordHash nvarchar(256)    NULL,
            passwordSalt varbinary(256)   NULL
        );

        -- Create default user
        -- email = default@nhsnlink.org
        -- password = linkt3mppass
        INSERT INTO dbo.[user] (email, passwordSalt, passwordHash)
        VALUES ('default@nhsnlink.org',
                0xc665798206a55f432c43f63701ea1a7ed24ba26d26932cc5d939ff69588ff3dd280ccbbdd63a0190ba00fd1e119880cf0d063df52b9f8c2a9e8c3ed79f976702,
                '$argon2id$v=19$m=15360,t=2,p=1$xmV5ggalX0MsQ/Y3AeoaftJLom0mkyzF2Tn/aViP890oDMu91joBkLoA/R4RmIDPDQY99SufjCqejD7Xn5dnAg$8w3buojb38d2X3Q8UbPX8FbiwMXzzMmvW5QKwmeUSYY');
    END

GO

CREATE OR ALTER PROCEDURE dbo.saveUser @id uniqueidentifier,
                                       @email nvarchar(256),
                                       @enabled bit,
                                       @name nvarchar(128),
                                       @passwordHash nvarchar(256),
                                       @passwordSalt varbinary(256)
AS
BEGIN
    IF EXISTS (SELECT * FROM dbo.[user] WHERE email = @email AND (@id IS NULL OR id != @id))
        BEGIN
            THROW 50005, 'The specified email address is already in use.', 1;
        END
    IF EXISTS (SELECT * FROM dbo.[user] WHERE id = @id)
        BEGIN
            UPDATE dbo.[user]
            SET email        = @email,
                [enabled]    = @enabled,
                [name]       = @name,
                passwordHash = @passwordHash,
                passwordSalt = @passwordSalt
            OUTPUT INSERTED.id
            WHERE id = @id;
        END
    ELSE
        BEGIN
            INSERT INTO dbo.[user] (email, [enabled], [name], passwordHash, passwordSalt)
            OUTPUT INSERTED.id
            VALUES (@email, @enabled, @name, @passwordHash, @passwordSalt)
        END
END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'tenantConfig')
    BEGIN
        CREATE TABLE dbo.tenantConfig
        (
            id   nvarchar(128) NOT NULL PRIMARY KEY,
            json nvarchar(max) NOT NULL
        );
    END

GO

DROP PROCEDURE IF EXISTS dbo.saveTenant;

GO

CREATE OR ALTER PROCEDURE dbo.saveTenantConfig @id nvarchar(128),
                                               @json nvarchar(max)
AS
BEGIN
    IF EXISTS (SELECT * FROM dbo.tenantConfig WHERE id = @id)
        BEGIN
            UPDATE dbo.tenantConfig
            SET json = @json
            OUTPUT INSERTED.id
            WHERE id = @id;
        END
    ELSE
        BEGIN
            INSERT INTO dbo.tenantConfig (id, json)
            OUTPUT INSERTED.id
            VALUES (@id, @json);
        END
END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'measureDef')
    BEGIN
        CREATE TABLE dbo.measureDef
        (
            id          uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
            measureId   nvarchar(64)     NOT NULL UNIQUE,
            bundle      nvarchar(max)    NOT NULL,
            lastUpdated datetime2        NOT NULL             DEFAULT GETDATE()
        );
    END

GO

CREATE OR ALTER PROCEDURE dbo.saveMeasureDef @measureId nvarchar(64),
                                             @bundle nvarchar(max),
                                             @lastUpdated datetime2
AS
BEGIN
    IF EXISTS (SELECT * FROM dbo.measureDef WHERE measureId = @measureId)
        BEGIN
            UPDATE dbo.measureDef
            SET bundle      = @bundle,
                lastUpdated = @lastUpdated
            OUTPUT INSERTED.id
            WHERE measureId = @measureId;
        END
    ELSE
        BEGIN
            INSERT INTO dbo.measureDef (measureId, bundle, lastUpdated)
            OUTPUT INSERTED.id
            VALUES (@measureId, @bundle, @lastUpdated);
        END
END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'measurePackage')
    BEGIN
        CREATE TABLE dbo.measurePackage
        (
            id        uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
            packageId nvarchar(64)     NOT NULL UNIQUE,
            measures  nvarchar(max)    NOT NULL
        );
    END

GO

CREATE OR ALTER PROCEDURE dbo.saveMeasurePackage @packageId nvarchar(64),
                                                 @measures nvarchar(max)
AS
BEGIN
    IF EXISTS (SELECT * FROM dbo.measurePackage WHERE packageId = @packageId)
        BEGIN
            UPDATE dbo.measurePackage
            SET measures = @measures
            OUTPUT INSERTED.id
            WHERE packageId = @packageId;
        END
    ELSE
        BEGIN
            INSERT INTO dbo.measurePackage (packageId, measures)
            OUTPUT INSERTED.id
            VALUES (@packageId, @measures);
        END
END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'audit')
    BEGIN
        CREATE TABLE dbo.audit
        (
            id          uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
            network     nvarchar(128)    NULL,
            notes       nvarchar(max)    NULL,
            tenantId    nvarchar(128)    NULL REFERENCES dbo.tenantConfig (id),
            [timestamp] datetime2        NOT NULL             DEFAULT GETDATE(),
            [type]      nvarchar(64)     NULL,
            userId      uniqueidentifier NOT NULL REFERENCES dbo.[user] (id)
        );
    END

GO

ALTER TABLE dbo.audit
    ALTER COLUMN userId uniqueidentifier NULL;

GO

CREATE OR ALTER PROCEDURE dbo.saveAudit @id uniqueidentifier,
                                        @network nvarchar(128),
                                        @notes nvarchar(max),
                                        @tenantId nvarchar(128),
                                        @timestamp datetime2,
                                        @type nvarchar(64),
                                        @userId uniqueidentifier
AS
BEGIN
    IF EXISTS (SELECT * FROM dbo.audit WHERE id = @id)
        BEGIN
            UPDATE dbo.audit
            SET network     = @network,
                notes       = @notes,
                tenantId    = @tenantId,
                [timestamp] = @timestamp,
                [type]      = @type,
                userId      = @userId
            OUTPUT INSERTED.id
            WHERE id = @id;
        END
    ELSE
        BEGIN
            INSERT INTO dbo.audit (id, network, notes, tenantId, [timestamp], [type], userId)
            OUTPUT INSERTED.id
            VALUES (@id, @network, @notes, @tenantId, @timestamp, @type, @userId);
        END
END

GO

DROP PROCEDURE IF EXISTS dbo.saveBulkStatusResult;

GO

DROP TABLE IF EXISTS dbo.bulkStatusResult;

GO

DROP PROCEDURE IF EXISTS dbo.saveBulkStatus;

GO

DROP TABLE IF EXISTS dbo.bulkStatus;

GO
