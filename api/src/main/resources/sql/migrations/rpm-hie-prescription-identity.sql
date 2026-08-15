ALTER TABLE rpm_patient_dispensing_request
    ADD COLUMN hie_prescription_group_id VARCHAR(255);

ALTER TABLE rpm_patient_dispensing_request_line
    ADD COLUMN hie_medication_request_id VARCHAR(255);

UPDATE rpm_patient_dispensing_request
SET hie_prescription_group_id = CONCAT('EP-unknown-', patient_dispensing_request_id)
WHERE request_type = 'PRESCRIPTION'
  AND (hie_prescription_group_id IS NULL OR hie_prescription_group_id = '');

UPDATE rpm_patient_dispensing_request_line
SET hie_medication_request_id = uuid
WHERE uuid IS NOT NULL
  AND uuid <> ''
  AND (hie_medication_request_id IS NULL OR hie_medication_request_id = '');

CREATE INDEX rpm_request_hie_group_idx
    ON rpm_patient_dispensing_request (hie_prescription_group_id);

CREATE INDEX rpm_request_line_hie_medication_idx
    ON rpm_patient_dispensing_request_line (hie_medication_request_id);
