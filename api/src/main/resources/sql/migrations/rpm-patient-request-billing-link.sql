SET @rpm_add_patient_bill_id := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'rpm_patient_dispensing_request'
              AND column_name = 'patient_bill_id'
        ),
        'SELECT 1',
        'ALTER TABLE rpm_patient_dispensing_request ADD COLUMN patient_bill_id INT NULL, ADD INDEX rpm_request_patient_bill_idx (patient_bill_id)'
    )
);
PREPARE rpm_stmt FROM @rpm_add_patient_bill_id;
EXECUTE rpm_stmt;
DEALLOCATE PREPARE rpm_stmt;
