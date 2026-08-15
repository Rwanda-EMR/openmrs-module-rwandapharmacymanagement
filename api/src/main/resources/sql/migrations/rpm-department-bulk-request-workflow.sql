alter table rpm_pharmacy_location
    modify column location_type varchar(64);

create table rpm_department_bulk_request (
    department_bulk_request_id int(11) not null auto_increment,
    department_location_id int(11) not null,
    supplying_pharmacy_location_id int(11) not null,
    request_status varchar(32) not null,
    workflow_mode varchar(32) not null,
    justification varchar(255) null,
    correction_note varchar(255) null,
    finance_approved_by int(11) null,
    finance_approved_date datetime null,
    dispensed_by int(11) null,
    dispensed_date datetime null,
    creator int(11) null,
    date_created datetime null,
    voided tinyint(1) not null default 0,
    voided_by int(11) null,
    date_voided datetime null,
    void_reason varchar(255) null,
    uuid char(38) not null,
    primary key (department_bulk_request_id),
    key idx_rpm_dept_bulk_status (request_status),
    key idx_rpm_dept_bulk_department (department_location_id),
    key idx_rpm_dept_bulk_supply (supplying_pharmacy_location_id),
    constraint fk_rpm_dept_bulk_department
        foreign key (department_location_id) references rpm_pharmacy_location(pharmacy_location_id),
    constraint fk_rpm_dept_bulk_supply
        foreign key (supplying_pharmacy_location_id) references rpm_pharmacy_location(pharmacy_location_id)
);

create table rpm_department_bulk_request_line (
    department_bulk_request_line_id int(11) not null auto_increment,
    department_bulk_request_id int(11) not null,
    item_id int(11) not null,
    previous_requested_quantity decimal(18,2) null,
    previous_received_quantity decimal(18,2) null,
    previous_used_quantity decimal(18,2) null,
    current_remaining_quantity decimal(18,2) null,
    requested_quantity decimal(18,2) not null,
    approved_quantity decimal(18,2) null,
    dispensed_quantity decimal(18,2) null,
    batch_number varchar(255) null,
    line_justification varchar(255) null,
    creator int(11) null,
    date_created datetime null,
    voided tinyint(1) not null default 0,
    voided_by int(11) null,
    date_voided datetime null,
    void_reason varchar(255) null,
    uuid char(38) not null,
    primary key (department_bulk_request_line_id),
    key idx_rpm_dept_bulk_line_request (department_bulk_request_id),
    key idx_rpm_dept_bulk_line_item (item_id),
    constraint fk_rpm_dept_bulk_line_request
        foreign key (department_bulk_request_id) references rpm_department_bulk_request(department_bulk_request_id),
    constraint fk_rpm_dept_bulk_line_item
        foreign key (item_id) references rpm_item(item_id)
);

create table rpm_department_stock_issue (
    department_stock_issue_id int(11) not null auto_increment,
    department_location_id int(11) not null,
    item_id int(11) not null,
    quantity decimal(18,2) not null,
    batch_number varchar(255) null,
    issued_to varchar(255) null,
    reason varchar(255) null,
    issued_by int(11) null,
    creator int(11) null,
    date_created datetime null,
    voided tinyint(1) not null default 0,
    voided_by int(11) null,
    date_voided datetime null,
    void_reason varchar(255) null,
    uuid char(38) not null,
    primary key (department_stock_issue_id),
    key idx_rpm_dept_issue_department (department_location_id),
    key idx_rpm_dept_issue_item (item_id),
    constraint fk_rpm_dept_issue_department
        foreign key (department_location_id) references rpm_pharmacy_location(pharmacy_location_id),
    constraint fk_rpm_dept_issue_item
        foreign key (item_id) references rpm_item(item_id)
);
