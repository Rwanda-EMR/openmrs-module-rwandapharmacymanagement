alter table rpm_patient_dispensing_request
    add column requesting_location_id int(11) null;

alter table rpm_patient_dispensing_request
    add index idx_rpm_requesting_location (requesting_location_id);
