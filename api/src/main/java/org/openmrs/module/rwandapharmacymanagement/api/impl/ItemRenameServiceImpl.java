package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.Drug;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.BillingPriceService;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.api.ItemRenameService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.db.PharmacyDao;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.ItemRenameProposal;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.ItemRenameProposalStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Transactional
public class ItemRenameServiceImpl extends BaseOpenmrsService implements ItemRenameService {

    private PharmacyDao pharmacyDao;
    private PharmacyService pharmacyService;
    private BillingPriceService billingPriceService;
    private IntegrationLogService integrationLogService;
    private ConceptService conceptService;

    public void setConceptService(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    public void setPharmacyDao(PharmacyDao pharmacyDao) {
        this.pharmacyDao = pharmacyDao;
    }

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    public void setBillingPriceService(BillingPriceService billingPriceService) {
        this.billingPriceService = billingPriceService;
    }

    public void setIntegrationLogService(IntegrationLogService integrationLogService) {
        this.integrationLogService = integrationLogService;
    }

    @Override
    public void proposeSupplierRename(Item item, ProductMasterItem productMasterItem) {
        if (item == null || item.getItemId() == null || productMasterItem == null) {
            return;
        }
        String supplierName = trim(productMasterItem.getItemName());
        if (supplierName.isEmpty() || sameName(item.getName(), supplierName)) {
            return;
        }
        String program = trim(productMasterItem.getProgram());
        for (ItemRenameProposal rejected : pharmacyDao.getItemRenameProposals(item.getItemId(), ItemRenameProposalStatus.REJECTED)) {
            if (sameName(rejected.getProposedName(), supplierName) && program.equalsIgnoreCase(trim(rejected.getProgram()))) {
                return;
            }
        }
        ItemRenameProposal proposal = null;
        for (ItemRenameProposal pending : pharmacyDao.getItemRenameProposals(item.getItemId(), ItemRenameProposalStatus.PENDING)) {
            if (ItemRenameProposal.SOURCE_SUPPLIER_SYNC.equals(pending.getSource())
                    && program.equalsIgnoreCase(trim(pending.getProgram()))) {
                proposal = pending;
                break;
            }
        }
        if (proposal == null) {
            proposal = newProposal(item, ItemRenameProposal.SOURCE_SUPPLIER_SYNC);
        }
        proposal.setProductMasterItem(productMasterItem);
        proposal.setNpcCode(trim(productMasterItem.getNpcCode()));
        proposal.setProgram(program);
        proposal.setCurrentName(item.getName());
        proposal.setProposedName(supplierName);
        proposal.setReason("Supplier product master name changed");
        pharmacyDao.saveItemRenameProposal(proposal);
    }

    @Override
    public int runConsistencyCheck() {
        requireManageSettingsPrivilege();
        Map<String, List<Integer>> billingServicesByName = pharmacyDao.getNonRetiredFacilityServiceIdsByName(null);
        int flagged = 0;
        for (Item item : pharmacyService.getAllItems()) {
            String itemName = trim(item == null ? null : item.getName());
            if (itemName.isEmpty()) {
                continue;
            }
            List<String> problems = new ArrayList<String>();
            Drug drug = item.getDrug();
            if (drug != null && !itemName.equals(trim(drug.getName()))) {
                problems.add("OpenMRS drug is named '" + trim(drug.getName()) + "'");
            }
            List<Integer> servicesNamedLikeItem = billingServicesByName.get(billingKey(itemName));
            if (servicesNamedLikeItem != null && servicesNamedLikeItem.size() > 1) {
                problems.add(servicesNamedLikeItem.size() + " MOH Billing services are named '" + itemName + "'");
            } else if (servicesNamedLikeItem == null && drug != null) {
                List<Integer> servicesNamedLikeDrug = billingServicesByName.get(billingKey(drug.getName()));
                if (servicesNamedLikeDrug != null && !servicesNamedLikeDrug.isEmpty()) {
                    problems.add("MOH Billing service is named after the drug '" + trim(drug.getName()) + "'");
                }
            }
            if (problems.isEmpty()) {
                continue;
            }
            ItemRenameProposal proposal = null;
            for (ItemRenameProposal pending : pharmacyDao.getItemRenameProposals(item.getItemId(), ItemRenameProposalStatus.PENDING)) {
                if (ItemRenameProposal.SOURCE_CONSISTENCY_CHECK.equals(pending.getSource())) {
                    proposal = pending;
                    break;
                }
            }
            if (proposal == null) {
                proposal = newProposal(item, ItemRenameProposal.SOURCE_CONSISTENCY_CHECK);
            }
            proposal.setCurrentName(itemName);
            proposal.setProposedName(itemName);
            proposal.setNpcCode(trim(item.getCode()));
            proposal.setReason(truncate(join(problems), 500));
            pharmacyDao.saveItemRenameProposal(proposal);
            flagged++;
        }
        return flagged;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemRenameProposal> getPendingProposals() {
        return pharmacyDao.getItemRenameProposals(null, ItemRenameProposalStatus.PENDING);
    }

    @Override
    public String approveRename(Integer itemId, String newName) {
        requireManageSettingsPrivilege();
        String name = trim(newName);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Enter the name to apply.");
        }
        Item item = itemId == null ? null : pharmacyService.getItem(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Pharmacy item was not found.");
        }
        String oldItemName = trim(item.getName());
        Drug drug = item.getDrug();
        String oldDrugName = drug == null ? "" : trim(drug.getName());

        Item sameNamedItem = pharmacyService.getItemByExactName(name);
        if (sameNamedItem != null && !sameNamedItem.getItemId().equals(item.getItemId())) {
            throw new IllegalArgumentException("Another pharmacy item (" + sameNamedItem.getItemId() + ") is already named '"
                    + name + "'.");
        }
        if (drug != null && !pharmacyDao.getOtherActiveDrugIdsByName(name, drug.getDrugId()).isEmpty()) {
            throw new IllegalArgumentException("Another OpenMRS drug is already named '" + name + "'.");
        }

        Set<String> currentNames = new LinkedHashSet<String>();
        if (!oldItemName.isEmpty()) {
            currentNames.add(oldItemName);
        }
        if (!oldDrugName.isEmpty()) {
            currentNames.add(oldDrugName);
        }
        Set<String> lookupNames = new LinkedHashSet<String>(currentNames);
        lookupNames.add(name);
        Map<String, List<Integer>> billingServicesByName = pharmacyDao.getNonRetiredFacilityServiceIdsByName(lookupNames);
        Set<Integer> currentServiceIds = new LinkedHashSet<Integer>();
        for (String currentName : currentNames) {
            List<Integer> ids = billingServicesByName.get(billingKey(currentName));
            if (ids != null) {
                currentServiceIds.addAll(ids);
            }
        }
        List<Integer> servicesWithNewName = billingServicesByName.get(billingKey(name));
        if (servicesWithNewName != null) {
            for (Integer id : servicesWithNewName) {
                if (!currentServiceIds.contains(id)) {
                    throw new IllegalArgumentException("MOH Billing already has another service named '" + name
                            + "'. Two services with the same name would stop MOH Billing from billing this drug.");
                }
            }
        }
        if (currentServiceIds.size() > 1) {
            throw new IllegalArgumentException(currentServiceIds.size() + " MOH Billing services match "
                    + currentNames + ". Resolve the duplicate billing services before renaming.");
        }

        item.setName(name);
        pharmacyService.saveItem(item);
        if (drug != null && !name.equals(drug.getName())) {
            drug.setName(name);
            conceptService.saveDrug(drug);
        }
        String billingResult;
        if (currentServiceIds.isEmpty()) {
            billingResult = "No MOH Billing service was found; add one from Missing billing mappings.";
        } else {
            Integer serviceId = currentServiceIds.iterator().next();
            billingPriceService.renameFacilityService(serviceId, name);
            billingResult = "MOH Billing service " + serviceId + " renamed.";
        }

        User decidedBy = Context.getAuthenticatedUser();
        Date now = new Date();
        for (ItemRenameProposal pending : pharmacyDao.getItemRenameProposals(item.getItemId(), ItemRenameProposalStatus.PENDING)) {
            pending.setStatus(sameName(pending.getProposedName(), name)
                    ? ItemRenameProposalStatus.APPROVED : ItemRenameProposalStatus.REJECTED);
            pending.setDecidedBy(decidedBy);
            pending.setDateDecided(now);
            pharmacyDao.saveItemRenameProposal(pending);
        }

        String summary = "Item " + item.getItemId() + " renamed from '" + oldItemName + "' to '" + name + "'"
                + (drug == null ? "" : "; drug " + drug.getDrugId() + " renamed from '" + oldDrugName + "'")
                + ". " + billingResult;
        IntegrationLog log = new IntegrationLog();
        log.setIntegrationType(IntegrationType.PRODUCT_MASTER_SYNC);
        log.setStatus(IntegrationStatus.SUCCESS);
        log.setSummary(truncate(summary, 255));
        log.setRequestPayload(summary);
        log.setCreator(decidedBy);
        log.setDateCreated(now);
        integrationLogService.saveLog(log);
        return summary;
    }

    @Override
    public void rejectRenames(Integer itemId) {
        requireManageSettingsPrivilege();
        User decidedBy = Context.getAuthenticatedUser();
        Date now = new Date();
        for (ItemRenameProposal pending : pharmacyDao.getItemRenameProposals(itemId, ItemRenameProposalStatus.PENDING)) {
            pending.setStatus(ItemRenameProposalStatus.REJECTED);
            pending.setDecidedBy(decidedBy);
            pending.setDateDecided(now);
            pharmacyDao.saveItemRenameProposal(pending);
        }
    }

    void requireManageSettingsPrivilege() {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_MANAGE_SETTINGS);
    }

    private ItemRenameProposal newProposal(Item item, String source) {
        ItemRenameProposal proposal = new ItemRenameProposal();
        proposal.setItem(item);
        proposal.setSource(source);
        proposal.setStatus(ItemRenameProposalStatus.PENDING);
        proposal.setCreator(Context.getAuthenticatedUser());
        proposal.setDateCreated(new Date());
        return proposal;
    }

    /**
     * Spacing and case differences alone are not treated as a rename.
     */
    static boolean sameName(String left, String right) {
        return key(left).equals(key(right));
    }

    private static String key(String value) {
        return trim(value).replaceAll("\\s+", " ").toLowerCase(Locale.ENGLISH);
    }

    /**
     * Same key as PharmacyDao.getNonRetiredFacilityServiceIdsByName: MOH Billing matches the stored name, so inner
     * spacing must not be collapsed here.
     */
    private static String billingKey(String value) {
        return trim(value).toLowerCase();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}
