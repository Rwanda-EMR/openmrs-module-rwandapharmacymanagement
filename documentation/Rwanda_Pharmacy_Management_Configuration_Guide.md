# Rwanda Pharmacy Management Full Configuration Guide
**Purpose:** checklist and configuration reference required to make the Rwanda Pharmacy Management module work end to end.
## 1. Configuration Order
1. Install and start required OpenMRS modules.
2. Assign module roles and privileges.
3. Configure global properties.
4. Configure OpenMRS locations and location tags.
5. Create pharmacy locations and link them to OpenMRS locations/tags.
6. Configure supplier/RMS and pull all master product items first.
7. Map master products to local items, concepts, drugs, programs, and material types.
8. Configure billing mappings for drugs and consumables where billing is expected.
9. Configure app framework links for pharmacy dashboard, lab request, general pharmacy request, consumable request, and prescription request.
10. Configure SMS, SMTP, HIE, reports, and alerts as needed.
11. Test each workflow using a real location, user role, patient, bill, and stock item.
## 2. Required Modules
| Module | Reason |
| --- | --- |
| appframework | Homepage and patient dashboard app links. |
| appui | UI includes and app shell. |
| coreapps | Patient dashboard integration. |
| emrapi | EMR API support used by OpenMRS distribution. |
| legacyui | Legacy OpenMRS UI compatibility. |
| uicommons | Shared UI resources. |
| uiframework | Page and fragment rendering. |
| webservices.rest | REST support. |
| mohbilling | Required if billing status and billable service mapping are used. |
## 3. Roles and Privileges
| Privilege | Use |
| --- | --- |
| `App: rwandapharmacymanagement.dashboard` | Access pharmacy app/dashboard. |
| `Task: rwandapharmacymanagement.createPrescription` | Create, edit, or cancel a patient prescription. |
| `Task: rwandapharmacymanagement.orderConsumables` | Create, edit, or cancel a patient consumable request. |
| `Task: rwandapharmacymanagement.viewStock` | View stock and reports. |
| `Task: rwandapharmacymanagement.viewPatientQueue` | Use patient queue. |
| `Task: rwandapharmacymanagement.dispenseWithOrder` | Dispense queued requests. |
| `Task: rwandapharmacymanagement.dispenseWithoutOrder` | Dispense without order where enabled. |
| `Task: rwandapharmacymanagement.transferStock` | Transfer stock between pharmacies. |
| `Task: rwandapharmacymanagement.approveMainToDispensingTransfer` | Head of Finance approval for main-to-dispensing transfers. |
| `Task: rwandapharmacymanagement.departmentStore` | Access the department store workspace and department stock issue workflows. |
| `Task: rwandapharmacymanagement.approvePaymentException` | Approve controlled exceptions where dispensing must proceed outside the normal payment rule. |
| `Task: rwandapharmacymanagement.submitSupplierOrders` | Create/submit/retry supplier purchase orders. |
| `Task: rwandapharmacymanagement.approvePurchaseOrders` | Health Facility Leader purchase order approval. |
| `Task: rwandapharmacymanagement.receiveDeliveries` | Receive supplier deliveries. |
| `Task: rwandapharmacymanagement.adjustStock` | Stock reconciliation/adjustment. |
| `Task: rwandapharmacymanagement.manageItems` | Item master administration. |
| `Task: rwandapharmacymanagement.managePharmacies` | Pharmacy location administration. |
| `Task: rwandapharmacymanagement.manageSupplierSettings` | Supplier/RMS/SMTP configuration. |
| `Task: rwandapharmacymanagement.syncSupplierProducts` | Pull/sync master product items. |
| `Task: rwandapharmacymanagement.syncSupplierPrices` | Sync supplier prices. |
| `Task: rwandapharmacymanagement.manageClinicalSafetyRules` | Manage safety rules. |
| `Task: rwandapharmacymanagement.viewIntegrationLogs` | View integration logs. |
| `Task: rwandapharmacymanagement.manageSettings` | Module settings. |

