ALTER TABLE rpm_product_master_item
    ADD COLUMN drug_id INT NULL AFTER item_price;

CREATE INDEX rpm_product_master_drug_idx
    ON rpm_product_master_item (drug_id);
