# OpenMRS Location, Location Tag, and Pharmacy Location

```mermaid
erDiagram
    OPENMRS_LOCATION {
        int location_id PK
        string name
    }

    OPENMRS_LOCATION_TAG {
        int location_tag_id PK
        string name
    }

    RPM_PHARMACY_LOCATION {
        int pharmacy_location_id PK
        string name
        string code
        int location_id FK
        int location_tag_id FK
        string location_type
        boolean main_store
        boolean active
    }

    RPM_PHARMACY_LOCATION_LOCATION_TAG {
        int pharmacy_location_id FK
        int location_tag_id FK
    }

    OPENMRS_LOCATION ||--o{ RPM_PHARMACY_LOCATION : "mapped by location_id"
    OPENMRS_LOCATION_TAG ||--o{ RPM_PHARMACY_LOCATION : "primary tag via location_tag_id"
    RPM_PHARMACY_LOCATION ||--o{ RPM_PHARMACY_LOCATION_LOCATION_TAG : "has tags"
    OPENMRS_LOCATION_TAG ||--o{ RPM_PHARMACY_LOCATION_LOCATION_TAG : "tag assigned"
```

## Meaning in the Module

```mermaid
flowchart LR
    A[OpenMRS Location] -->|physical/session/encounter location| C[Pharmacy Location]
    B[OpenMRS Location Tag] -->|categorizes OpenMRS locations| C
    B -->|many tags supported| D[rpm_pharmacy_location_location_tag]
    C -->|inventory, dispensing, transfers, reports| E[Pharmacy workflows]
```

## Current Code Mapping

- `PharmacyLocation.location` maps to `org.openmrs.Location` using `rpm_pharmacy_location.location_id`.
- `PharmacyLocation.locationTag` maps to one primary `org.openmrs.LocationTag` using `rpm_pharmacy_location.location_tag_id`.
- `PharmacyLocation.locationTags` maps to many `org.openmrs.LocationTag` records using `rpm_pharmacy_location_location_tag`.
- The location admin page currently saves selected OpenMRS location tags into `locationTags`, then sets `locationTag` to the first selected tag for backward compatibility.

Relevant files:

- `api/src/main/java/org/openmrs/module/rwandapharmacymanagement/model/PharmacyLocation.java`
- `api/src/main/resources/PharmacyLocation.hbm.xml`
- `omod/src/main/java/org/openmrs/module/rwandapharmacymanagement/page/controller/location/ListPageController.java`
- `omod/src/main/webapp/pages/location/list.gsp`
