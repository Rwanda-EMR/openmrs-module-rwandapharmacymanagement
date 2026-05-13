CREATE TABLE IF NOT EXISTS rpm_item_category (
    item_category_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (item_category_id)
);

CREATE TABLE IF NOT EXISTS rpm_item (
    item_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL,
    item_category_id INT,
    concept_id INT,
    drug_id INT,
    dispensable TINYINT(1) NOT NULL DEFAULT 1,
    stockable TINYINT(1) NOT NULL DEFAULT 1,
    supplier_managed TINYINT(1) NOT NULL DEFAULT 0,
    low_stock_threshold INT,
    requires_batch TINYINT(1) NOT NULL DEFAULT 0,
    requires_expiry TINYINT(1) NOT NULL DEFAULT 0,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (item_id)
);

CREATE TABLE IF NOT EXISTS rpm_pharmacy_location (
    pharmacy_location_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL,
    location_id INT,
    location_type VARCHAR(50) NOT NULL,
    main_store TINYINT(1) NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (pharmacy_location_id)
);

CREATE TABLE IF NOT EXISTS rpm_supplier_setting (
    supplier_setting_id INT NOT NULL AUTO_INCREMENT,
    base_url VARCHAR(255),
    username VARCHAR(100),
    password VARCHAR(255),
    facility_id VARCHAR(100),
    connection_timeout INT,
    read_timeout INT,
    integration_enabled TINYINT(1) NOT NULL DEFAULT 0,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (supplier_setting_id)
);

CREATE TABLE IF NOT EXISTS rpm_supplier_price (
    supplier_price_id INT NOT NULL AUTO_INCREMENT,
    item_id INT,
    supplier_product_code VARCHAR(100),
    unit_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    currency VARCHAR(20),
    effective_date DATETIME,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (supplier_price_id),
    INDEX rpm_supplier_price_lookup_idx (supplier_product_code, effective_date)
);

CREATE TABLE IF NOT EXISTS rpm_supplier_product_mapping (
    supplier_product_mapping_id INT NOT NULL AUTO_INCREMENT,
    item_id INT NOT NULL,
    supplier_product_code VARCHAR(100) NOT NULL,
    supplier_product_name VARCHAR(255),
    active TINYINT(1) NOT NULL DEFAULT 1,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (supplier_product_mapping_id),
    INDEX rpm_supplier_mapping_code_idx (supplier_product_code),
    INDEX rpm_supplier_mapping_item_idx (item_id)
);

CREATE TABLE IF NOT EXISTS rpm_inventory_balance (
    inventory_balance_id INT NOT NULL AUTO_INCREMENT,
    item_id INT NOT NULL,
    pharmacy_location_id INT NOT NULL,
    batch_number VARCHAR(100),
    expiry_date DATE,
    quantity_on_hand DECIMAL(18,2) NOT NULL DEFAULT 0,
    unit_price DECIMAL(18,2),
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (inventory_balance_id),
    INDEX rpm_inventory_item_location_idx (item_id, pharmacy_location_id),
    INDEX rpm_inventory_expiry_idx (expiry_date)
);

CREATE TABLE IF NOT EXISTS rpm_stock_transaction (
    stock_transaction_id INT NOT NULL AUTO_INCREMENT,
    item_id INT NOT NULL,
    pharmacy_location_id INT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    batch_number VARCHAR(100),
    expiry_date DATE,
    quantity DECIMAL(18,2) NOT NULL,
    unit_price DECIMAL(18,2),
    total_amount DECIMAL(18,2),
    reference_number VARCHAR(100),
    remarks VARCHAR(255),
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (stock_transaction_id),
    INDEX rpm_stock_txn_item_idx (item_id, date_created)
);

CREATE TABLE IF NOT EXISTS rpm_delivery_receipt (
    delivery_receipt_id INT NOT NULL AUTO_INCREMENT,
    receipt_number VARCHAR(100) NOT NULL,
    supplier_order_id INT,
    delivery_location_id INT,
    received_into_location_id INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (delivery_receipt_id)
);

CREATE TABLE IF NOT EXISTS rpm_delivery_receipt_line (
    delivery_receipt_line_id INT NOT NULL AUTO_INCREMENT,
    delivery_receipt_id INT NOT NULL,
    item_id INT NOT NULL,
    batch_number VARCHAR(100),
    expiry_date DATE,
    ordered_quantity DECIMAL(18,2),
    received_quantity DECIMAL(18,2) NOT NULL DEFAULT 0,
    accepted_quantity DECIMAL(18,2) NOT NULL DEFAULT 0,
    unit_price DECIMAL(18,2),
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (delivery_receipt_line_id),
    INDEX rpm_delivery_receipt_line_idx (delivery_receipt_id, item_id)
);

CREATE TABLE IF NOT EXISTS rpm_patient_dispensing_request (
    patient_dispensing_request_id INT NOT NULL AUTO_INCREMENT,
    patient_id INT NOT NULL,
    visit_id INT,
    encounter_id INT,
    order_id INT,
    ordering_provider_id INT,
    target_pharmacy_location_id INT,
    payment_confirmed TINYINT(1) NOT NULL DEFAULT 0,
    request_status VARCHAR(50) NOT NULL,
    medical_act_reference VARCHAR(100),
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (patient_dispensing_request_id),
    INDEX rpm_request_queue_idx (patient_id, request_status, target_pharmacy_location_id)
);

