ALTER TABLE rpm_inter_pharmacy_transfer_line
    ADD COLUMN unit_price DECIMAL(18,2) NULL AFTER quantity;

ALTER TABLE rpm_inter_pharmacy_transfer_line
    ADD COLUMN total_amount DECIMAL(18,2) NULL AFTER unit_price;
