ALTER TABLE rpm_item ADD COLUMN visible_to_requesters TINYINT(1) DEFAULT 1;
UPDATE rpm_item SET visible_to_requesters = 1 WHERE visible_to_requesters IS NULL;