CREATE TABLE IF NOT EXISTS rpm_patient_dispensing_request_line (
    patient_dispensing_request_line_id INT NOT NULL AUTO_INCREMENT,
    patient_dispensing_request_id INT NOT NULL,
    item_id INT,
    requested_quantity DECIMAL(18,2) NOT NULL DEFAULT 0,
    routed_pharmacy_location_id INT,
    line_status VARCHAR(50) NOT NULL,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (patient_dispensing_request_line_id),
    INDEX rpm_request_line_request_idx (patient_dispensing_request_id, item_id)
);

CREATE TABLE IF NOT EXISTS rpm_dispense_record (
    dispense_record_id INT NOT NULL AUTO_INCREMENT,
    patient_id INT,
    patient_dispensing_request_id INT,
    dispensing_pharmacy_location_id INT NOT NULL,
    provider_id INT,
    dispensed_without_order TINYINT(1) NOT NULL DEFAULT 0,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (dispense_record_id)
);

CREATE TABLE IF NOT EXISTS rpm_dispense_record_line (
    dispense_record_line_id INT NOT NULL AUTO_INCREMENT,
    dispense_record_id INT NOT NULL,
    item_id INT NOT NULL,
    batch_number VARCHAR(100),
    expiry_date DATE,
    quantity_dispensed DECIMAL(18,2) NOT NULL,
    cost_price DECIMAL(18,2),
    selling_price DECIMAL(18,2),
    line_total DECIMAL(18,2),
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (dispense_record_line_id),
    INDEX rpm_dispense_record_line_idx (dispense_record_id, item_id)
);

CREATE TABLE IF NOT EXISTS rpm_supplier_order (
    supplier_order_id INT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(100) NOT NULL,
    supplier_order_reference VARCHAR(100),
    facility_location_id INT,
    requested_by INT,
    status VARCHAR(50) NOT NULL,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (supplier_order_id),
    INDEX rpm_supplier_order_lookup_idx (order_number, status)
);

CREATE TABLE IF NOT EXISTS rpm_supplier_order_line (
    supplier_order_line_id INT NOT NULL AUTO_INCREMENT,
    supplier_order_id INT NOT NULL,
    item_id INT NOT NULL,
    ordered_quantity DECIMAL(18,2) NOT NULL DEFAULT 0,
    unit_price DECIMAL(18,2),
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (supplier_order_line_id),
    INDEX rpm_supplier_order_line_idx (supplier_order_id, item_id)
);

CREATE TABLE IF NOT EXISTS rpm_inter_pharmacy_transfer (
    inter_pharmacy_transfer_id INT NOT NULL AUTO_INCREMENT,
    source_pharmacy_location_id INT NOT NULL,
    destination_pharmacy_location_id INT NOT NULL,
    reason VARCHAR(255),
    transfer_status VARCHAR(50) NOT NULL,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (inter_pharmacy_transfer_id)
);

CREATE TABLE IF NOT EXISTS rpm_inter_pharmacy_transfer_line (
    inter_pharmacy_transfer_line_id INT NOT NULL AUTO_INCREMENT,
    inter_pharmacy_transfer_id INT NOT NULL,
    item_id INT NOT NULL,
    batch_number VARCHAR(100),
    expiry_date DATE,
    quantity DECIMAL(18,2) NOT NULL,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (inter_pharmacy_transfer_line_id),
    INDEX rpm_inter_pharmacy_transfer_line_idx (inter_pharmacy_transfer_id, item_id)
);

CREATE TABLE IF NOT EXISTS rpm_adjustment_reason (
    adjustment_reason_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active TINYINT(1) NOT NULL DEFAULT 1,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (adjustment_reason_id)
);

CREATE TABLE IF NOT EXISTS rpm_stock_adjustment (
    stock_adjustment_id INT NOT NULL AUTO_INCREMENT,
    pharmacy_location_id INT NOT NULL,
    adjustment_reason_id INT,
    comments VARCHAR(255),
    adjusted_by INT,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (stock_adjustment_id)
);

CREATE TABLE IF NOT EXISTS rpm_stock_adjustment_line (
    stock_adjustment_line_id INT NOT NULL AUTO_INCREMENT,
    stock_adjustment_id INT NOT NULL,
    item_id INT NOT NULL,
    batch_number VARCHAR(100),
    expiry_date DATE,
    quantity DECIMAL(18,2) NOT NULL DEFAULT 0,
    system_quantity DECIMAL(18,2),
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (stock_adjustment_line_id),
    INDEX rpm_stock_adjustment_line_idx (stock_adjustment_id, item_id)
);

CREATE TABLE IF NOT EXISTS rpm_clinical_safety_rule (
    clinical_safety_rule_id INT NOT NULL AUTO_INCREMENT,
    rule_type VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    blocking TINYINT(1) NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    rule_name VARCHAR(255) NOT NULL,
    drug_id INT,
    interacting_drug_id INT,
    contraindicated_condition_id INT,
    gender_restriction VARCHAR(10),
    rule_definition TEXT,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (clinical_safety_rule_id)
);

CREATE TABLE IF NOT EXISTS rpm_integration_log (
    integration_log_id INT NOT NULL AUTO_INCREMENT,
    integration_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    endpoint VARCHAR(255),
    request_payload TEXT,
    response_payload TEXT,
    summary VARCHAR(255),
    duration_ms BIGINT,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (integration_log_id),
    INDEX rpm_integration_log_idx (integration_type, status, date_created)
);
