ALTER TABLE rpm_patient_dispensing_request_line
    ADD COLUMN order_id INT NULL AFTER patient_dispensing_request_id;

CREATE INDEX rpm_request_line_order_idx
    ON rpm_patient_dispensing_request_line (order_id);
