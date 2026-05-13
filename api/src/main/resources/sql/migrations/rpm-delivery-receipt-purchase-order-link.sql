ALTER TABLE rpm_delivery_receipt
    ADD COLUMN purchase_order_id INT NULL AFTER receipt_number;

CREATE INDEX rpm_delivery_receipt_purchase_order_idx
    ON rpm_delivery_receipt (purchase_order_id);
