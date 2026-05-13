ALTER TABLE rpm_pharmacy_location
    ADD COLUMN location_tag_id INT NULL;

ALTER TABLE rpm_pharmacy_location
    ADD CONSTRAINT fk_rpm_pharmacy_location_location_tag
        FOREIGN KEY (location_tag_id) REFERENCES location_tag(location_tag_id);
