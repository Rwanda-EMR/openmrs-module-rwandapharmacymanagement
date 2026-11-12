package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.ItemRenameProposal;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;

import java.util.List;

/**
 * Reviewed renames of local pharmacy items. MOH Billing bills a drug order by finding a facility service named
 * exactly like the OpenMRS drug, so an approved rename changes the item, its drug and its billing service together.
 */
public interface ItemRenameService extends OpenmrsService {

    /**
     * Records a pending proposal when the supplier's name for a linked product differs from the local item name.
     * A name already rejected for the same program is not proposed again.
     */
    void proposeSupplierRename(Item item, ProductMasterItem productMasterItem);

    /**
     * Proposes fixes for items whose drug or MOH Billing service name differs from the item name.
     * Safe to run repeatedly; returns the number of items flagged.
     */
    int runConsistencyCheck();

    List<ItemRenameProposal> getPendingProposals();

    /**
     * Renames the item, its drug and its MOH Billing service to exactly the given name, all or nothing.
     * Pending proposals for the item are marked approved or rejected. Returns a summary of what changed.
     */
    String approveRename(Integer itemId, String newName);

    void rejectRenames(Integer itemId);
}
