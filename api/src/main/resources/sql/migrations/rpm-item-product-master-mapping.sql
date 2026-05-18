CREATE TABLE IF NOT EXISTS rpm_item_product_master_map (
    item_product_master_map_id INT NOT NULL AUTO_INCREMENT,
    item_id INT NOT NULL,
    product_master_item_id INT NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    uuid CHAR(38),
    PRIMARY KEY (item_product_master_map_id),
    UNIQUE INDEX rpm_item_product_master_product_uq (product_master_item_id),
    INDEX rpm_item_product_master_item_idx (item_id)
);

SET @rpm_po_product_master_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'rpm_purchase_order_item'
      AND column_name = 'product_master_item_id'
);

SET @rpm_po_product_master_sql = IF(
    @rpm_po_product_master_column_exists = 0,
    'ALTER TABLE rpm_purchase_order_item ADD COLUMN product_master_item_id INT NULL AFTER purchase_order_id',
    'SELECT 1'
);

PREPARE rpm_po_product_master_stmt FROM @rpm_po_product_master_sql;
EXECUTE rpm_po_product_master_stmt;
DEALLOCATE PREPARE rpm_po_product_master_stmt;

SET @rpm_dr_product_master_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'rpm_delivery_receipt_line'
      AND column_name = 'product_master_item_id'
);

SET @rpm_dr_product_master_sql = IF(
    @rpm_dr_product_master_column_exists = 0,
    'ALTER TABLE rpm_delivery_receipt_line ADD COLUMN product_master_item_id INT NULL AFTER item_id',
    'SELECT 1'
);

PREPARE rpm_dr_product_master_stmt FROM @rpm_dr_product_master_sql;
EXECUTE rpm_dr_product_master_stmt;
DEALLOCATE PREPARE rpm_dr_product_master_stmt;
