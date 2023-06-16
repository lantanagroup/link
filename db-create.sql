CREATE TABLE dbo.[user] (
    id uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
    email nvarchar(256) NOT NULL UNIQUE,
    enabled bit NOT NULL DEFAULT 1,
    name nvarchar(128),
    passwordHash nvarchar(256),
    passwordSalt varbinary(256)
);

CREATE TABLE dbo.tenantConfig (
    id nvarchar(128) NOT NULL PRIMARY KEY,
    -- Store the config itself as an opaque JSON string
    -- That way, we don't have to alter this table every time its structure changes
    -- But worthwhile to call out the database name in an explicit column as follows?
    -- [database] nvarchar(128) NOT NULL UNIQUE
    json nvarchar(max) NOT NULL
);

CREATE TABLE dbo.measureDef (
    -- Use measureId as the primary key instead?
    id uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
    bundle nvarchar(max) NOT NULL,
    lastUpdated datetime2 NOT NULL DEFAULT GETDATE(),
    -- Could technically use varchar rather than nvarchar, since this should be a FHIR ID ([A-Za-z0-9\-\.]{1,64})
    -- But better to use nvarchar everywhere and not have to think about it?
    measureId nvarchar(64) NOT NULL UNIQUE
);

CREATE TABLE dbo.measurePackage (
    -- Use measureId as the primary key instead?
    id uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
    packageId nvarchar(64) NOT NULL UNIQUE,
	measures nvarchar(max) NOT NULL
);

-- Note that most of these columns allow null
CREATE TABLE dbo.audit (
    id uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
    network nvarchar(128),
    -- Is nvarchar(max) overkill?
    -- In practice, what's the longest value we expect here?
    notes nvarchar(max),
    tenantId nvarchar(128) REFERENCES dbo.tenantConfig (id),
    timestamp datetime2 NOT NULL DEFAULT GETDATE(),
    -- Normalize to refer to a table mirroring the AuditTypes enum?
    -- Probably not worth the effort
    type nvarchar(64),
    userId uniqueidentifier NOT NULL REFERENCES dbo.[user] (id)
);

GO