The `Rwanda Pharmacy Doctor` role is assigned both patient-ordering privileges. The `Rwanda Pharmacy Nurse` role is assigned `orderConsumables` but not `createPrescription`; both roles also have dashboard access. The administrator role receives both ordering privileges.
## 4. Global Properties
| Property | Example | Use |
| --- | --- | --- |
| `rwandapharmacymanagement.facilityId` | `1660` | Facility/FOSA ID for purchase orders, RMS, HIE, and SMS prescription number context. This is the correct property. |
| `rwandapharmancymanagement.facilityId` | `1660` | Legacy misspelled fallback. Keep same value during transition. |
| `rwandapharmacymanagement.purchaseOrder.hospital` | `Kirehe Hospital` | Hospital name used in SMS message text. |
| `rwandapharmacymanagement.purchaseOrder.hospitalName` | `Kirehe Hospital` | Hospital name on purchase order emails and PDFs. Keep same as hospital. |
| `rwandapharmacymanagement.prescription.paymentBillMode` | `strict` or `latest` | Strict checks only linked bill. Latest can fall back to latest bill. |
| `rwandapharmacymanagement.alerts.expiryDays` | `90` | Default number of days ahead used by the Expiration Alerts table. Users can override it from the alert page filter. |
| `rwandapharmacymanagement.alerts.daysOfStockDays` | `14` | Default days-of-stock threshold for Low Stock alerts. Items with calculated days of stock less than or equal to this value are shown as low stock. |
| `rwandapharmacymanagement.alerts.consumptionLookbackDays` | `90` | Default number of past days used to calculate average daily consumption for the Stockout and Low Stock table. |
| `rwandapharmacymanagement.pricing.dispensingMarkupPercentage` | `0` | Markup for dispensing price calculations. |
| `rwandapharmacymanagement.lab.serviceConceptSet` | Concept UUID or ID | Concept set/answers used for lab request services. |
| `rwandapharmacymanagement.prescription.frequencyConceptSet` | Concept UUID or ID | Concept set/answers used for prescription frequency options. |
## 5. Supplier, RMS, and Product Master Setup
### 5.1 Legacy RMS Defaults
| Property | Example | Use |
| --- | --- | --- |
| `rwandapharmacymanagement.supplier.baseUrl` | RMS base URL | RMS/supplier API root. |
| `rwandapharmacymanagement.supplier.username` | MOH.USER | RMS username. |
| `rwandapharmacymanagement.supplier.password` | Password | RMS password. |
| `rwandapharmacymanagement.supplier.connectionTimeout` | `10000` | API connection timeout in milliseconds. |
| `rwandapharmacymanagement.supplier.readTimeout` | `30000` | API read timeout in milliseconds. |
| `rwandapharmacymanagement.supplier.enabled` | `true` | Enable RMS integration. |
| `rwandapharmacymanagement.supplier.deliveryFetchSchedulerEnabled` | `false` | Enable scheduled RMS delivery fetching. |
| `rwandapharmacymanagement.supplier.deliveryFetchSchedulerIntervalSeconds` | `3600` | Scheduler interval for RMS delivery fetching. |
### 5.2 Supplier Records
Create suppliers in **Supplier Configuration**. For RMS set API enabled. For non-RMS suppliers, set email address and leave API disabled if orders are sent by email.
### 5.3 First Product Master Pull
1. Open **Product sync**.
2. Authenticate to RMS if required.
3. Pull all master product items before creating local items or purchase orders.
4. Confirm each item has NPC code, name, material type, program, price, and active status.
5. Remember that the same NPC can appear more than once when it belongs to different programs. Do not merge these records unless the program is also the same.
### 5.4 Local Item Mapping
| Field | Rule |
| --- | --- |
| Name | Use a clear item name matching product master where possible. |
| Code/NPC | Use NPC code from product master. |
| Program | Set program from product master. Same NPC in different programs should create separate program-aware item/price behavior. |
| Material type | Drug or consumable based on RMS material type. Drugs are prescribed; consumables are requested from patient consumable workflow. |
| Drug/concept | Map drugs to OpenMRS Drug and concept where prescription and billing checks are needed. |
| Requires expiry | Enable for drugs and expiry-controlled items. Disable for consumables that can be received as N/A expiry. |
| Requires batch | Enable when stock should be tracked by batch. |
| Low stock threshold | Set for alerts and suggested purchase quantities. |
## 6. Pharmacy Locations and OpenMRS Location Tags
1. Create one main pharmacy location and mark it as main store.
2. Create each dispensing pharmacy.
3. Link each pharmacy to an OpenMRS location and/or one or more OpenMRS location tags.
4. If one OpenMRS location tag maps to more than one dispensing pharmacy, users will be asked to choose active pharmacy once per session.
5. If one dispensing pharmacy serves multiple OpenMRS location tags, add all relevant tags to that pharmacy.
6. Main-pharmacy-only navigation links are shown only when the logged-in OpenMRS location maps to the main pharmacy. Dispensing-only links are shown only when the location maps to at least one active dispensing pharmacy. Shared pharmacy pages require either mapping.
## 7. Billing Configuration
1. Install and configure MOH Billing.
2. Create facility services and billable services for drugs and consumables that should be billed.
3. Map each billed drug/consumable concept to a facility service price.
4. For prescriptions, drug billing mapping is checked through product drug concept and stocked local item concept/drug concept.
5. Consumable order page loading does not check billing mappings. Billing mappings are validated when the order is submitted.
6. Configure `rwandapharmacymanagement.prescription.paymentBillMode` as `strict` or `latest`.
> The module creates and links its own prescription/consumable request records to billing. Avoid database triggers that also create drug billing rows from OpenMRS orders, because they can duplicate bills or fail when billing mappings contain duplicate rows.
Use **Missing billing mappings** to identify products that cannot be billed, and **Link prescription bills** when a request exists but the bill link needs administrative correction.
## 8. SMS Configuration
| Property | Example | Use |
| --- | --- | --- |
| `rwandapharmacymanagement.sms.enabled` | `true` | Enable prescription SMS. |
| `rwandapharmacymanagement.sms.authorizationUrl` | Auth endpoint | Optional token/auth endpoint when the SMS provider requires authentication before send. |
| `rwandapharmacymanagement.sms.postUrl` | `http://41.186.44.171:1401/send` | Intouch HTTP send endpoint from the high-volume API documentation. |
| `rwandapharmacymanagement.sms.username` | Provided by Intouch | SMS username. |
| `rwandapharmacymanagement.sms.password` | Provided by Intouch | SMS password. |
| `rwandapharmacymanagement.sms.sender` | Approved sender name | SMS sender/from value. |
| `rwandapharmacymanagement.sms.coding` | `0` | SMS coding value. |
| `rwandapharmacymanagement.sms.dlrLevel` | `2` | Delivery report level. |
| `rwandapharmacymanagement.sms.dlrUrl` | Public callback URL | Optional delivery report callback. Must reach `/module/rwandapharmacymanagement/sms/dlr`. |
| `rwandapharmacymanagement.sms.phonePersonAttributeTypeUuid` | Phone attribute type UUID | Where patient phone number is stored. |
| `rwandapharmacymanagement.sms.connectionTimeout` | `10000` | Connection timeout. |
| `rwandapharmacymanagement.sms.readTimeout` | `30000` | Read timeout. |
| `rwandapharmacymanagement.sms.authPayloadTemplate` | JSON template | Optional authentication payload template using `{{username}}` and `{{password}}`. |
| `rwandapharmacymanagement.sms.postPayloadTemplate` | JSON template | Optional SMS send payload template using `{{sender}}`, `{{phoneNumber}}`, and `{{message}}`. |
| `rwandapharmacymanagement.sms.retrySchedulerEnabled` | `true` | Retry failed/pending SMS messages through the scheduler. |
| `rwandapharmacymanagement.sms.retrySchedulerIntervalSeconds` | `1800` | SMS retry scheduler interval. |
| `rwandapharmacymanagement.sms.retryBatchSize` | `50` | Maximum SMS log rows retried per scheduler run. |
> If Intouch requires IP whitelisting, provide the public outbound IP address of the OpenMRS server. A connection timeout can occur when the IP is not whitelisted or when the endpoint/port is blocked.
## 9. SMTP Configuration for Purchase Orders
| Property | Example | Use |
| --- | --- | --- |
| `rwandapharmacymanagement.smtp.host` | `smtp.gmail.com` | SMTP host. |
| `rwandapharmacymanagement.smtp.port` | `587` | SMTP port. |
| `rwandapharmacymanagement.smtp.username` | Email username | SMTP username. |
| `rwandapharmacymanagement.smtp.password` | App password | SMTP password. |
| `rwandapharmacymanagement.smtp.from` | sender@example.org | From email. |
| `rwandapharmacymanagement.smtp.auth` | `true` | Require authentication. |
| `rwandapharmacymanagement.smtp.starttls` | `true` | Use STARTTLS. |
## 10. HIE Configuration
| Property | Use |
| --- | --- |
| `rwandapharmacymanagement.hie.enabled` | Enable HIE integration. |
| `rwandapharmacymanagement.hie.baseUrl` | HIE base URL. |
| `rwandapharmacymanagement.hie.username` | HIE username. |
| `rwandapharmacymanagement.hie.password` | HIE password. |
| `rwandapharmacymanagement.hie.encounterPath` | Encounter endpoint path. |
| `rwandapharmacymanagement.hie.encounterServiceTypeCode` | Service type code sent in pharmacy encounters. |
| `rwandapharmacymanagement.hie.encounterServiceTypeDisplay` | Service type display sent in pharmacy encounters. |
| `rwandapharmacymanagement.hie.sourceSystem` | Source system name sent to HIE. |
| `rwandapharmacymanagement.hie.medicationRequestPath` | MedicationRequest endpoint path. |
| `rwandapharmacymanagement.hie.medicationDispensePath` | MedicationDispense endpoint path. |
| `rwandapharmacymanagement.hie.patientIdentifierTypeUuid` | Patient identifier type sent to HIE and used as UPID search/display in the patient queue. |
| `rwandapharmacymanagement.hie.medicationCodeSystem` | Medication code system URI. |
| `rwandapharmacymanagement.hie.groupIdentifierSystem` | Group identifier system for prescription/order number. |
| `rwandapharmacymanagement.hie.connectionTimeout` | Connection timeout. |
| `rwandapharmacymanagement.hie.readTimeout` | Read timeout. |
| `rwandapharmacymanagement.hie.retrySchedulerEnabled` | Enable retry of failed/pending HIE messages. |
| `rwandapharmacymanagement.hie.retrySchedulerIntervalSeconds` | HIE retry scheduler interval. |
| `rwandapharmacymanagement.hie.retryBatchSize` | Maximum HIE log rows retried per scheduler run. |
HIE prescription messages keep the same group identifier across all drugs in one prescription. Dose unit is currently sent as a blank value to avoid showing incorrect tablet/capsule units.
## 11. App Framework Configuration
Use these app configurations as references. Adjust required privileges, order, labels, icons, and location rules for the deployment.
### 11.1 Pharmacy Main Dashboard App
```
{
  "id": "rwandapharmacymanagement.pharmacyManageApp",
  "description": "Rwanda Pharmacy Management dashboard",
  "order": 10,
  "extensions": [
    {
      "id": "rwandapharmacymanagement.dashboard",
      "label": "Pharmacy",
      "extensionPointId": "org.openmrs.referenceapplication.homepageLink",
      "type": "link",
      "description": "Rwanda Pharmacy Management dashboard",
      "order": 50,
      "icon": "icon-medkit",
      "url": "rwandapharmacymanagement/index.page",
      "requiredPrivilege": "App: rwandapharmacymanagement.dashboard",
      "require": "typeof sessionLocation !== 'undefined' && hasMemberWithProperty(sessionLocation.tags, 'display', 'Pharmacy Location')"
    }
  ]
}
```
### 11.2 Prescription Request Patient Dashboard Action
```sql
{
  "id": "rwandapharmacymanagement.prescription.enter",
  "label": "Prescription",
  "description": "Create a drug prescription and queue it for pharmacy",
  "extensions": [
    {
      "id": "rwandapharmacymanagement.prescription.enter.action",
      "extensionPointId": "patientDashboard.overallActions",
      "type": "link",
      "label": "Prescription",
      "url": "rwandapharmacymanagement/prescription/enter.page?patientId={{patient.uuid}}",
      "icon": "icon-edit",
      "order": 45,
      "requiredPrivilege": "App: rwandapharmacymanagement.dashboard"
    }
  ],
  "requiredPrivilege": "App: rwandapharmacymanagement.dashboard"
}
```
### 11.3 Consumable Request Patient Dashboard Action
```
{
  "id": "rwandapharmacymanagement.consumable.enter",
  "label": "Consumables",
  "description": "Request patient consumable items",
  "extensions": [
    {
      "id": "rwandapharmacymanagement.consumable.enter.action",
      "extensionPointId": "patientDashboard.overallActions",
      "type": "link",
      "label": "Consumables",
      "url": "rwandapharmacymanagement/consumable/enter.page?patientId={{patient.uuid}}",
      "icon": "icon-list",
      "order": 46,
      "requiredPrivilege": "App: rwandapharmacymanagement.dashboard"
    }
  ],
  "requiredPrivilege": "App: rwandapharmacymanagement.dashboard"
}
```
### 11.4 HTML Form Entry Pharmacy Order Tags
The module registers embedded HTML Form Entry tags when it starts. These tags let form builders add pharmacy ordering controls directly inside an HTML Form Entry form:

