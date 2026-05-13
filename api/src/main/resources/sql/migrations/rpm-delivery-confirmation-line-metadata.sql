ALTER TABLE rpm_delivery_receipt_line
    ADD COLUMN confirmation_item_no VARCHAR(50) NULL,
    ADD COLUMN rejection_reason_code VARCHAR(50) NULL,
    ADD COLUMN rejection_reason_detail TEXT NULL,
    ADD COLUMN notes TEXT NULL;
