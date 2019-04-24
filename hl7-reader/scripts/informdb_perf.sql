CREATE TEMPORARY VIEW PROCESSING_TIMES AS
SELECT
message_type,
processing_start_time,
processing_end_time,
(processing_end_time-processing_start_time) AS processing_time
FROM ids_effect_logging
;

-- message processing time by message type
SELECT
message_type,
COUNT(*),
MIN(processing_time),
AVG(processing_time),
MAX(processing_time)
FROM PROCESSING_TIMES
GROUP BY message_type
ORDER BY avg DESC
;

-- total message processing time by message type
SELECT
message_type,
sum(processing_time)
FROM PROCESSING_TIMES
GROUP BY message_type
ORDER BY sum(processing_time) desc
;

-- total processing time for all messages
SELECT
sum(processing_time)
FROM PROCESSING_TIMES
;
