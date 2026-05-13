package org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto;

import java.util.List;

public class SupplierProductMasterProgram {

    private String program;
    private List<SupplierProductMasterItem> items;

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public List<SupplierProductMasterItem> getItems() {
        return items;
    }

    public void setItems(List<SupplierProductMasterItem> items) {
        this.items = items;
    }
}
