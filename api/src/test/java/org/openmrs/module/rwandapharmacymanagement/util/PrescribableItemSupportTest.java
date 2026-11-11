package org.openmrs.module.rwandapharmacymanagement.util;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Drug;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PrescribableItemInfo;
import org.openmrs.module.rwandapharmacymanagement.model.Item;

import java.math.BigDecimal;

public class PrescribableItemSupportTest {

    @Test
    public void unlinkedDispensableDrugItemShouldBePrescribable() {
        Assert.assertTrue(PrescribableItemSupport.isPrescribable(drugItem(), null, null));
    }

    @Test
    public void itemWithoutDrugShouldNotBePrescribable() {
        Item item = drugItem();
        item.setDrug(null);

        Assert.assertFalse(PrescribableItemSupport.isPrescribable(item, null, BigDecimal.TEN));
    }

    @Test
    public void nonDispensableOrVoidedItemShouldNotBePrescribable() {
        Item notDispensable = drugItem();
        notDispensable.setDispensable(Boolean.FALSE);
        Item voided = drugItem();
        voided.setVoided(Boolean.TRUE);

        Assert.assertFalse(PrescribableItemSupport.isPrescribable(notDispensable, null, BigDecimal.TEN));
        Assert.assertFalse(PrescribableItemSupport.isPrescribable(voided, null, BigDecimal.TEN));
    }

    @Test
    public void linkedItemShouldBePrescribableWhenAnyLinkedRowIsActive() {
        PrescribableItemInfo info = new PrescribableItemInfo(1);
        info.addLinkedRow(false, true, null, null);
        info.addLinkedRow(true, true, null, null);

        Assert.assertTrue(PrescribableItemSupport.isPrescribable(drugItem(), info, null));
    }

    @Test
    public void discontinuedLinkedItemShouldStayPrescribableWhileStockRemains() {
        PrescribableItemInfo info = new PrescribableItemInfo(1);
        info.addLinkedRow(false, true, null, null);

        Assert.assertTrue(PrescribableItemSupport.isPrescribable(drugItem(), info, new BigDecimal("3")));
        Assert.assertFalse(PrescribableItemSupport.isPrescribable(drugItem(), info, BigDecimal.ZERO));
        Assert.assertFalse(PrescribableItemSupport.isPrescribable(drugItem(), info, null));
    }

    @Test
    public void linkedItemWithoutMedicineRowShouldNotBePrescribable() {
        PrescribableItemInfo info = new PrescribableItemInfo(1);
        info.addLinkedRow(true, false, null, null);

        Assert.assertFalse(PrescribableItemSupport.isPrescribable(drugItem(), info, BigDecimal.TEN));
    }

    @Test
    public void requirePrescribableShouldExplainDiscontinuedItemWithoutStock() {
        PrescribableItemInfo info = new PrescribableItemInfo(1);
        info.addLinkedRow(false, true, null, null);
        try {
            PrescribableItemSupport.requirePrescribable(drugItem(), info, null);
            Assert.fail("Expected discontinued item without stock to be rejected");
        }
        catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("discontinued"));
        }
    }

    @Test
    public void infoShouldCollectEveryRequiredPrivilegeAcrossLinkedRows() {
        PrescribableItemInfo info = new PrescribableItemInfo(1);
        info.addLinkedRow(true, true, "Prescribe TB drugs", "TB clinicians only");
        info.addLinkedRow(true, true, "Prescribe controlled drugs", "");
        info.addLinkedRow(true, true, "Prescribe TB drugs", "duplicate");
        info.addLinkedRow(true, true, " ", null);

        Assert.assertEquals(2, info.getRestrictionReasonsByPrivilege().size());
        Assert.assertEquals("TB clinicians only", info.getRestrictionReasonsByPrivilege().get("Prescribe TB drugs"));
        Assert.assertTrue(info.getRestrictionReasonsByPrivilege().containsKey("Prescribe controlled drugs"));
    }

    private Item drugItem() {
        Item item = new Item();
        item.setItemId(1);
        item.setName("Amoxicillin 500mg Caps");
        item.setDispensable(Boolean.TRUE);
        item.setVoided(Boolean.FALSE);
        Drug drug = new Drug();
        drug.setDrugId(10);
        item.setDrug(drug);
        return item;
    }
}
