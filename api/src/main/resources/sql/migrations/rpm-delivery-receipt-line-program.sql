SET @rpm_dr_program_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'rpm_delivery_receipt_line'
      AND column_name = 'program'
);

SET @rpm_dr_program_sql = IF(
    @rpm_dr_program_column_exists = 0,
    'ALTER TABLE rpm_delivery_receipt_line ADD COLUMN program VARCHAR(100) NULL AFTER product_master_item_id',
    'SELECT 1'
);

PREPARE rpm_dr_program_stmt FROM @rpm_dr_program_sql;
EXECUTE rpm_dr_program_stmt;
DEALLOCATE PREPARE rpm_dr_program_stmt;
