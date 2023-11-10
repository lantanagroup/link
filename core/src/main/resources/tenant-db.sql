IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'conceptMap')
    BEGIN
        CREATE TABLE dbo.conceptMap
        (
            id         nvarchar(128)  NOT NULL PRIMARY KEY,
            contexts   nvarchar(1024) NOT NULL,
            conceptMap nvarchar(max)  NOT NULL
        );
    END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'patientList')
    BEGIN
        CREATE TABLE dbo.patientList
        (
            id          uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
            measureId   nvarchar(64)     NOT NULL,
            periodStart nvarchar(32)     NOT NULL,
            periodEnd   nvarchar(32)     NOT NULL,
            patients    nvarchar(max)    NOT NULL,
            lastUpdated datetime2        NOT NULL,
            UNIQUE (measureId, periodStart, periodEnd)
        );
    END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'report')
    BEGIN
        CREATE TABLE dbo.report
        (
            id            nvarchar(128)  NOT NULL PRIMARY KEY,
            measureIds    nvarchar(1024) NOT NULL,
            periodStart   nvarchar(32)   NOT NULL,
            periodEnd     nvarchar(32)   NOT NULL,
            status        nvarchar(64)   NOT NULL,
            version       nvarchar(64)   NOT NULL,
            generatedTime datetime2      NULL,
            submittedTime datetime2      NULL
        );
    END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'reportPatientList')
    BEGIN
        CREATE TABLE dbo.reportPatientList
        (
            reportId      nvarchar(128)    NOT NULL REFERENCES dbo.report (id),
            patientListId uniqueidentifier NOT NULL REFERENCES dbo.patientList (id),
            PRIMARY KEY (reportId, patientListId)
        );
    END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'query')
    BEGIN
        CREATE TABLE dbo.query
        (
            id        uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
            reportId  nvarchar(128)    NOT NULL REFERENCES dbo.report (id),
            queryType nvarchar(128)    NOT NULL,
            url       nvarchar(max)    NOT NULL,
            body      nvarchar(max)    NULL,
            retrieved datetime2        NOT NULL
        );
    END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'dataTrace')
    BEGIN
        CREATE TABLE dbo.dataTrace
        (
            id               uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
            queryId          uniqueidentifier NULL REFERENCES dbo.query (id),
            patientId        nvarchar(64)     NULL,
            resourceType     nvarchar(64)     NOT NULL,
            resourceId       nvarchar(256)    NOT NULL,
            originalResource nvarchar(max)    NOT NULL
        );
    END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'patientData')
    BEGIN
        CREATE TABLE dbo.patientData
        (
            id           uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
            patientId    nvarchar(64)     NOT NULL,
            resourceType nvarchar(64)     NOT NULL,
            resourceId   nvarchar(64)     NOT NULL,
            resource     nvarchar(max)    NOT NULL,
            retrieved    datetime2        NOT NULL,
            UNIQUE (patientId, resourceType, resourceId)
        );
    END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'patientData'
                 AND COLUMN_NAME = 'dataTraceId')
    BEGIN
        ALTER TABLE dbo.patientData
            ADD dataTraceId uniqueidentifier NULL REFERENCES dbo.dataTrace (id);
    END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'patientMeasureReport')
    BEGIN
        CREATE TABLE dbo.patientMeasureReport
        (
            id            nvarchar(128) NOT NULL PRIMARY KEY,
            reportId      nvarchar(128) NOT NULL REFERENCES dbo.report (id),
            measureId     nvarchar(64)  NOT NULL,
            patientId     nvarchar(64)  NOT NULL,
            measureReport nvarchar(max) NOT NULL,
            UNIQUE (reportId, measureId, patientId)
        );
    END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'aggregate')
    BEGIN
        CREATE TABLE dbo.[aggregate]
        (
            id        nvarchar(128) NOT NULL PRIMARY KEY,
            reportId  nvarchar(128) NOT NULL REFERENCES dbo.report (id),
            measureId nvarchar(64)  NOT NULL,
            report    nvarchar(max) NOT NULL,
            UNIQUE (reportId, measureId)
        );
    END

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'bulkStatus')
    BEGIN
        CREATE TABLE dbo.bulkStatus
        (
            id        uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
            tenantId  nvarchar(128)    NULL,
            statusUrl nvarchar(max)    NULL,
            status    nvarchar(128)    NULL,
            date      datetime2        NOT NULL             DEFAULT GETDATE()
        );
    END

GO

ALTER TABLE dbo.bulkStatus
    DROP COLUMN IF EXISTS tenantId;

GO

IF NOT EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = 'dbo'
                 AND TABLE_NAME = 'bulkStatusResult')
    BEGIN
        CREATE TABLE dbo.bulkStatusResult
        (
            id       uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
            statusId nvarchar(256)    NOT NULL,
            result   nvarchar(max)    NOT NULL
        );
    END

GO

IF (SELECT DATA_TYPE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'dbo'
      AND TABLE_NAME = 'bulkStatusResult'
      AND COLUMN_NAME = 'statusId') = 'nvarchar'
    BEGIN
        ALTER TABLE dbo.bulkStatusResult
            ALTER COLUMN statusId uniqueidentifier NULL;
        ALTER TABLE dbo.bulkStatusResult
            ADD FOREIGN KEY (statusId) REFERENCES dbo.bulkStatus (id);
    END

GO
