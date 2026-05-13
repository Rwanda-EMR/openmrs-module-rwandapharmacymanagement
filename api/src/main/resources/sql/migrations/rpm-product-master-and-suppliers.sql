CREATE TABLE IF NOT EXISTS rpm_product_master_item (
    product_master_item_id INT NOT NULL AUTO_INCREMENT,
    item_name VARCHAR(255) NOT NULL,
    npc_code VARCHAR(100) NOT NULL,
    program VARCHAR(100),
    material_type VARCHAR(100),
    item_price DECIMAL(18,2),
    active TINYINT(1) NOT NULL DEFAULT 1,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (product_master_item_id),
    UNIQUE INDEX rpm_product_master_npc_idx (npc_code)
);

CREATE TABLE IF NOT EXISTS rpm_supplier (
    supplier_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL,
    email_address VARCHAR(255),
    api_enabled TINYINT(1) NOT NULL DEFAULT 0,
    api_base_url VARCHAR(255),
    api_username VARCHAR(255),
    api_password VARCHAR(255),
    facility_id VARCHAR(100),
    connection_timeout INT,
    read_timeout INT,
    active TINYINT(1) NOT NULL DEFAULT 1,
    creator INT,
    date_created DATETIME,
    voided TINYINT(1) NOT NULL DEFAULT 0,
    voided_by INT,
    date_voided DATETIME,
    void_reason VARCHAR(255),
    PRIMARY KEY (supplier_id),
    UNIQUE INDEX rpm_supplier_code_idx (code)
);

ALTER TABLE rpm_supplier_order
    ADD COLUMN supplier_id INT NULL AFTER supplier_order_reference;

ALTER TABLE rpm_supplier_order_line
    MODIFY COLUMN item_id INT NULL,
    ADD COLUMN supplier_product_code VARCHAR(100) NULL AFTER item_id,
    ADD COLUMN supplier_product_name VARCHAR(255) NULL AFTER supplier_product_code,
    ADD COLUMN program_name VARCHAR(100) NULL AFTER supplier_product_name;

INSERT INTO rpm_supplier (
    name, code, email_address, api_enabled, api_base_url, api_username, api_password, facility_id,
    connection_timeout, read_timeout, active, creator, date_created, voided
)
SELECT
    'RMS Main Supplier',
    'RMS',
    NULL,
    COALESCE(integration_enabled, 0),
    base_url,
    username,
    password,
    facility_id,
    connection_timeout,
    read_timeout,
    1,
    creator,
    COALESCE(date_created, NOW()),
    0
FROM rpm_supplier_setting
WHERE NOT EXISTS (
    SELECT 1 FROM rpm_supplier WHERE code = 'RMS'
)
LIMIT 1;
