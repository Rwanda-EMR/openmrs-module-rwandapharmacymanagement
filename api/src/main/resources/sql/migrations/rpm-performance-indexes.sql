CREATE INDEX rpm_stock_txn_pharmacy_date_idx
    ON rpm_stock_transaction (pharmacy_location_id, date_created);

CREATE INDEX rpm_stock_txn_item_pharmacy_date_idx
    ON rpm_stock_transaction (item_id, pharmacy_location_id, date_created);

CREATE INDEX rpm_stock_txn_reference_type_idx
    ON rpm_stock_transaction (reference_number, transaction_type);

CREATE INDEX rpm_request_status_target_date_idx
    ON rpm_patient_dispensing_request (voided, request_status, target_pharmacy_location_id, date_created);

CREATE INDEX rpm_transfer_status_source_date_idx
    ON rpm_inter_pharmacy_transfer (voided, transfer_status, source_pharmacy_location_id, date_created);

CREATE INDEX rpm_transfer_status_dest_date_idx
    ON rpm_inter_pharmacy_transfer (voided, transfer_status, destination_pharmacy_location_id, date_created);

CREATE INDEX rpm_purchase_order_approval_date_idx
    ON rpm_purchase_order (approval_status, requested_date);

CREATE INDEX rpm_purchase_order_dispatch_date_idx
    ON rpm_purchase_order (dispatch_status, requested_date);
