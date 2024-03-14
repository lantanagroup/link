SELECT r.id,
       r.[version],
       r.[status],
       r.generatedTime,
       r.submittedTime,
       r.periodStart,
       r.periodEnd,
       r.measureIds,
       counts.totalPatients,
       counts.maxTotalInIP
FROM dbo.report r
         LEFT JOIN (SELECT R.id,
                           MAX(PL.patientCount)  AS totalPatients,
                           MAX(A.ipPatientCount) AS maxTotalInIP
                    FROM dbo.report AS R
                             INNER JOIN dbo.reportPatientList AS RPL ON R.id = RPL.reportId
                             INNER JOIN (SELECT *, (SELECT COUNT(*) FROM OPENJSON(patients)) AS patientCount
                                         FROM dbo.patientList) AS PL ON RPL.patientListId = PL.id
                             INNER JOIN (SELECT *,
                                                (SELECT COUNT(*) FROM OPENJSON(report, '$.contained[0].entry')) AS ipPatientCount
                                         FROM dbo.[aggregate]) AS A ON R.id = A.reportId
                    GROUP BY R.id) AS counts ON r.id = counts.id