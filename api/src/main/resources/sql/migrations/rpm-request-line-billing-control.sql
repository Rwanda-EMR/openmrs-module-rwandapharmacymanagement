alter table rpm_patient_dispensing_request_line
    add column billable tinyint(1) not null default 1;

alter table rpm_patient_dispensing_request_line
    add column billing_exclusion_reason varchar(255) null;

update rpm_patient_dispensing_request_line
set billable = 1
where billable is null;
