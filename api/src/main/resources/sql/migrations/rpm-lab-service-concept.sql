alter table rpm_lab_supply_request_line
    add column lab_service_concept_id int(11) null after item_id,
    add key idx_rpm_lab_request_line_service (lab_service_concept_id);
