# Rwanda Pharmacy Management User Manual
**Module:** Rwanda Pharmacy Management
**Audience:** pharmacy staff, dispensing store users, main pharmacy users, doctors, lab request users, purchase order users, finance approvers, facility leadership, and administrators.
## 1. Purpose
The module manages hospital pharmacy stock, patient prescription queues, consumable requests, lab supply requests, pharmacy item requests, pharmacy-to-pharmacy transfers, return-to-main transfers, purchase orders, supplier deliveries, inventory reports, billing linkage, alerts, and integrations such as SMS, email, RMS, and HIE.
## 2. Main Navigation
| Menu | Use |
| --- | --- |
| Dashboard | Quick actions and counts for queues, purchase orders, and alerts. |
| Patient queue | See patient prescription and consumable requests waiting for payment or dispensing in the current pharmacy. |
| Pharmacy item request | User requests specific items from one selected pharmacy. One request cannot mix different pharmacies. |
| Pharmacy item request queue | Pharmacy staff approve and issue items requested by users from their pharmacy. |
| Lab request / lab queue | Lab users request items and pharmacy staff approve/issue lab supplies. |
| Main pharmacy | Main store inventory, purchase order cart, and main-pharmacy stock operations. |
| Dispensing pharmacies | Dispensing stock view, request cart, and pharmacy-specific stock operations. |
| Main to dispensing | Dispensing pharmacy requests stock from main; Head of Finance approval is required before transfer. |
| Return to main pharmacy | Main pharmacy requests items back from a dispensing pharmacy; the dispensing pharmacy approves or rejects. |
| Dispensing to dispensing | One dispensing pharmacy requests stock from another dispensing pharmacy. |
| Purchase orders | Create, edit, approve, reject, dispatch, and retry supplier purchase orders. |
| Receive delivery | Main pharmacy receives RMS/API deliveries, uploaded supplier templates, or direct delivery entries. |
| Reports | Stock, valuation, patient request, inventory movement, and item movement reports with exports. |
| Control | Alerts, patient queue cleanup, link bills, safety rules, settings, and integration logs. |
## 3. Current Pharmacy Selection
If the logged-in OpenMRS location is connected to more than one pharmacy, some pages ask the user to choose the active pharmacy. The selection is remembered for the session. Pages such as patient queue, dispensing store, pharmacy request queue, and transfer approvals use this active pharmacy to filter what the user sees.
The navigation also uses the current OpenMRS location mapping. Main-pharmacy-only links are hidden from dispensing pharmacy users, dispensing-only links are hidden from main pharmacy users, and shared pharmacy pages such as reconciliation remain visible only when the current location maps to a pharmacy.
## 4. Prescriptions
### 4.1 Creating a Prescription
Prescriptions can be created from the patient dashboard prescription page or from an HTML Form Entry form that contains the embedded prescription order section.

1. Open the patient dashboard.
2. Click **Prescription**.
3. Select drugs, frequency, route, duration, quantity, target pharmacy, and **Dose instructions**.
4. The page shows stock status and billing mapping status for items in store. Hidden items are shown as unavailable/out of stock for requesters.
5. Submit the prescription.
The system creates OpenMRS drug orders, creates a patient dispensing request, links a bill when available, and can send SMS/HIE notifications if enabled.

When a clinical form contains an embedded prescription order section, add one or more prescription lines in the section before saving the form. The prescription request is queued when the form is saved. If the embedded section is left empty and it is not configured as required, no pharmacy request is created from that section.

