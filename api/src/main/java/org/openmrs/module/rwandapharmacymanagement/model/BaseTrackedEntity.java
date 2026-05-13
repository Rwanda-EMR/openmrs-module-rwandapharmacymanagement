package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.OpenmrsObject;
import org.openmrs.User;

import java.lang.reflect.Field;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class BaseTrackedEntity implements Serializable, OpenmrsObject {

    private User creator;
    private Date dateCreated;
    private User voidedBy;
    private Date dateVoided;
    private String voidReason;
    private Boolean voided = Boolean.FALSE;
    private String uuid = UUID.randomUUID().toString();

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public User getVoidedBy() {
        return voidedBy;
    }

    public void setVoidedBy(User voidedBy) {
        this.voidedBy = voidedBy;
    }

    public Date getDateVoided() {
        return dateVoided;
    }

    public void setDateVoided(Date dateVoided) {
        this.dateVoided = dateVoided;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    public Boolean getVoided() {
        return voided;
    }

    public void setVoided(Boolean voided) {
        this.voided = voided;
    }

    @Override
    public Integer getId() {
        Field idField = getIdField();
        try {
            return (Integer) idField.get(this);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to read id field for " + getClass().getName(), e);
        }
    }

    @Override
    public void setId(Integer id) {
        Field idField = getIdField();
        try {
            idField.set(this, id);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to write id field for " + getClass().getName(), e);
        }
    }

    @Override
    public String getUuid() {
        if (uuid == null || uuid.trim().isEmpty()) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    private Field getIdField() {
        Class<?> current = getClass();
        while (current != null && !Object.class.equals(current)) {
            for (Field field : current.getDeclaredFields()) {
                if (Integer.class.equals(field.getType()) && field.getName().endsWith("Id")) {
                    field.setAccessible(true);
                    return field;
                }
            }
            current = current.getSuperclass();
        }

        throw new IllegalStateException("No Integer id field ending with 'Id' found for " + getClass().getName());
    }
}
