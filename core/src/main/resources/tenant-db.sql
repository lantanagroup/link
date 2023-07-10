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
