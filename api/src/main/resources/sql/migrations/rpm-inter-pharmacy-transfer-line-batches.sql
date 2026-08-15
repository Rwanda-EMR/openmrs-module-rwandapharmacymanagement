CREATE TABLE IF NOT EXISTS rpm_inter_pharmacy_transfer_line_batch (
    inter_pharmacy_transfer_line_batch_id INT NOT NULL AUTO_INCREMENT,
    inter_pharmacy_transfer_line_id INT NOT NULL,
    batch_number VARCHAR(100),
    expiry_date DATE,
    quantity DECIMAL(18,2) NOT NULL,
    unit_price DECIMAL(18,2),
    total_amount DECIMAL(18,2),
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (inter_pharmacy_transfer_line_batch_id),
    INDEX rpm_transfer_line_batch_line_idx (inter_pharmacy_transfer_line_id),
    INDEX rpm_transfer_line_batch_lookup_idx (batch_number, voided)
);
