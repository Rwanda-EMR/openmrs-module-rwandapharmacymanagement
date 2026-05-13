package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType;

import java.util.HashSet;
import java.util.Set;

public class PharmacyLocation extends BaseTrackedEntity {

    private Integer pharmacyLocationId;
    private String name;
    private String code;
    private Location location;
    private LocationTag locationTag;
    private Set<LocationTag> locationTags = new HashSet<LocationTag>();
    private PharmacyLocationType locationType;
    private Boolean mainStore = Boolean.FALSE;
    private Boolean active = Boolean.TRUE;

    public Integer getPharmacyLocationId() {
        return pharmacyLocationId;
    }

    public void setPharmacyLocationId(Integer pharmacyLocationId) {
        this.pharmacyLocationId = pharmacyLocationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public LocationTag getLocationTag() {
        return locationTag;
    }

    public void setLocationTag(LocationTag locationTag) {
        this.locationTag = locationTag;
    }

    public Set<LocationTag> getLocationTags() {
        return locationTags;
    }

    public void setLocationTags(Set<LocationTag> locationTags) {
        this.locationTags = locationTags;
    }

    public PharmacyLocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(PharmacyLocationType locationType) {
        this.locationType = locationType;
    }

    public Boolean getMainStore() {
        return mainStore;
    }

    public void setMainStore(Boolean mainStore) {
        this.mainStore = mainStore;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
