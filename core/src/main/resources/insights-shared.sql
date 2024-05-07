DECLARE @TenantId AS nvarchar(128) = ?;
DECLARE @ReportId AS nvarchar(128) = ?;
DECLARE @Version AS nvarchar(64) = ?;

-- Log messages

SELECT LE.level_string AS [level],
       COUNT(*)        AS [count]
FROM dbo.logging_event AS LE
         INNER JOIN dbo.logging_event_property AS LEP ON LE.event_id = LEP.event_id
WHERE LEP.mapped_key = 'report'
  AND LEP.mapped_value = @TenantId + '-' + @ReportId + '-' + @Version
GROUP BY LE.level_string
ORDER BY CASE LE.level_string
             WHEN 'ERROR' THEN 1
             WHEN 'WARN' THEN 2
             WHEN 'INFO' THEN 3
             WHEN 'DEBUG' THEN 4
             ELSE 5 END;

-- Errors and warnings

SELECT LE.level_string      AS [level],
       COUNT(*)             AS [count],
       LE.formatted_message AS message
FROM dbo.logging_event AS LE
         INNER JOIN dbo.logging_event_property AS LEP ON LE.event_id = LEP.event_id
WHERE LEP.mapped_key = 'report'
  AND LEP.mapped_value = @TenantId + '-' + @ReportId + '-' + @Version
  AND LE.level_string IN ('ERROR', 'WARN')
GROUP BY LE.level_string, LE.formatted_message
ORDER BY CASE LE.level_string
             WHEN 'ERROR' THEN 1
             WHEN 'WARN' THEN 2
             ELSE 3 END,
         MIN(LE.timestmp);

-- Metrics

SELECT category + '-' + taskName           AS task,
       [count],
       FORMAT(duration, '0.000')           AS totalSeconds,
       FORMAT(duration / [count], '0.000') AS meanSeconds
FROM (SELECT *,
             CAST(JSON_VALUE(data, '$.count') AS int)             AS [count],
             CAST(JSON_VALUE(data, '$.duration') AS int) / 1000.0 AS duration
      FROM dbo.metrics) AS M
WHERE tenantId = @TenantId
  AND reportId = @ReportId
  AND version = @Version
ORDER BY category, taskName;
