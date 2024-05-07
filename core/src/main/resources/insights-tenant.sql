DECLARE @ReportId AS nvarchar(128) = ?;

-- Patient lists

SELECT measureId,
       periodStart,
       periodEnd,
       (SELECT COUNT(*) FROM OPENJSON(patients)) AS patients
FROM dbo.patientList AS PL
         INNER JOIN dbo.reportPatientList AS RPL ON PL.id = RPL.patientListId
WHERE RPL.reportId = @ReportId
ORDER BY measureId;

-- Individual measure reports

SELECT COUNT(*) AS [count]
FROM dbo.patientMeasureReport
WHERE reportId = @ReportId;

-- Aggregate measure reports

SELECT measureId,
       (SELECT COUNT(*) FROM OPENJSON(report, '$.contained[0].entry')) AS ipPatients,
       JSON_VALUE(report, '$.group[0].population[0].count')            AS ipEncounters
FROM dbo.[aggregate]
WHERE reportId = @ReportId;

-- Patient data

SELECT PD.resourceType,
       COUNT(*) AS [count]
FROM dbo.patientData AS PD
         INNER JOIN dbo.reportPatientData AS RPD ON
    PD.patientId = RPD.patientId
        AND PD.resourceType = RPD.resourceType
        AND PD.resourceId = RPD.resourceId
WHERE RPD.reportId = @ReportId
GROUP BY PD.resourceType
ORDER BY PD.resourceType;
