CREATE TABLE dbo.conceptMap
(
    id         nvarchar(128)  NOT NULL PRIMARY KEY,
    conceptMap nvarchar(max)  NOT NULL,
    contexts   nvarchar(1024) NOT NULL
);

CREATE TABLE dbo.patientList
(
    id          uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
    lastUpdated datetime2        NOT NULL,
    measureId   nvarchar(64)     NOT NULL,
    patients    nvarchar(max)    NOT NULL,
    periodEnd   datetime2        NOT NULL,
    periodStart datetime2        NOT NULL,
    UNIQUE (measureId, periodEnd, periodStart)
);

CREATE TABLE dbo.report
(
    id            nvarchar(128)  NOT NULL PRIMARY KEY,
    generatedTime datetime2      NOT NULL,
    measureIds    nvarchar(1024) NOT NULL,
    periodEnd     datetime2      NOT NULL,
    periodStart   datetime2      NOT NULL,
    status        nvarchar(64)   NOT NULL,
    version       nvarchar(64)   NOT NULL
);

CREATE TABLE dbo.reportPatientList
(
    reportId      nvarchar(128)    NOT NULL REFERENCES dbo.report (id),
    patientListId uniqueidentifier NOT NULL REFERENCES dbo.patientList (id),
    PRIMARY KEY (reportId, patientListId)
);

CREATE TABLE dbo.patientData
(
    id           uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
    patientId    nvarchar(64)     NOT NULL,
    resourceId   nvarchar(64)     NOT NULL,
    resourceType nvarchar(64)     NOT NULL,
    resource     nvarchar(max)    NOT NULL,
    retrieved    datetime2        NOT NULL,
    UNIQUE (patientId, resourceId, resourceType)
);

CREATE TABLE dbo.patientMeasureReport
(
    id            nvarchar(128) NOT NULL PRIMARY KEY,
    measureId     nvarchar(64)  NOT NULL,
    measureReport nvarchar(max) NOT NULL,
    patientId     nvarchar(64)  NOT NULL,
    periodEnd     datetime2     NOT NULL,
    periodStart   datetime2     NOT NULL,
    reportId      nvarchar(128) NOT NULL REFERENCES dbo.report (id),
    UNIQUE (measureId, patientId, periodEnd, periodStart)
);

CREATE TABLE dbo.[aggregate]
(
    id       uniqueidentifier NOT NULL PRIMARY KEY DEFAULT NEWID(),
    report   nvarchar(max)    NOT NULL,
    reportId nvarchar(128)    NOT NULL REFERENCES dbo.report (id)
);