> **Billing note:** if the configured payment mode is strict, only the bill linked to the request is checked. If it is latest, the system may use the latest patient bill when no bill was passed from the prescription page.
> **Repeat prescription note:** if the same drug is still open at the pharmacy for this patient (not yet paid, paid and waiting, or being dispensed), the prescription is not saved and the message names the open prescription. Edit the unpaid prescription instead of prescribing the drug again. Once the earlier prescription is dispensed or closed, the module replaces its active order when the drug is prescribed again, so OpenMRS does not block repeat prescribing within 24 hours.
Dose unit is intentionally not displayed on the dispensing workflow and is sent blank to HIE because the system cannot reliably determine whether the product is tablet, capsule, liquid, injection, or another form from the available data.
### 4.2 Patient Queue
1. Open **Patient queue**.
2. Select the active pharmacy if prompted.
3. Use tabs such as Recent, Ready, Waiting Payment, Exception Approved, Older Pending, or Search All to find requests.
4. Search by patient name, patient identifier, request ID, prescription group/SMS identifier, or UPID when UPID is configured.
5. Requests with unpaid linked bills remain in payment status. Paid, exception-approved, or unbillable requests move to pending dispense.
6. Open the request to view or dispense.
The serve page only offers batches from the active pharmacy. Hidden prescription items cannot be dispensed, even if stock exists.
After the serve form passes validation, the submit buttons are disabled while the request is being processed. This prevents accidental double-click dispensing from the same browser page.
Use **Dispensing history** to review completed patient dispensing records. Use **Dispense without order** only where the workflow is enabled and the user has the required privilege.
### 4.3 Patient Queue Cleanup
Administrators can open **Control > Patient queue cleanup** to mark old pending patient requests as **Not served**.
1. Select a date range and optionally a pharmacy.
2. Preview matching pending payment/pending dispense requests.
3. Enter a reason, confirm the action, and mark the matched requests as not served.
Requests marked **Not served** are treated as terminal and are removed from the patient queue.
### 4.4 Drug Not Available
The **Item not available in all pharmacies** action is available from the prescription workflow so the user can record unavailable items even when stock status may be uncertain.
## 5. Consumable Requests
Consumable requests can be created from the patient dashboard consumable request page or from an HTML Form Entry form that contains the embedded consumables order section.

