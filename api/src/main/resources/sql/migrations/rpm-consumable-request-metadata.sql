alter table rpm_patient_dispensing_request
    add column request_type varchar(32) not null default 'PRESCRIPTION';

alter table rpm_patient_dispensing_request
    add column billable tinyint(1) not null default 1;

alter table rpm_patient_dispensing_request_line
    add column unit_of_measure varchar(64) null;

update rpm_patient_dispensing_request
set request_type = 'PRESCRIPTION'
where request_type is null or trim(request_type) = '';

update rpm_patient_dispensing_request
set billable = 1
where billable is null;
