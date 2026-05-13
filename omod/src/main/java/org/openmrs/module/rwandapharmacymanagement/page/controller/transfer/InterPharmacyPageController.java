package org.openmrs.module.rwandapharmacymanagement.page.controller.transfer;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class InterPharmacyPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    HttpSession session) {
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        List<PharmacyLocation> pharmacies = getEligiblePharmacies(pharmacyService, activePharmacy);
        model.addAttribute("sourcePharmacyLocations", pharmacies);
        model.addAttribute("receivingPharmacy", activePharmacy);
        Map<Integer, InterPharmacyTransfer> requestsById = new LinkedHashMap<Integer, InterPharmacyTransfer>();
        if (activePharmacy != null && activePharmacy.getPharmacyLocationId() != null) {
            for (InterPharmacyTransfer transfer : inventoryService.getTransfers(null, activePharmacy.getPharmacyLocationId(), null, 200)) {
                if (transfer != null && transfer.getInterPharmacyTransferId() != null) {
                    requestsById.put(transfer.getInterPharmacyTransferId(), transfer);
                }
            }
            for (InterPharmacyTransfer transfer : inventoryService.getTransfers(null, null, activePharmacy.getPharmacyLocationId(), 200)) {
                if (transfer != null && transfer.getInterPharmacyTransferId() != null) {
                    requestsById.put(transfer.getInterPharmacyTransferId(), transfer);
                }
            }
        }
        List<InterPharmacyTransfer> requests = new ArrayList<InterPharmacyTransfer>(requestsById.values());
        sortTransfers(requests);
        model.addAttribute("requests", requests);
        model.addAttribute("stockByPharmacy", buildStockByPharmacy(pharmacies, inventoryService));
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("inventoryService") InventoryService inventoryService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "transferId", required = false) Integer transferId,
                       @RequestParam(value = "sourcePharmacyLocationId", required = false) Integer sourcePharmacyLocationId,
                       @RequestParam(value = "destinationPharmacyLocationId", required = false) Integer destinationPharmacyLocationId,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       @RequestParam(value = "expiryDate", required = false) String[] expiryDates,
                       @RequestParam(value = "reason", required = false) String reason,
                       @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                       HttpSession session,
                       UiUtils ui) throws ParseException {
        PharmacyLocation activePharmacy = CurrentPharmacySupport.requireCurrentPharmacy(pharmacyService, session);
        if ("approve".equalsIgnoreCase(action) && transferId != null) {
            inventoryService.approveTransfer(transferId);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/interPharmacy");
        }
        if ("reject".equalsIgnoreCase(action) && transferId != null) {
            inventoryService.rejectTransfer(transferId, rejectionReason);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/interPharmacy");
        }
        if (("transfer".equalsIgnoreCase(action) || "serve".equalsIgnoreCase(action)) && transferId != null) {
            inventoryService.serveTransfer(transferId, quantities, batchNumbers, parseDates(expiryDates));
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/interPharmacy");
        }
        InterPharmacyTransfer transfer = new InterPharmacyTransfer();
        transfer.setSourcePharmacyLocation(pharmacyService.getPharmacyLocation(sourcePharmacyLocationId));
        transfer.setDestinationPharmacyLocation(activePharmacy);
        validateTransferEndpoints(transfer.getSourcePharmacyLocation(), transfer.getDestinationPharmacyLocation());
        transfer.setReason(reason);
        transfer.setTransferStatus("REQUESTED");
        transfer.setCreator(Context.getAuthenticatedUser());
        transfer.setDateCreated(new java.util.Date());
        transfer.setLines(buildLines(pharmacyService, transfer, itemIds, quantities, batchNumbers, expiryDates));
        inventoryService.requestTransfer(transfer);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/interPharmacy");
    }

    private List<PharmacyLocation> getEligiblePharmacies(PharmacyService pharmacyService, PharmacyLocation activePharmacy) {
        List<PharmacyLocation> pharmacies = new ArrayList<PharmacyLocation>();
        Integer activePharmacyId = activePharmacy == null ? null : activePharmacy.getPharmacyLocationId();
        boolean activeIsMainStore = activePharmacy != null && Boolean.TRUE.equals(activePharmacy.getMainStore());
        for (PharmacyLocation location : pharmacyService.getAllPharmacyLocations()) {
            if (location == null || !Boolean.TRUE.equals(location.getActive()) || location.getPharmacyLocationId() == null) {
                continue;
            }
            if (activePharmacyId != null && activePharmacyId.equals(location.getPharmacyLocationId())) {
                continue;
            }
            if (!activeIsMainStore && Boolean.TRUE.equals(location.getMainStore())) {
                continue;
            }
            pharmacies.add(location);
        }
        return pharmacies;
    }

    private boolean isRelevantTransfer(InterPharmacyTransfer transfer, PharmacyLocation activePharmacy) {
        if (activePharmacy == null || activePharmacy.getPharmacyLocationId() == null) {
            return false;
        }
        Integer activeId = activePharmacy.getPharmacyLocationId();
        Integer sourceId = transfer.getSourcePharmacyLocation() == null ? null : transfer.getSourcePharmacyLocation().getPharmacyLocationId();
        Integer destinationId = transfer.getDestinationPharmacyLocation() == null ? null : transfer.getDestinationPharmacyLocation().getPharmacyLocationId();
        return activeId.equals(sourceId) || activeId.equals(destinationId);
    }

    private Map<Integer, List<Map<String, Object>>> buildStockByPharmacy(List<PharmacyLocation> pharmacies, InventoryService inventoryService) {
        Map<Integer, List<Map<String, Object>>> stockByPharmacy = new LinkedHashMap<Integer, List<Map<String, Object>>>();
        for (PharmacyLocation pharmacy : pharmacies) {
            List<Map<String, Object>> itemRows = new ArrayList<Map<String, Object>>();
            Map<Integer, Map<String, Object>> byItem = new LinkedHashMap<Integer, Map<String, Object>>();
            for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(pharmacy.getPharmacyLocationId())) {
                if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                    continue;
                }
                Integer itemId = balance.getItem().getItemId();
                Map<String, Object> itemRow = byItem.get(itemId);
                if (itemRow == null) {
                    itemRow = new LinkedHashMap<String, Object>();
                    itemRow.put("itemId", itemId);
                    itemRow.put("name", balance.getItem().getName());
                    itemRow.put("available", BigDecimal.ZERO);
                    itemRow.put("unitPrice", balance.getUnitPrice());
                    itemRow.put("batches", new ArrayList<Map<String, Object>>());
                    byItem.put(itemId, itemRow);
                    itemRows.add(itemRow);
                }
                if (itemRow.get("unitPrice") == null && balance.getUnitPrice() != null) {
                    itemRow.put("unitPrice", balance.getUnitPrice());
                }
                itemRow.put("available", ((BigDecimal) itemRow.get("available")).add(balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand()));
                Map<String, Object> batch = new LinkedHashMap<String, Object>();
                batch.put("batchNumber", balance.getBatchNumber());
                batch.put("expiryDate", balance.getExpiryDate());
                batch.put("available", balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand());
                batch.put("unitPrice", balance.getUnitPrice());
                ((List<Map<String, Object>>) itemRow.get("batches")).add(batch);
            }
            stockByPharmacy.put(pharmacy.getPharmacyLocationId(), itemRows);
        }
        return stockByPharmacy;
    }

    private Date[] parseDates(String[] expiryDates) throws ParseException {
        if (expiryDates == null) {
            return null;
        }
        Date[] parsedDates = new Date[expiryDates.length];
        for (int i = 0; i < expiryDates.length; i++) {
            String expiryDate = expiryDates[i];
            if (expiryDate != null && !expiryDate.trim().isEmpty()) {
                parsedDates[i] = new SimpleDateFormat("yyyy-MM-dd").parse(expiryDate);
            }
        }
        return parsedDates;
    }

    private void validateTransferEndpoints(PharmacyLocation source, PharmacyLocation destination) {
        if (source == null || destination == null) {
            throw new IllegalArgumentException("Source and destination pharmacies are required");
        }
        if (source.getPharmacyLocationId() != null && source.getPharmacyLocationId().equals(destination.getPharmacyLocationId())) {
            throw new IllegalArgumentException("Source and destination pharmacies must be different");
        }
    }

    private List<InterPharmacyTransferLine> buildLines(PharmacyService pharmacyService,
                                                       InterPharmacyTransfer transfer,
                                                       Integer[] itemIds,
                                                       BigDecimal[] quantities,
                                                       String[] batchNumbers,
                                                       String[] expiryDates) throws ParseException {
        List<InterPharmacyTransferLine> lines = new ArrayList<InterPharmacyTransferLine>();
        if (itemIds == null || quantities == null) {
            return lines;
        }
        for (int i = 0; i < itemIds.length; i++) {
            Integer itemId = itemIds[i];
            BigDecimal quantity = quantities.length > i ? quantities[i] : null;
            if (itemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            InterPharmacyTransferLine line = new InterPharmacyTransferLine();
            line.setInterPharmacyTransfer(transfer);
            line.setItem(pharmacyService.getItem(itemId));
            line.setQuantity(quantity);
            line.setBatchNumber(batchNumbers != null && batchNumbers.length > i ? batchNumbers[i] : null);
            String expiryDate = expiryDates != null && expiryDates.length > i ? expiryDates[i] : null;
            if (expiryDate != null && !expiryDate.trim().isEmpty()) {
                line.setExpiryDate(new SimpleDateFormat("yyyy-MM-dd").parse(expiryDate));
            }
            lines.add(line);
        }
        return lines;
    }

    private void sortTransfers(List<InterPharmacyTransfer> requests) {
        Collections.sort(requests, new Comparator<InterPharmacyTransfer>() {
            @Override
            public int compare(InterPharmacyTransfer left, InterPharmacyTransfer right) {
                int statusCompare = Integer.valueOf(statusRank(left)).compareTo(statusRank(right));
                if (statusCompare != 0) {
                    return statusCompare;
                }
                Date leftDate = left == null ? null : left.getDateCreated();
                Date rightDate = right == null ? null : right.getDateCreated();
                if (leftDate == null && rightDate == null) {
                    return 0;
                }
                if (leftDate == null) {
                    return 1;
                }
                if (rightDate == null) {
                    return -1;
                }
                return rightDate.compareTo(leftDate);
            }
        });
    }

    private int statusRank(InterPharmacyTransfer transfer) {
        String status = transfer == null ? null : transfer.getTransferStatus();
        if ("REQUESTED".equalsIgnoreCase(status)) {
            return 0;
        }
        if ("APPROVED".equalsIgnoreCase(status)) {
            return 1;
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            return 2;
        }
        if ("SERVED".equalsIgnoreCase(status)) {
            return 3;
        }
        return 4;
    }
}
