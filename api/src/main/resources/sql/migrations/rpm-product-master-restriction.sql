ALTER TABLE rpm_product_master_item
    ADD COLUMN required_privilege VARCHAR(255) NULL,
    ADD COLUMN restriction_reason VARCHAR(255) NULL;

CREATE INDEX rpm_product_master_required_privilege_idx ON rpm_product_master_item(required_privilege);
