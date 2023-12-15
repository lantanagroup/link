IF OBJECT_ID(N'dbo.user', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[user]
    (
        id           UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
        email        NVARCHAR(256)    NOT NULL UNIQUE,
        [enabled]    BIT              NOT NULL             DEFAULT 1,
        [name]       NVARCHAR(128),
        passwordHash NVARCHAR(256),
        passwordSalt VARBINARY(256)
    );

    INSERT INTO dbo.[user] (email,
                            passwordSalt,
                            passwordHash)
    VALUES ('default@nhsnlink.org',
            0xc665798206a55f432c43f63701ea1a7ed24ba26d26932cc5d939ff69588ff3dd280ccbbdd63a0190ba00fd1e119880cf0d063df52b9f8c2a9e8c3ed79f976702,
            '$argon2id$v=19$m=15360,t=2,p=1$xmV5ggalX0MsQ/Y3AeoaftJLom0mkyzF2Tn/aViP890oDMu91joBkLoA/R4RmIDPDQY99SufjCqejD7Xn5dnAg$8w3buojb38d2X3Q8UbPX8FbiwMXzzMmvW5QKwmeUSYY');
END
GO

IF OBJECT_ID(N'dbo.tenantConfig', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tenantConfig
    (
        id   NVARCHAR(128) NOT NULL PRIMARY KEY,
        -- Store the config itself as an opaque JSON string
        -- That way, we don't have to alter this table every time its structure changes
        -- But worthwhile to call out the database name in an explicit column as follows?
        -- [database] NVARCHAR(128) NOT NULL UNIQUE
        json NVARCHAR(MAX) NOT NULL
    );
END
GO

IF OBJECT_ID(N'dbo.measureDef', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.measureDef
    (
        -- Use measureId as the primary key instead?
        id          UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
        bundle      NVARCHAR(max)    NOT NULL,
        lastUpdated DATETIME2        NOT NULL             DEFAULT GETDATE(),
        -- Could technically use varchar rather than NVARCHAR, since this should be a FHIR ID ([A-Za-z0-9\-\.]{1,64})
        -- But better to use NVARCHAR everywhere and not have to think about it?
        measureId   NVARCHAR(64)     NOT NULL UNIQUE
    );
END
GO

IF OBJECT_ID(N'dbo.measurePackage', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.measurePackage
    (
        -- Use measureId as the primary key instead?
        id        UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
        packageId NVARCHAR(64)     NOT NULL UNIQUE,
        -- comma separated list of measure ids (currently), or a separate table?
        measures  NVARCHAR(max)    NOT NULL
    );
END
GO

IF OBJECT_ID(N'dbo.audit', N'U') IS NULL
BEGIN
	-- Note that most of these columns allow null
    CREATE TABLE dbo.audit
    (
        id          UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
        network     NVARCHAR(128),
        -- Is NVARCHAR(max) overkill?
        -- In practice, what's the longest value we expect here?
        notes       NVARCHAR(max),
        tenantId    NVARCHAR(128) REFERENCES dbo.tenantConfig (id),
        [timestamp] DATETIME2        NOT NULL             DEFAULT GETDATE(),
        -- Normalize to refer to a table mirroring the AuditTypes enum?
        -- Probably not worth the effort
        [type]      NVARCHAR(64),
        userId      UNIQUEIDENTIFIER NOT NULL REFERENCES dbo.[user] (id)
    );
END
GO

-- LNK-1359: Adding metrics table to SQL database
IF OBJECT_ID(N'dbo.metrics', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.[metrics]
        (
            id           UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
            tenantId     NVARCHAR(128)    NOT NULL,
            reportId     NVARCHAR(128)    NOT NULL,
            category     NVARCHAR(128),
            taskName     NVARCHAR(128),
            timestamp    NVARCHAR(128) DEFAULT GETDATE(),
            data         NVARCHAR(max)
        );
    END
GO

IF OBJECT_ID(N'dbo.SubmissionStatus') IS NULL
  BEGIN
      CREATE TABLE dbo.[submissionStatus]
      (
          id            UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
          tenantId      NVARCHAR(128)   NOT NULL,
          reportId      NVARCHAR(128)   NOT NULL,
          status        NVARCHAR(50)    NOT NULL,
          measureIds    nvarchar(1024)  NOT NULL,
          startDate     DATETIME2       NOT NULL,
          endDate       DATETIME2
      );
    END
GO

-- LNK-1150: Remove non-null constraint from dbo.audit.userId
ALTER TABLE dbo.audit
    ALTER COLUMN userId UNIQUEIDENTIFIER;
GO

CREATE OR ALTER PROCEDURE [dbo].[saveMetrics]
    @id UNIQUEIDENTIFIER,
    @tenantId NVARCHAR(128),
    @reportId NVARCHAR(128),
    @category NVARCHAR(128),
    @taskName NVARCHAR(128),
    @timestamp NVARCHAR(128),
    @data NVARCHAR(MAX)
AS
BEGIN
    INSERT INTO dbo.metrics(id, tenantId, reportId, category, taskName, timestamp, data)
    VALUES(@id, @tenantId, @reportId, @category, @taskName, @timestamp, @data)
END
GO

-- LOGBACK tables
IF OBJECT_ID(N'dbo.logging_event', N'U') IS NULL
    BEGIN
        CREATE TABLE logging_event
        (
            timestmp          DECIMAL(20)   NOT NULL,
            formatted_message VARCHAR(4000) NOT NULL,
            logger_name       VARCHAR(254)  NOT NULL,
            level_string      VARCHAR(254)  NOT NULL,
            thread_name       VARCHAR(254),
            reference_flag    SMALLINT,
            arg0              VARCHAR(254),
            arg1              VARCHAR(254),
            arg2              VARCHAR(254),
            arg3              VARCHAR(254),
            caller_filename   VARCHAR(254)  NOT NULL,
            caller_class      VARCHAR(254)  NOT NULL,
            caller_method     VARCHAR(254)  NOT NULL,
            caller_line       CHAR(4)       NOT NULL,
            event_id          DECIMAL(38)   NOT NULL identity,
            PRIMARY KEY (event_id)
        )
    END
GO

IF OBJECT_ID(N'dbo.logging_event_property', N'U') IS NULL
    BEGIN
        CREATE TABLE logging_event_property
        (
            event_id     DECIMAL(38)  NOT NULL,
            mapped_key   VARCHAR(254) NOT NULL,
            mapped_value VARCHAR(1024),
            PRIMARY KEY (event_id, mapped_key),
            FOREIGN KEY (event_id) REFERENCES logging_event (event_id)
        )
    END
GO

IF OBJECT_ID(N'dbo.logging_event_exception', N'U') IS NULL
    BEGIN
        CREATE TABLE logging_event_exception
        (
            event_id   DECIMAL(38)  NOT NULL,
            i          SMALLINT     NOT NULL,
            trace_line VARCHAR(254) NOT NULL,
            PRIMARY KEY (event_id, i),
            FOREIGN KEY (event_id) REFERENCES logging_event (event_id)
        )
    END
GO

CREATE OR ALTER PROCEDURE saveUser
    -- Add the parameters for the stored procedure here
    @id UNIQUEIDENTIFIER,
	@email NVARCHAR(256),
	@enabled BIT = 1,
	@name NVARCHAR(128),
	@passwordHash NVARCHAR(256),
	@passwordSalt VARBINARY(256)
AS
BEGIN
    IF (SELECT COUNT(*) FROM [user] WHERE id = @id) > 0
        BEGIN
            DECLARE @currentEmail NVARCHAR(256)
		SET @currentEmail = (SELECT email FROM [user] WHERE id = @id)

		IF (@currentEmail != @email) AND (SELECT COUNT(*) FROM [user] WHERE email = @email) > 0
			THROW 50005, 'Another user with that email address already exists', 1

            UPDATE [user]
            SET email        = @email,
                [enabled]    = @enabled,
                [name]       = @name,
                passwordHash = @passwordHash,
                passwordSalt = @passwordSalt
            WHERE id = @id
            SELECT @id
        END
    ELSE
        BEGIN
            IF (SELECT COUNT(*) FROM [user] WHERE email = @email) > 0
                THROW 50005, 'A user with that email address already exists', 1

            INSERT INTO [user] (email, [enabled], [name], passwordHash, passwordSalt)
            OUTPUT inserted.id
            VALUES (@email, @enabled, @name, @passwordHash, @passwordSalt)
        END
END
GO

CREATE OR ALTER PROCEDURE [dbo].[saveTenant]
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

CREATE OR ALTER PROCEDURE [dbo].[saveMeasureDef]
    @measureId NVARCHAR(64),
	@bundle NVARCHAR(MAX),
	@lastUpdated DATETIME2
AS
BEGIN
	IF NOT EXISTS(SELECT * FROM dbo.measureDef t WHERE t.measureId = @measureId)
    BEGIN
        INSERT INTO dbo.measureDef(measureId, bundle, lastUpdated)
        VALUES(@measureId, @bundle, @lastUpdated)
    END
    ELSE
    BEGIN
        UPDATE measureDef
        SET bundle = @bundle, lastUpdated = @lastUpdated
        WHERE measureId = @measureId
    END
    END
GO

CREATE OR ALTER PROCEDURE [dbo].[saveMeasurePackage]
    @packageId NVARCHAR(64),
	@measures NVARCHAR(MAX)
AS
BEGIN
	IF NOT EXISTS(SELECT * FROM dbo.measurePackage t WHERE t.packageId = @packageId)
    BEGIN
        INSERT INTO dbo.measurePackage(packageId, measures)
        VALUES(@packageId, @measures)
    END
    ELSE
    BEGIN
        UPDATE measurePackage
        SET measures = @measures
        WHERE packageId = @packageId
    END
END
GO

-- LNK-1359
CREATE OR ALTER PROCEDURE [dbo].[saveMetrics]
    @id UNIQUEIDENTIFIER,
    @tenantId NVARCHAR(128),
    @reportId NVARCHAR(128),
    @category NVARCHAR(128),
    @taskName NVARCHAR(128),
    @timestamp NVARCHAR(128),
    @data NVARCHAR(MAX)
AS
BEGIN
    INSERT INTO dbo.metrics(id, tenantId, reportId, category, taskName, timestamp, data)
    VALUES(@id, @tenantId, @reportId, @category, @taskName, @timestamp, @data)
END
GO

CREATE OR ALTER PROCEDURE [dbo].[saveAudit]
    @id UNIQUEIDENTIFIER,
	@network NVARCHAR(128),
	@notes NVARCHAR(MAX),
	@tenantId NVARCHAR(128),
	@timestamp DATETIME2,
	@type NVARCHAR(64),
	@userId VARCHAR(MAX)
AS
BEGIN
	IF NOT EXISTS(SELECT * FROM dbo.audit t WHERE t.id = @id)
    BEGIN
        INSERT INTO dbo.[audit](id, network, notes, tenantId, [timestamp], [type], userId)
        VALUES(@id, @network, @notes, @tenantId, @timestamp, @type, @userId)
    END
    ELSE
    BEGIN
        UPDATE audit
        SET network = @network,
            notes = @notes,
            tenantId = @tenantId,
            [timestamp] = @timestamp,
            [type] = @type,
            userId = @userId
        WHERE id = @id
    END
END
GO

CREATE OR ALTER PROCEDURE [dbo].[saveTenant]
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

/*CREATE OR ALTER PROCEDURE getTenantSummary  @Search nvarchar(128) = null, @Sort nvarchar(128), @SortAscend Bit
AS
BEGIN
    CREATE TABLE #TenantSummary
    (
        tenantId			nvarchar(128),
        tenantName			nvarchar(1024),
        cdcOrgId			nvarchar(128),
        bundlingName		nvarchar(128),
        reportId            nvarchar(128),
        measureIds			nvarchar(1024),
        submittedTime		datetime2
    )

    --Figure Out Which Databases to call
    SELECT name as DatabaseName
    INTO #databases
    FROM master.sys.databases d
    WHERE name not in ('master','model','msdb','tempdb', DB_NAME()) -- don't query system DBs or the shared Db

    while((select Count(*) from #databases) > 0)
        BEGIN
            declare @database varchar(255) = (Select top 1 DatabaseName from #databases)
            declare @tenantConfig TABLE
                                  (
                                      Id nvarchar(128),
                                      Name nvarchar(1024),
                                      CdcOrgId nvarchar(128),
                                      bundlingName nvarchar(128)
                                  );

            -- We can get the id, name and cdcOrgId from the shared tenantConfig table
            delete from @tenantConfig
            insert into @tenantConfig
            select top 1
                JSON_VALUE(json, '$.id') as Id,
                JSON_VALUE(json, '$.name') as Name,
                JSON_VALUE(json, '$.cdcOrgId') as cdcOrgId,
                JSON_VALUE(json, '$.bundling.name') as bundlingName
            from tenantConfig
            where json like '%databaseName=' + @database + ';%'
              and ( @Search is not null and (JSON_VALUE(json, '$.name') like '%'+ @Search +'%' or  JSON_VALUE(json, '$.cdcOrgId') like '%'+ @Search +'%' or JSON_VALUE(json, '$.bundling.name') like '%'+ @Search +'%') or @Search is null)

            declare @Id varchar(255) =  '''' + (select top 1 Id from @tenantConfig) + ''''
            declare @cdcOrgId varchar(255) = ''''  + (select top 1 cdcOrgId from @tenantConfig) + ''''
            declare @name varchar(255) = ''''  + (select top 1 Name from @tenantConfig) + ''''
            declare @bundlingName varchar(255) = ''''  + (select top 1 bundlingName from @tenantConfig) + ''''


            -- Construct query to run on each tenant db
            if @Id is not null
                BEGIN
                    declare @SQL nvarchar(max) = 'IF OBJECT_ID(''[' + @database + N'].[dbo].report'', ''U'') IS NOT NULL
							  INSERT INTO #TenantSummary
							  SELECT ' + @Id +  ',' +   @name +  ',' +  @cdcOrgId + ','  +   @bundlingName + ',  id, measureIds, submittedTime' +
                                                 ' FROM [' + @database + '].dbo.Report' +
                                                 ' WHERE submittedTime IS NOT NULL' +
                                                 ' order by submittedTime desc'
                    EXEC sp_executesql @SQL
                END

            delete from #databases where Databasename = @database
        END

    select * from #TenantSummary
    order by Case when @sort = 'NAME'               AND @SortAscend = 'true'  then  tenantName    end  ASC,
             Case when @sort = 'NAME'               AND @SortAscend = 'false' then  tenantName    end  DESC,
             Case when @sort = 'NHSN_ORG_ID'        AND @SortAscend = 'true'  then  cdcOrgId      end  ASC,
             Case when @sort = 'NHSN_ORG_ID'        AND @SortAscend = 'false' then  cdcOrgId      end  DESC,
             Case when @sort = 'SUBMISSION_DATE'    AND @SortAscend = 'true'  then  submittedTime end  ASC,
             Case when @sort = 'SUBMISSION_DATE'    AND @SortAscend = 'false' then  submittedTime end  DESC

END
GO*/