1. Open the patient dashboard.
2. Click the consumable request app/action.
3. Select consumable items and quantities.
4. Submit the request.
Consumables are billable and should be mapped in MOH Billing. Billing mappings are validated when the order is submitted, not while loading the consumable order page.
When a clinical form contains an embedded consumables order section, add one or more consumable lines in the section before saving the form. The consumable request is queued when the form is saved. If the embedded section is left empty and it is not configured as required, no pharmacy request is created from that section.
If a patient has an active visit registration but the selected insurance has no open global bill, the page warns the user before the request is submitted. This normally means the open bill does not match the active visit insurance/policy being used by the request.
## 6. User Pharmacy Item Request
This page is for a user requesting items from a specific pharmacy, not for pharmacy-to-pharmacy transfers.
1. Open **Pharmacy item request**.
2. Select one pharmacy for the request.
3. Add items and quantities.
4. Submit the request.
One request summary must contain items requested from only one pharmacy. The page includes **My request history** so users can track previous requests and download a PDF copy of a request.
## 7. Pharmacy Item Request Queue
1. Open **Pharmacy item request queue**.
2. The queue uses the current active pharmacy.
3. Review requested items and available stock.
4. Approve quantities. Stock is issued from the selected active pharmacy only.
The request history page is in the **Dispensing** navigation block. Pharmacy staff can filter previous records and download one request as a PDF.
## 8. Lab Supply Requests
### 8.1 Creating Lab Requests
1. Open **Lab request**.
2. Select configured lab service/request items.
3. Submit the request.
### 8.2 Lab Queue
1. Open **Lab dispensing queue**.
2. Review pending requests.
3. Approve and issue available quantities.
## 9. Inventory
### 9.1 Main Pharmacy
Main pharmacy users can view stock, search items, add items to the purchase order cart, receive deliveries, reconcile stock, and control batch expiry dates.
### 9.2 Dispensing Pharmacy
Dispensing pharmacy users can view current stock, request items, and see requests and transfers related to the active pharmacy.
### 9.3 Expiry Rules
Drugs and items configured to require expiry must have an expiry date when received. Consumables that do not require expiry can be received with blank or N/A expiry.
### 9.4 Batch Expiry Control
1. Open **Batch expiry control**.
2. Search for the item/batch.
3. Enter the corrected expiry date.
4. Save the change.
This changes the expiry date for an existing batch number without changing quantity.
### 9.5 Item Visibility Control
1. Open **Item visibility control**.
2. Search by item name or code.
3. Review hidden items first, then visible items in alphabetical order.
4. Hide or show the item as needed.
Hidden items are treated as unavailable on prescription/requester-facing pages and cannot be dispensed from the patient queue.
### 9.6 Reconciliation
Use **Reconciliation** for controlled stock corrections in either the main pharmacy or a dispensing pharmacy, when the user has stock adjustment permission and the current location is mapped to that pharmacy. Select the pharmacy, item, batch, quantity adjustment, and reason. The change creates an auditable stock transaction.
## 10. Pharmacy Transfers
### 10.1 Main to Dispensing Transfer
1. A dispensing pharmacy opens **Main to dispensing**.
2. The destination pharmacy is the active dispensing pharmacy.
3. The user requests items from main pharmacy.
4. The Head of Finance opens the review/approval page and approves, rejects, or removes unavailable lines with a reason.
5. Transfer approval is restricted to the pharmacy responsible for serving the request. A user only sees and acts on requests where their current pharmacy is the supplying pharmacy.
6. The review page shows the current quantity in the requesting pharmacy, the current quantity available in the serving pharmacy, and **Previous info** shows calculated previous balance, received quantity, and dispensed/transferred-out quantity based on dates.
7. Approved requests are transferred using transfer labels and buttons. A single requested item can be served from more than one batch number when needed.
Transferred requests can be downloaded as a PDF from transfer history/review pages.
### 10.2 Dispensing to Dispensing Transfer
1. Open **Dispensing to dispensing**.
2. The source list excludes main pharmacy and excludes the current pharmacy.
3. The receiving pharmacy is the current active pharmacy.
4. The supplying dispensing pharmacy approves/transfers or rejects the request.
Transferred dispensing-to-dispensing requests also support PDF download once the transfer is completed.
### 10.3 Return to Main Pharmacy
1. Main pharmacy opens **Return to main pharmacy**.
2. Main selects the dispensing pharmacy, items, quantities, and reason.
3. The selected dispensing pharmacy sees the request and approves transfer back or rejects with a reason.
Return-to-main requests can be downloaded as PDF after transfer.
## 11. Department Store Workflows
### 11.1 Department Store Workspace
The department store workspace is a single page for department store users. It gives access to department stock, department requests, internal issues, and downloadable request PDFs without requiring a user-to-department assignment.
### 11.2 Department Bulk Request
1. Open **Department bulk request** or the department store workspace.
2. Add items requested from pharmacy.
3. When full visibility exists, the page can autofill request rows from dispensed items; users can still edit quantities before submitting.
4. Submit the request for the configured approval flow.
### 11.3 Finance Approval and Pharmacy Dispense
1. Finance opens **Department finance approval** and reviews pending requests.
2. Pharmacy opens **Department pharmacy dispense** for approved requests.
3. Batch numbers are loaded from the active dispensing pharmacy only.
4. After dispensing, the request can be downloaded as PDF.
### 11.4 Department Stock Issue
Use **Department stock issue** to issue items internally from department stock. The page supports issuing more than one item at the same time.
## 12. Purchase Orders
1. Open **Main pharmacy**.
2. Add items to the order cart.
3. Edit quantity and unit price in the cart.
4. Select supplier and submit cart. Orders are grouped by program.
5. Health Facility Leader reviews pending purchase orders.
6. Prices are editable on the review page. RMS orders may refresh prices from RMS. Non-RMS supplier orders keep manually edited prices.
7. Approve to dispatch by API or email depending on supplier setup.
Purchase order emails and PDFs include the configured hospital name.
## 13. Supplier and Product Master
| Page | Use |
| --- | --- |
| Supplier Configuration | Create or edit suppliers, RMS/API settings, and email dispatch settings. |
| Product sync | Pull master product items from RMS/supplier source before mapping local items or placing purchase orders. |
| Price history | Review persisted supplier prices and previous price sync results. |
| Receive delivery | Receive supplier deliveries from RMS/API, supplier template upload, or direct entry. |
When the same NPC exists under different programs, treat each program-specific product carefully. Do not merge records unless the product and program are truly the same.
## 14. Receiving Deliveries
Deliveries can be received from RMS/API details, uploaded supplier Excel templates, or direct entry. Templates include expected format examples in the first row and use zero values where a sample number is needed. Expiry can be N/A for consumables that do not require expiry.
After a successful receipt, use **Download proof of receipt** to download the receipt PDF. Use **Receipt history** to review past receipts.
## 15. Master Data and Administration
| Page | Use |
| --- | --- |
| Items | Create and maintain local item metadata, product mapping readiness, concept/drug mapping, expiry requirement, batch requirement, and stock flags. |
| Locations | Create and maintain pharmacy locations, main/dispensing type, and OpenMRS location/tag mapping. |
| Settings | Manage module global settings from the administration page when the user has settings privilege. |
| Safety rules | Create or maintain clinical safety rules used by prescription checks. |
| Missing billing mappings | Identify configured products that do not have expected MOH Billing mappings. |
## 16. Billing Link Administration
1. Open **Link patient request bills**.
2. Search by request ID or patient identifier/name.
3. Enter the patient bill ID to link.
4. The request payment status is updated according to the bill paid status.
This is intended for prescription and consumable requests that entered the system without a bill link or had billing mapping issues.
## 17. Integrations and Logs
| Page | Use |
| --- | --- |
| Pending HIE queue | Review pending or failed HIE messages and retry after configuration/network issues are corrected. |
| Integration logs | Review SMS, HIE, RMS, email, and supplier integration attempts with status and response details. |
| Safety rules | Manage clinical safety rules used during prescription checks. |
| Missing billing mappings | Find products that are missing billing mappings before prescriptions or consumable orders fail at billing time. |
## 18. Alerts
The alerts dashboard shows two separate alert tables: **Expiration Alerts** and **Stockout and Low Stock Alerts**. Use the filters at the top of the page to narrow the results and use **Download PDF** to export the same filtered report.
### 18.1 Alert Filters
| Filter | Use |
| --- | --- |
| Pharmacy location | Select one pharmacy to view alerts for that location. Leave blank to view hospital-wide alerts. |
| Category | Limit alerts to one item category. |
| Item name/code | Search for a specific item by name or code. |
| Expiry status | Show all expiry alerts, expired only, or expiring soon only. |
| Stock status | Show stockout and low stock together, stockout only, or low stock only. |
| Expiry Alert Days | Controls how far ahead the expiration table looks. Example: 90 means show expired batches and batches expiring within the next 90 days. |
| Days of Stock Alert Days | Controls the low stock threshold. Example: 14 means alert when stock is expected to last 14 days or less. |
| Consumption Lookback Days | Controls how far back the system looks to calculate average daily consumption for days-of-stock calculations. |
### 18.2 Expiration Alerts Table
The expiration table is batch-based. Each row represents an item batch that is already expired or will expire within the selected **Expiry Alert Days**.
| Column | Meaning |
| --- | --- |
| Item / Code | The item and its local code. |
| Location | The pharmacy location holding the batch. |
| Batch | The batch number. |
| Expiry | The batch expiry date. |
| Days to Expiry | Negative or zero means expired; positive means days remaining. |
| On Hand | Current quantity on hand for that batch. |
| Status | **Expired** or **Expiring soon**. |
### 18.3 Stockout and Low Stock Alerts Table
The stockout/low stock table is item-based. If no pharmacy location is selected, it calculates stock for the whole hospital by summing all pharmacy locations. If a pharmacy location is selected, it calculates only for that location.
This table only concerns items that have ever had stock activity, meaning an inventory balance or stock transaction exists. It does not list every item configured in the item master.
| Column | Meaning |
| --- | --- |
| Current Qty | Current quantity on hand for the selected location or whole hospital. |
| Consumed | Total outbound stock movement during the selected consumption lookback period. |
| Avg/Day | Average daily consumption calculated from the lookback period. |
| Days of Stock | Estimated days current stock will last based on average daily consumption. |
| Status | **Stockout** when current quantity is zero or less. **Low stock** when days of stock is less than or equal to the alert threshold. |
> If an item has current stock but no recent consumption, it is not shown as low stock unless it is stocked out.
### 18.4 Downloading the Alert PDF
1. Open **Alerts Dashboard**.
2. Choose the filters required for the report.
3. Click **Apply filters**.
4. Click **Download PDF**.
The PDF includes the selected filters, the expiration alert table, and the stockout/low stock alert table.
## 19. Reports
| Report | Purpose |
| --- | --- |
| Individual pharmacy report | Stock for one selected pharmacy. |
| Overall hospital stock report | Stock across all pharmacies. |
| Inventory movement report | Movements over a selected period. |
| Dispensing report | Patient dispensing activity with export options. |
| Patient request report | Patient prescription and consumable request records, including active and terminal request states. |
| Payment exception report | Payment exception approvals and related dispensing requests. |
| Item movement report | All movements for one selected item, all pharmacies or one pharmacy, including batch, expiry, received, issued, balance, and months of stock. |
| Stock valuation | Stock values using available unit costs. |
Reports support Excel and/or PDF export where available.
## 20. Roles and Responsibilities
| Role | Main responsibility |
| --- | --- |
| Doctor/clinician | Create prescriptions from patient dashboard. |
| Dispenser | Use patient queue and dispense requests. |
| Main store manager | Main stock, purchase order cart, deliveries, transfers, and reconciliation where permitted. |
| Dispensing store manager | Dispensing stock, dispensing pharmacy transfers, and reconciliation where permitted. |
| Department store user | Department workspace, department stock visibility, department requests, and internal stock issue. |
| Head of Finance | Approve main-to-dispensing transfers. |
| Health Facility Leader | Approve purchase orders. |
| Administrator | Settings, locations, items, roles, billing links, safety rules, integrations. |
## 21. Troubleshooting
| Problem | What to check |
| --- | --- |
| Queue badge shows zero but records exist | Check OpenMRS logs for queue notification warnings. Confirm active pharmacy selection and location-tag mapping. |
| Prescription unpaid after bill is paid | Check request patient_bill_id, payment_confirmed, request_status, line_status, bill paid flag, and payment bill mode. |
| SMS times out | Check SMS endpoint, port, network access, server public IP whitelist, username, password, sender, and timeout properties. |
| Product not available for ordering | Pull/sync master product items first, then confirm local item mapping, material type, program, and stock flags. |
| Billing mapped shown incorrectly | Check item concept/drug concept and MOH Billing facility service/billable service mapping. |
| Transfer fails for stock | Check batch number, available balance, and whether the item is stocked in the selected source pharmacy. |
| Main-to-dispensing transfer says user is not at main pharmacy | Check the logged-in OpenMRS session location, pharmacy-location mapping, and whether the active location is mapped to the main store. |
| Transfer approval page does not show a request | Check that the current OpenMRS location maps to the request's supplying pharmacy. Approval is restricted to the pharmacy responsible for serving the request. |
| Main-only page is hidden for a user | Check whether the logged-in OpenMRS location maps to the main pharmacy. The menu hides main-pharmacy-only pages from dispensing pharmacy locations. |
| Department dispense shows main pharmacy batches | Check active pharmacy selection and location mapping. Department pharmacy dispense should use the active dispensing pharmacy only. |
| Hidden item still appears available | Refresh product/stock pages and confirm the item is hidden in Item visibility control. Hidden items should behave as unavailable to requesters and patient queue dispensing. |
| Prescription billing creates fewer lines than requested | Check billing mappings, duplicate billable service mappings, hidden/unavailable items, and linked patient bill status. |
| Item movement shows duplicate patient dispense rows | Check `rpm_dispense_record`, `rpm_dispense_record_line`, and `rpm_stock_transaction` for the same request, item, quantity, batch, user, and close timestamps. New dispense stock transactions store the request id in `reference_number`. |
