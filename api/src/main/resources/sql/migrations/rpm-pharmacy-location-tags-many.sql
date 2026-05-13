CREATE TABLE IF NOT EXISTS rpm_pharmacy_location_location_tag (
    pharmacy_location_id INT NOT NULL,
    location_tag_id INT NOT NULL,
    PRIMARY KEY (pharmacy_location_id, location_tag_id),
    CONSTRAINT fk_rpm_pharm_loc_tag_pharmacy
        FOREIGN KEY (pharmacy_location_id) REFERENCES rpm_pharmacy_location(pharmacy_location_id),
    CONSTRAINT fk_rpm_pharm_loc_tag_tag
        FOREIGN KEY (location_tag_id) REFERENCES location_tag(location_tag_id)
);

INSERT INTO rpm_pharmacy_location_location_tag (pharmacy_location_id, location_tag_id)
SELECT pharmacy_location_id, location_tag_id
FROM rpm_pharmacy_location
WHERE location_tag_id IS NOT NULL;
