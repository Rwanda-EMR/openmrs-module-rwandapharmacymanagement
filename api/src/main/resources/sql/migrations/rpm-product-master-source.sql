ALTER TABLE rpm_product_master_item
    ADD COLUMN source VARCHAR(40) NULL AFTER active;

UPDATE rpm_product_master_item
SET source = CASE
    WHEN npc_code IS NOT NULL AND UPPER(npc_code) LIKE 'LOCAL-%' THEN 'MANUAL'
    ELSE 'API_SYNCED'
END
WHERE source IS NULL OR source = '';

ALTER TABLE rpm_product_master_item
    MODIFY COLUMN source VARCHAR(40) NOT NULL DEFAULT 'MANUAL';

CREATE INDEX rpm_product_master_source_idx
    ON rpm_product_master_item (source, active);
