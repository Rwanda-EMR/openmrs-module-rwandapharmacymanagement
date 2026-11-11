ALTER TABLE rpm_integration_log
    ADD COLUMN reference_id INT,
    MODIFY COLUMN request_payload MEDIUMTEXT;

CREATE INDEX rpm_integration_log_status_type_reference_idx
    ON rpm_integration_log (status, integration_type, reference_id);
