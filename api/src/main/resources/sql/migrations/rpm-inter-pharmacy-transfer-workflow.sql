ALTER TABLE rpm_inter_pharmacy_transfer
    ADD COLUMN approved_by INT NULL AFTER transfer_status;

ALTER TABLE rpm_inter_pharmacy_transfer
    ADD COLUMN approved_date DATETIME NULL AFTER approved_by;

ALTER TABLE rpm_inter_pharmacy_transfer
    ADD COLUMN rejected_by INT NULL AFTER approved_date;

ALTER TABLE rpm_inter_pharmacy_transfer
    ADD COLUMN rejected_date DATETIME NULL AFTER rejected_by;

ALTER TABLE rpm_inter_pharmacy_transfer
    ADD COLUMN rejection_reason VARCHAR(255) NULL AFTER rejected_date;

ALTER TABLE rpm_inter_pharmacy_transfer
    ADD COLUMN served_by INT NULL AFTER rejection_reason;

ALTER TABLE rpm_inter_pharmacy_transfer
    ADD COLUMN served_date DATETIME NULL AFTER served_by;
