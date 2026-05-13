ALTER TABLE rpm_item
    ADD COLUMN program VARCHAR(100) NULL AFTER code;

ALTER TABLE rpm_supplier_price
    ADD COLUMN program VARCHAR(100) NULL AFTER supplier_product_code;

CREATE INDEX rpm_item_code_program_idx
    ON rpm_item (code, program);

DROP INDEX rpm_product_master_npc_idx ON rpm_product_master_item;

CREATE INDEX rpm_product_master_npc_idx
    ON rpm_product_master_item (npc_code);

CREATE UNIQUE INDEX rpm_product_master_npc_program_idx
    ON rpm_product_master_item (npc_code, program);

CREATE INDEX rpm_supplier_price_code_program_idx
    ON rpm_supplier_price (supplier_product_code, program, effective_date);
