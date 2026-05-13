create table rpm_lab_supply_request (
    lab_supply_request_id int(11) not null auto_increment,
    lab_unit_location_id int(11) not null,
    dispensing_pharmacy_location_id int(11) null,
    request_status varchar(32) not null,
    approved_by int(11) null,
    approved_date datetime null,
    creator int(11) null,
    date_created datetime null,
    voided tinyint(1) not null default 0,
    voided_by int(11) null,
    date_voided datetime null,
    void_reason varchar(255) null,
    uuid char(38) not null,
    primary key (lab_supply_request_id),
    key idx_rpm_lab_request_status (request_status),
    key idx_rpm_lab_request_unit (lab_unit_location_id),
    key idx_rpm_lab_request_dispensing_pharmacy (dispensing_pharmacy_location_id)
);

create table rpm_lab_supply_request_line (
    lab_supply_request_line_id int(11) not null auto_increment,
    lab_supply_request_id int(11) not null,
    item_id int(11) not null,
    requested_quantity decimal(18,2) not null,
    approved_quantity decimal(18,2) null,
    creator int(11) null,
    date_created datetime null,
    voided tinyint(1) not null default 0,
    voided_by int(11) null,
    date_voided datetime null,
    void_reason varchar(255) null,
    uuid char(38) not null,
    primary key (lab_supply_request_line_id),
    key idx_rpm_lab_request_line_request (lab_supply_request_id),
    key idx_rpm_lab_request_line_item (item_id)
);
