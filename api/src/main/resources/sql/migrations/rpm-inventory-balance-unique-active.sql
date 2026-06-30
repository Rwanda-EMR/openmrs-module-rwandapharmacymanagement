CREATE TEMPORARY TABLE rpm_inventory_balance_duplicate_groups AS
SELECT
    MIN(inventory_balance_id) AS keep_inventory_balance_id,
    item_id,
    pharmacy_location_id,
    COALESCE(batch_number, '') AS batch_number_key,
    SUM(COALESCE(quantity_on_hand, 0)) AS total_quantity_on_hand,
    MIN(expiry_date) AS merged_expiry_date,
    MAX(unit_price) AS merged_unit_price
FROM rpm_inventory_balance
WHERE COALESCE(voided, 0) = 0
GROUP BY item_id, pharmacy_location_id, COALESCE(batch_number, '')
HAVING COUNT(*) > 1;

CREATE TEMPORARY TABLE rpm_inventory_balance_duplicate_rows AS
SELECT b.inventory_balance_id, g.keep_inventory_balance_id
FROM rpm_inventory_balance b
INNER JOIN rpm_inventory_balance_duplicate_groups g
    ON g.item_id = b.item_id
   AND g.pharmacy_location_id = b.pharmacy_location_id
   AND g.batch_number_key = COALESCE(b.batch_number, '')
WHERE COALESCE(b.voided, 0) = 0
  AND b.inventory_balance_id <> g.keep_inventory_balance_id;

UPDATE rpm_inventory_balance b
INNER JOIN rpm_inventory_balance_duplicate_groups g
    ON g.keep_inventory_balance_id = b.inventory_balance_id
SET b.quantity_on_hand = g.total_quantity_on_hand,
    b.expiry_date = COALESCE(b.expiry_date, g.merged_expiry_date),
    b.unit_price = COALESCE(b.unit_price, g.merged_unit_price),
    b.voided = 0;

UPDATE rpm_inventory_balance b
INNER JOIN rpm_inventory_balance_duplicate_rows d
    ON d.inventory_balance_id = b.inventory_balance_id
SET b.quantity_on_hand = 0,
    b.voided = 1,
    b.date_voided = NOW(),
    b.void_reason = 'Merged duplicate inventory balance';

DROP TEMPORARY TABLE rpm_inventory_balance_duplicate_rows;

DROP TEMPORARY TABLE rpm_inventory_balance_duplicate_groups;

ALTER TABLE rpm_inventory_balance
    ADD COLUMN batch_number_key VARCHAR(100)
        GENERATED ALWAYS AS (COALESCE(batch_number, '')) STORED,
    ADD COLUMN active_balance_key INT
        GENERATED ALWAYS AS (CASE WHEN COALESCE(voided, 0) = 0 THEN 0 ELSE NULL END) STORED;

CREATE UNIQUE INDEX rpm_inventory_balance_active_unique_idx
    ON rpm_inventory_balance (item_id, pharmacy_location_id, batch_number_key, active_balance_key);
