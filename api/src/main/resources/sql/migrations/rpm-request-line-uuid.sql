ALTER TABLE rpm_patient_dispensing_request_line
    ADD COLUMN uuid CHAR(38);

UPDATE rpm_patient_dispensing_request_line
SET uuid = UUID()
WHERE uuid IS NULL OR uuid = '';

ALTER TABLE rpm_patient_dispensing_request_line
    MODIFY uuid CHAR(38) NOT NULL;

CREATE UNIQUE INDEX rpm_request_line_uuid_idx
    ON rpm_patient_dispensing_request_line (uuid);
