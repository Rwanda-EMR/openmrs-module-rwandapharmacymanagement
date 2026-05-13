ALTER TABLE rpm_purchase_order
    ADD COLUMN fetched_receipt_payload TEXT NULL AFTER dispatch_reference,
    ADD COLUMN fetched_receipt_date DATETIME NULL AFTER fetched_receipt_payload;
