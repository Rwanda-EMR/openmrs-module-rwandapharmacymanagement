ALTER TABLE rpm_patient_dispensing_request
    ADD COLUMN payment_exception_approved TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN payment_exception_type VARCHAR(50),
    ADD COLUMN payment_exception_reason VARCHAR(255),
    ADD COLUMN payment_exception_note TEXT,
    ADD COLUMN payment_exception_approved_by INT,
    ADD COLUMN payment_exception_approved_date DATETIME;

CREATE INDEX rpm_patient_dispensing_request_payment_exception_idx
    ON rpm_patient_dispensing_request (payment_exception_approved, payment_exception_type);
