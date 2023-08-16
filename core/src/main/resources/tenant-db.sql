IF OBJECT_ID(N'dbo.conceptMap', N'U') IS NULL
CREATE TABLE dbo.conceptMap
(
    id         nvarchar(128)  NOT NULL PRIMARY KEY,
    contexts   nvarchar(1024) NOT NULL,
    conceptMap nvarchar(max)  NOT NULL
);

IF OBJECT_ID(N'dbo.patientList', N'U') IS NULL
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

IF OBJECT_ID(N'dbo.report', N'U') IS NULL
CREATE TABLE dbo.report
(
    id            nvarchar(128)  NOT NULL PRIMARY KEY,
    measureIds    nvarchar(1024) NOT NULL,
    periodStart   nvarchar(32)   NOT NULL,
    periodEnd     nvarchar(32)   NOT NULL,
    status        nvarchar(64)   NOT NULL,
    version       nvarchar(64)   NOT NULL,
    generatedTime datetime2,
    submittedTime datetime2
);

IF OBJECT_ID(N'dbo.reportPatientList', N'U') IS NULL
CREATE TABLE dbo.reportPatientList
(
    reportId      nvarchar(128)    NOT NULL REFERENCES dbo.report (id),
    patientListId uniqueidentifier NOT NULL REFERENCES dbo.patientList (id),
    PRIMARY KEY (reportId, patientListId)
);

IF OBJECT_ID(N'dbo.patientData', N'U') IS NULL
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

IF OBJECT_ID(N'dbo.patientMeasureReport', N'U') IS NULL
CREATE TABLE dbo.patientMeasureReport
(
    id            nvarchar(128) NOT NULL PRIMARY KEY,
    reportId      nvarchar(128) NOT NULL REFERENCES dbo.report (id),
    measureId     nvarchar(64)  NOT NULL,
    patientId     nvarchar(64)  NOT NULL,
    measureReport nvarchar(max) NOT NULL,
    UNIQUE (reportId, measureId, patientId)
);

IF OBJECT_ID(N'dbo.[aggregate]', N'U') IS NULL
CREATE TABLE dbo.[aggregate]
(
    id        nvarchar(128) NOT NULL PRIMARY KEY,
    reportId  nvarchar(128) NOT NULL REFERENCES dbo.report (id),
    measureId nvarchar(64)  NOT NULL,
    report    nvarchar(max) NOT NULL,
    UNIQUE (reportId, measureId)
);

IF OBJECT_ID(N'[dbo].[bulkStatus]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[bulkStatus](
        [id] [uniqueidentifier] NOT NULL,
        [tenantId] [nvarchar](128) NULL,
        [statusUrl] [nvarchar](max) NULL,
        [status] [nvarchar](128) NULL,
        [date] [datetime2] NOT NULL
        PRIMARY KEY CLUSTERED
    (
    [id] ASC
    )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
        ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

    ALTER TABLE [dbo].[bulkStatus] ADD  DEFAULT (newid()) FOR [id]
    ALTER TABLE [dbo].[bulkStatus] ADD  DEFAULT (getdate()) FOR [date]
    ALTER TABLE [dbo].[bulkStatus]  WITH CHECK ADD FOREIGN KEY([tenantId])
    REFERENCES [dbo].[tenantConfig] ([id])
END

IF OBJECT_ID(N'[dbo].[bulkStatusResult]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[bulkStatusResult](
        [id] [uniqueidentifier] NOT NULL,
        [statusId] [nvarchar](256) NOT NULL,
        [result] [nvarchar](max) NOT NULL,
        PRIMARY KEY CLUSTERED
    (
    [id] ASC
    )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
        ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

    ALTER TABLE [dbo].[bulkStatusResult] ADD  DEFAULT (newid()) FOR [id]
END

DROP PROCEDURE IF EXISTS saveBulkStatusResult
GO

CREATE PROCEDURE [dbo].[saveBulkStatusResult]
    @id NVARCHAR(64),
    @statusId NVARCHAR(256),
    @result NVARCHAR(MAX)
AS
BEGIN
    IF NOT EXISTS(SELECT * FROM dbo.bulkStatusResult t WHERE t.id = @id)
    BEGIN
        INSERT INTO dbo.bulkStatusResult(statusId, result)
        VALUES(@statusId, @result)
    END
    ELSE
        BEGIN
            UPDATE dbo.bulkStatusResult
            SET statusId = @statusId, result = @result
            WHERE id = @id
        END
    END
GO

DROP PROCEDURE IF EXISTS saveBulkStatus
GO

CREATE PROCEDURE [dbo].[saveBulkStatus]
    @id NVARCHAR(64),
    @tenantId NVARCHAR(128),
    @statusUrl NVARCHAR(MAX),
    @status NVARCHAR(128),
    @date DateTime2(7)
AS
BEGIN
    IF NOT EXISTS(SELECT * FROM dbo.bulkStatus t WHERE t.id = @id)
    BEGIN
        INSERT INTO dbo.bulkStatus(tenantId, statusUrl, [status], [date])
        VALUES(@tenantId, @statusUrl, @status, @date)
    END
    ELSE
        BEGIN
            UPDATE dbo.bulkStatus
            SET tenantId = @tenantId, statusUrl = @statusUrl, [status] = @status, [date] = @date
            WHERE id = @id
        END
    END
GO