```xml
<rpmPrescriptionOrder />
<rpmConsumableOrder />
```

Optional attributes:

| Tag | Attribute | Use |
| --- | --- | --- |
| `rpmPrescriptionOrder` | `required="true"` | Requires at least one prescription line before the HFE form can be saved. |
| `rpmPrescriptionOrder` | `patientBillId="123"` | Links the created prescription request to a known patient bill ID. |
| `rpmPrescriptionOrder` | `title="..."` | Changes the embedded section title. |
| `rpmPrescriptionOrder` | `buttonLabel="..."` | Changes the add-line button label. |
| `rpmConsumableOrder` | `required="true"` | Requires at least one consumable line before the HFE form can be saved. |
| `rpmConsumableOrder` | `title="..."` | Changes the embedded section title. |
| `rpmConsumableOrder` | `buttonLabel="..."` | Changes the add-line button label. |

On form save, the tags create Rwanda Pharmacy Management patient requests through the same prescription and consumable services used by the normal patient dashboard pages. Empty embedded order tables are ignored, so a form can include an optional order section without creating blank pharmacy requests.

Prescription orders require a patient, active visit, provider, and location. Consumable orders require a patient, location, and consumable billing readiness. Edit/delete of created lines remains in the normal pharmacy request workflows.

### 11.5 Lab Request App
```
{
  "id": "rwandapharmacymanagement.lab.request",
  "label": "Lab Supply Request",
  "description": "Request lab supply items",
  "extensions": [
    {
      "id": "rwandapharmacymanagement.lab.request.link",
      "extensionPointId": "org.openmrs.referenceapplication.homepageLink",
      "type": "link",
      "label": "Lab Supply Request",
      "url": "rwandapharmacymanagement/lab/request.page",
      "icon": "icon-beaker",
      "order": 55,
      "requiredPrivilege": "App: rwandapharmacymanagement.dashboard"
    }
  ],
  "requiredPrivilege": "App: rwandapharmacymanagement.dashboard"
}
```
### 11.6 General Pharmacy Item Request App
```sql
{
  "id": "rwandapharmacymanagement.pharmacyorder.request",
  "label": "Pharmacy Item Request",
  "description": "Request selected items from a selected pharmacy",
  "extensions": [
    {
      "id": "rwandapharmacymanagement.pharmacyorder.request.link",
      "extensionPointId": "org.openmrs.referenceapplication.homepageLink",
      "type": "link",
      "label": "Pharmacy Item Request",
      "url": "rwandapharmacymanagement/pharmacyorder/request.page",
      "icon": "icon-shopping-cart",
      "order": 56,
      "requiredPrivilege": "App: rwandapharmacymanagement.dashboard"
    }
  ],
  "requiredPrivilege": "App: rwandapharmacymanagement.dashboard"
}
```
## 12. Operational Workflow Configuration
### 12.1 Prescription and Patient Queue
| Area | Configuration rule |
| --- | --- |
| OpenMRS drugs | Each prescribed product must map to the correct OpenMRS Drug/concept. Remove duplicate active OpenMRS drugs with the same practical medication name to avoid ambiguous prescription behavior. |
| Dose instructions | Dose instructions are mandatory on prescription entry. They are stored and shown as instructions, while dose unit is intentionally not shown on dispensing and is sent blank to HIE. |
| Repeat prescriptions | When the same active drug is prescribed again for the same patient, the module replaces/discontinues the active same-drug order before saving the new one so OpenMRS does not block repeat prescribing within 24 hours. |
| Hidden items | Items hidden from requesters are treated as out of stock on prescription entry and cannot be dispensed from patient queue. |
| Billing | Prescription requests must have billable service mappings unless the item is intentionally unbillable. Administrative linking is available through Link prescription bills. |
| UPID search | Configure `rwandapharmacymanagement.hie.patientIdentifierTypeUuid` with the UPID patient identifier type UUID. Patient queue search can then match that identifier and the queue displays it in a UPID column. |
| Queue cleanup | `Control > Patient queue cleanup` requires `Task: rwandapharmacymanagement.manageSettings`. It previews pending payment/pending dispense requests by date range and optional pharmacy, then marks confirmed matches as `NOT_SERVED` with a reason so they leave the patient queue. |
### 12.2 HTML Form Entry Embedded Orders
| Area | Configuration rule |
| --- | --- |
| Module dependency | HTML Form Entry must be installed and started because the pharmacy module registers `rpmPrescriptionOrder` and `rpmConsumableOrder` as custom HFE tags. |
| Prescription tag | Use `<rpmPrescriptionOrder />` in an HFE form to add an embedded prescription section. On save, it creates OpenMRS drug orders and the pharmacy dispensing request through `PrescriptionService`. |
| Consumable tag | Use `<rpmConsumableOrder />` in an HFE form to add an embedded consumable section. On save, it creates consumable requests through `ConsumableOrderService`. |
| Optional sections | Leave `required` unset or `false` to allow the HFE form to save with no pharmacy lines. Use `required="true"` when the form must contain at least one pharmacy line. |
| Follow-up changes | Edits, deletes, payment checks, dispensing, and billing cleanup continue to be managed in the existing pharmacy request and patient queue pages after the HFE form creates the request. |

