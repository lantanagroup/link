SELECT r.id,
       r.[version],
       r.[status],
       r.generatedTime,
       r.submittedTime,
       r.periodStart,
       r.periodEnd,
       r.measureIds,
       CASE
           WHEN pl.patients IS NOT NULL THEN (SELECT COUNT(*) FROM OPENJSON(pl.patients))
           ELSE NULL
           END AS totalPatients,
       t.maxTotalInIP
FROM [dbo].[report] r
         CROSS APPLY OPENJSON(r.measureIds) AS mids
         LEFT JOIN [dbo].[patientList] pl
                   ON pl.measureId = mids.value AND r.periodStart = pl.periodStart AND r.periodEnd = pl.periodEnd
         LEFT JOIN (SELECT a.reportId, MAX(a.totalInIP) AS maxTotalInIP
                    FROM (SELECT reportId, (SELECT COUNT(*) FROM OPENJSON(report, '$.contained[0].entry')) AS totalInIP
                          from [dbo].[aggregate]) a
                    GROUP BY a.reportId) t ON t.reportId = r.id