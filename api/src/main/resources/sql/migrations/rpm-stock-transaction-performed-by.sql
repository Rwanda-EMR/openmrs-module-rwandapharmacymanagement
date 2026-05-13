SET @performed_by_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'rpm_stock_transaction'
      AND COLUMN_NAME = 'performed_by'
);

SET @performed_by_sql := IF(
    @performed_by_exists = 0,
    'ALTER TABLE rpm_stock_transaction ADD COLUMN performed_by INT NULL AFTER remarks',
    'SELECT 1'
);

PREPARE performed_by_stmt FROM @performed_by_sql;
EXECUTE performed_by_stmt;
DEALLOCATE PREPARE performed_by_stmt;
