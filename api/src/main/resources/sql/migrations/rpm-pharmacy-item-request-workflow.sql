create table rpm_pharmacy_item_request (
    pharmacy_item_request_id int(11) not null auto_increment,
    requested_pharmacy_location_id int(11) not null,
    request_status varchar(32) not null,
    reason varchar(255) null,
    approved_by int(11) null,
    approved_date datetime null,
    creator int(11) null,
    date_created datetime null,
    voided tinyint(1) not null default 0,
    voided_by int(11) null,
    date_voided datetime null,
    void_reason varchar(255) null,
    uuid char(38) not null,
    primary key (pharmacy_item_request_id),
    key idx_rpm_pharm_item_req_status (request_status),
    key idx_rpm_pharm_item_req_pharmacy (requested_pharmacy_location_id),
    constraint fk_rpm_pharm_item_req_pharmacy
        foreign key (requested_pharmacy_location_id) references rpm_pharmacy_location(pharmacy_location_id)
);

create table rpm_pharmacy_item_request_line (
    pharmacy_item_request_line_id int(11) not null auto_increment,
    pharmacy_item_request_id int(11) not null,
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
    primary key (pharmacy_item_request_line_id),
    key idx_rpm_pharm_item_req_line_request (pharmacy_item_request_id),
    key idx_rpm_pharm_item_req_line_item (item_id),
    constraint fk_rpm_pharm_item_req_line_request
        foreign key (pharmacy_item_request_id) references rpm_pharmacy_item_request(pharmacy_item_request_id),
    constraint fk_rpm_pharm_item_req_line_item
        foreign key (item_id) references rpm_item(item_id)
);
