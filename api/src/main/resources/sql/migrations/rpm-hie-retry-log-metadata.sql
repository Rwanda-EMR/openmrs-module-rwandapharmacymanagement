ALTER TABLE rpm_integration_log
    ADD COLUMN retry_count INT DEFAULT 0,
    ADD COLUMN last_attempt_date DATETIME;

UPDATE rpm_integration_log
SET retry_count = 0
WHERE retry_count IS NULL;