### 12.3 Item Visibility and Batch Expiry Control
| Control page | Configuration and effect |
| --- | --- |
| Item visibility control | Requires `Task: rwandapharmacymanagement.adjustStock`. Hidden items are listed first for review, can be searched by item name/code, and are hidden from requester-facing selection/availability. |
| Batch expiry control | Requires `Task: rwandapharmacymanagement.adjustStock` and a current main pharmacy mapping. Main pharmacy users can update the expiry date for an existing batch number without changing quantity. |
| Reconciliation | Requires `Task: rwandapharmacymanagement.adjustStock` and a current pharmacy mapping. Main and dispensing pharmacies can use it for controlled stock corrections with adjustment reason and audit trail. |
### 12.4 Pharmacy Item Requests
Pharmacy item requests are user requests to a selected pharmacy. The queue is placed under the **Dispensing** navigation block, and history is available with PDF download per request.
| Feature | Configuration rule |
| --- | --- |
| Requester page | Requester selects one pharmacy, then adds visible/requestable items. One request should not mix source pharmacies. |
| Queue | Queue uses the user's active pharmacy and issues stock only from that pharmacy. |
| History | Users with transfer or approval privilege can view request history and download a PDF copy of a request. |
### 12.5 Main, Dispensing, and Department Transfers
| Workflow | Configuration rule |
| --- | --- |
| Main to dispensing | Requires a main store, dispensing pharmacies, Head of Finance approval privilege, and transfer privilege. Review shows requesting pharmacy current quantity, previous info, removable request lines with reason, and multi-batch allocations when serving. |
| Transfer approval scope | Pharmacy transfer approval is restricted to the request's supplying pharmacy. Users only see and can approve/reject requested transfers where their current main or dispensing pharmacy is the transfer source. |
| Dispensing to dispensing | Source excludes main pharmacy and the current active pharmacy. The receiving pharmacy is the active pharmacy. |
| Return to main | Main pharmacy initiates return request; selected dispensing pharmacy approves transfer back or rejects. |
| Transfer PDFs | Transferred requests can be downloaded as PDF from transfer history/review pages. |
| Multi-batch transfer | The transfer form can allocate one requested line across more than one source batch. Validation should compare the sum of allocations with requested/approved quantity, not each batch allocation against the full line quantity. |
### 12.6 Department Store Workflows
Department workflows are controlled by `Task: rwandapharmacymanagement.departmentStore` for the department workspace. The current configuration avoids user-to-department assignment; access is role/privilege based.
| Page | Use |
| --- | --- |
| Department store workspace | Single workspace for department users to request from pharmacy, view department stock, issue internally, and access request PDFs. |
| Department bulk request | Department requests items from pharmacy. The request page can autofill from dispensed items when full visibility exists, but values remain editable. |
| Department finance approval | Finance reviews department requests before pharmacy dispensing when workflow mode requires finance approval. |
| Department pharmacy dispense | Pharmacy dispenses approved department requests from the active dispensing pharmacy only. Batch choices must come from that dispensing pharmacy, not main pharmacy. |
| Department stock issue | Department users can issue more than one internal item at the same time from department stock. |
The department workflow mode global property is initialized to periodic accountability. Review workflow expectations before go-live.
### 12.7 Supplier, Delivery, and Purchase Order Controls
| Area | Configuration rule |
| --- | --- |
| Purchase order cart | Main pharmacy users add items from stock/product view. Orders are grouped by program and supplier. |
| Approval | Health Facility Leader privilege approves or rejects purchase orders. Approved orders can be dispatched by API or email depending on supplier setup. |
| Main-only supplier pages | Purchase orders, supplier configuration, product sync, price history, and delivery receiving are main-pharmacy workflows and should only be visible when the current OpenMRS location maps to the main pharmacy. |
| Delivery receiving | Main pharmacy receives from RMS/API order details, uploaded supplier template, or direct entry. Receipt proof PDF is available after successful receipt. |
| Supplier templates | Templates contain example format rows and support blank/N/A expiry for items configured not to require expiry. |
### 12.8 Reports and Exports
| Report | Configuration rule |
| --- | --- |
| Pharmacy stock | Requires active pharmacy locations and current inventory balances. |
| Overall hospital stock | Sums stock across pharmacy locations. Main-pharmacy scoped in navigation. |
| Inventory movement | Requires stock transactions. Used to audit received, issued, adjusted, and transferred quantities. |
| Item movement | Shows one item across one or all pharmacies, including batch, expiry, received, issued, balance, and months of stock. Patient dispense movements are stock transactions; new patient dispense rows store the dispensing request id in `reference_number`. |
| Dispensing report | Uses dispensing records for patient dispensing activity. |
| Patient request report | Uses patient prescription and consumable request records. It is listed under Reports, not the Dispensing navigation block. |
| Payment exception report | Uses payment exception approvals and related requests. |
## 13. Alerts Dashboard Configuration
The Alerts Dashboard contains two separate alert tables: **Expiration Alerts** and **Stockout and Low Stock Alerts**. Both tables support filters and PDF export.
### 13.1 Expiration Alerts
Expiration alerts are calculated at batch level from current inventory balances.
| Setting/filter | Use |
| --- | --- |
| Pharmacy location | Optional. When selected, only batches in that pharmacy are shown. When blank, batches from all pharmacy locations are shown. |
| Item name/code | Optional item search. |
| Category | Optional item category filter. |
| Expiry Alert Days | Looks forward from today. A value of 90 shows expired batches and batches expiring in the next 90 days. |
| Expiry status | Can show all expiry alerts, expired only, or expiring soon only. |
Only batches with positive quantity on hand and an expiry date are included.
### 13.2 Stockout and Low Stock Alerts
Stockout and low stock alerts are calculated at item/location level when a pharmacy location is selected, and at whole-hospital item level when no location is selected.
| Setting/filter | Use |
| --- | --- |
| Pharmacy location | Optional. When blank, the report sums stock and consumption across the whole hospital. |
| Days of Stock Alert Days | Threshold for Low Stock. Example: 14 means alert when current stock is expected to last 14 days or less. |
| Consumption Lookback Days | Number of past days used to calculate average daily consumption. Default is 90. |
| Stock status | Can show stockout and low stock together, stockout only, or low stock only. |
The table only considers items that have ever had inventory activity: an inventory balance row or a stock transaction. It does not list every item in `rpm_item`.
```
average daily consumption = consumed quantity during lookback / consumption lookback days
days of stock = current quantity / average daily consumption
```
Items with current quantity of zero or less are shown as **Stockout**. Items with positive stock and days of stock less than or equal to the threshold are shown as **Low stock**.
### 13.3 PDF Export
The **Download PDF** button exports both alert tables using the same filters selected on the page.
## 14. Workflow Setup Checklist
| Workflow | Required configuration before testing |
| --- | --- |
| Prescription request | Product master pulled; product linked to OpenMRS Drug/concept; local item exists; stock exists; billing mapping configured if billable; patient has visit; app action installed. |
| Consumable request | Product master pulled; item material type is consumable; local item exists; concept/billing mapping configured; app action installed. |
| Lab request | Lab concept set configured; lab request app installed; lab location mapped to pharmacy tag/location; stock exists. |
| General pharmacy request | Pharmacies created; stock exists in requested pharmacy; app installed; active user has dashboard privilege. |
| Department store | Department store privilege assigned; department request workflow mode reviewed; department stock and active dispensing pharmacy mappings exist. |
| Patient queue | OpenMRS location linked to pharmacy or tag; pharmacy selected where multiple; billing property configured; UPID identifier type configured if UPID search is expected. |
| Patient queue cleanup | Manage settings privilege assigned; operational rules agreed for date ranges and reason text before marking old pending requests as not served. |
| Main to dispensing | Main store exists; dispensing pharmacy exists; Head of Finance role assigned; main stock exists; approval users log in at the supplying pharmacy location. |
| Return to main | Main store active; dispensing pharmacies active; current main pharmacy can request; dispensing pharmacy can approve. |
| Dispensing to dispensing | At least two active dispensing pharmacies; stock exists in source; user active pharmacy is selected. |
| Purchase order | Product master and prices pulled; supplier configured; facility ID and hospital name set; Health Facility Leader approval role assigned; SMTP/API configured; current location maps to main pharmacy. |
| Delivery receive | Supplier/order exists; item mappings exist; expiry rules set; templates use accepted format. |
| Payment exception | Exception approver role assigned; payment exception report access tested; dispensing staff understand when exceptions are allowed. |
| Reports and alerts | Stock transactions or inventory balances exist; items and pharmacies are active; alert global properties are configured; user has stock/report access. |
## 15. Validation Queries for Troubleshooting
### 15.1 Patient request and bill link
```sql
select patient_dispensing_request_id, patient_id, patient_bill_id, billable,
       payment_confirmed, request_status, request_type
from rpm_patient_dispensing_request
where patient_dispensing_request_id = <REQUEST_ID>;
```
### 15.2 Patient bill paid flag
```sql
select patient_bill_id, is_paid
from moh_bill_patient_bill
where patient_bill_id = <PATIENT_BILL_ID>;
```
### 15.3 Request lines
```sql
select patient_dispensing_request_line_id, patient_dispensing_request_id, line_status
from rpm_patient_dispensing_request_line
where patient_dispensing_request_id = <REQUEST_ID>;
```
### 15.4 Duplicate patient dispense check
```sql
select dr.patient_dispensing_request_id,
       dr.dispense_record_id,
       dr.date_created,
       dr.creator,
       dr.dispensing_pharmacy_location_id,
       drl.dispense_record_line_id,
       drl.item_id,
       drl.quantity_dispensed,
       drl.batch_number
from rpm_dispense_record dr
join rpm_dispense_record_line drl
  on drl.dispense_record_id = dr.dispense_record_id
where dr.patient_dispensing_request_id = <REQUEST_ID>
  and dr.voided = 0
  and drl.voided = 0
order by dr.date_created, dr.dispense_record_id;
```
### 15.5 Inventory by item and pharmacy
```sql
select i.name, b.pharmacy_location_id, b.batch_number, b.expiry_date, b.quantity_on_hand
from rpm_inventory_balance b
join rpm_item i on i.item_id = b.item_id
where i.code = '<NPC_CODE>' and b.voided = 0;
```
## 16. Final Go-Live Test Script
1. Login as administrator and verify global properties, roles, app links, and locations.
2. Pull master products and prices from RMS.
3. Create or verify local items for one drug and one consumable.
4. Receive stock into main pharmacy.
5. Transfer stock from main to dispensing through approval.
6. Create a prescription for a patient with an active visit and test SMS/logs if enabled.
7. Pay or link the patient bill and confirm the patient queue updates.
8. Dispense the prescription.
9. Create a consumable request and confirm it appears in queue even if mapping is missing.
10. Create a general pharmacy item request and approve from its queue.
11. Create a department bulk request, approve if required, dispense from the active dispensing pharmacy, and issue multiple internal department items.
12. Create a lab request and approve from lab queue.
13. Create a return-to-main request from main and approve it from dispensing.
14. Create a purchase order, edit prices, approve, and dispatch to supplier/API/email.
15. Receive delivery and verify stock balances and movement reports.
16. Export item movement report to Excel/PDF and confirm receiving/issuing pharmacy columns.
17. Hide one item, confirm requester-facing pages show it unavailable, and confirm patient queue cannot dispense it.
18. Change a batch expiry date from Batch expiry control and confirm the alert/report output changes accordingly.
