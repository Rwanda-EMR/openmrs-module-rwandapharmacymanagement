package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.enums.ItemRenameProposalStatus;

import java.util.Date;

/**
 * A proposed new name for a local pharmacy item. Approving it renames the item, its OpenMRS drug and its
 * MOH Billing service together so billing keeps finding the service by name.
 */
public class ItemRenameProposal extends BaseTrackedEntity {

    public static final String SOURCE_SUPPLIER_SYNC = "SUPPLIER_SYNC";
    public static final String SOURCE_CONSISTENCY_CHECK = "CONSISTENCY_CHECK";

    private Integer itemRenameProposalId;
    private Item item;
    private ProductMasterItem productMasterItem;
    private String npcCode;
    private String program;
    private String currentName;
    private String proposedName;
    private String source;
    private ItemRenameProposalStatus status = ItemRenameProposalStatus.PENDING;
    private String reason;
    private User decidedBy;
    private Date dateDecided;

    public Integer getItemRenameProposalId() {
        return itemRenameProposalId;
    }

    public void setItemRenameProposalId(Integer itemRenameProposalId) {
        this.itemRenameProposalId = itemRenameProposalId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ProductMasterItem getProductMasterItem() {
        return productMasterItem;
    }

    public void setProductMasterItem(ProductMasterItem productMasterItem) {
        this.productMasterItem = productMasterItem;
    }

    public String getNpcCode() {
        return npcCode;
    }

    public void setNpcCode(String npcCode) {
        this.npcCode = npcCode;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getCurrentName() {
        return currentName;
    }

    public void setCurrentName(String currentName) {
        this.currentName = currentName;
    }

    public String getProposedName() {
        return proposedName;
    }

    public void setProposedName(String proposedName) {
        this.proposedName = proposedName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ItemRenameProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ItemRenameProposalStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(User decidedBy) {
        this.decidedBy = decidedBy;
    }

    public Date getDateDecided() {
        return dateDecided;
    }

    public void setDateDecided(Date dateDecided) {
        this.dateDecided = dateDecided;
    }
}
