package org.openmrs.module.rwandapharmacymanagement.page.controller.supplier;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.module.rwandapharmacymanagement.web.dto.DeliveryReceiptImportItem;
import org.openmrs.module.rwandapharmacymanagement.web.dto.DeliveryReceiptImportRequest;
import org.openmrs.module.rwandapharmacymanagement.web.support.DeliveryReceiptSupport;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.HashSet;
import java.util.UUID;
import java.util.Set;
import java.nio.charset.StandardCharsets;

@Controller
public class DeliveryReceivePageController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FLASH_RECEIVED = "rpm.deliveryReceive.received";
    private static final String FLASH_UPLOAD_RECEIVED = "rpm.deliveryReceive.uploadReceived";
    private static final String FLASH_DELIVERY_RECEIPT_ID = "rpm.deliveryReceive.deliveryReceiptId";
    private static final String FLASH_ERROR_MESSAGE = "rpm.deliveryReceive.errorMessage";

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                    @RequestParam(value = "view", required = false) String view,
                    @RequestParam(value = "source", required = false) String source,
                    @RequestParam(value = "purchaseOrderId", required = false) Integer purchaseOrderId,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "fetchRms", required = false) String fetchRms,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        model.addAttribute("mainStock", inventoryService.getInventoryForPharmacy(null));
        model.addAttribute("items", Collections.emptyList());
        model.addAttribute("productMasterItems", pharmacyService.getAllProductMasterItems());
        List<PharmacyLocation> pharmacyLocations = pharmacyService.getAllPharmacyLocations();
        model.addAttribute("pharmacyLocations", pharmacyLocations);
        PharmacyLocation defaultReceiveLocation = (PharmacyLocation) model.getAttribute("activePharmacy");
        if (defaultReceiveLocation == null) {
            defaultReceiveLocation = getDefaultReceiveLocation(pharmacyLocations);
        }
        model.addAttribute("defaultReceiveLocation", defaultReceiveLocation);

        List<PurchaseOrder> approvedPurchaseOrders = getApprovedPurchaseOrders(pharmacyService);
        List<PurchaseOrder> rmsPurchaseOrders = new ArrayList<PurchaseOrder>();
        List<PurchaseOrder> externalPurchaseOrders = new ArrayList<PurchaseOrder>();
        for (PurchaseOrder purchaseOrder : approvedPurchaseOrders) {
            if (isRmsOrder(purchaseOrder)) {
                rmsPurchaseOrders.add(purchaseOrder);
            } else {
                externalPurchaseOrders.add(purchaseOrder);
            }
        }

        PurchaseOrder selectedPurchaseOrder = purchaseOrderId == null ? null : pharmacyService.getPurchaseOrder(purchaseOrderId);
        if (selectedPurchaseOrder != null && (!"APPROVED".equalsIgnoreCase(selectedPurchaseOrder.getApprovalStatus())
                || !isOpenForReceiving(selectedPurchaseOrder))) {
            selectedPurchaseOrder = null;
        }
        String resolvedView = normalizeView(view, selectedPurchaseOrder);
        String resolvedSource = normalizeSource(source, selectedPurchaseOrder);

        DeliveryReceiptImportRequest fetchedReceipt = null;
        String errorMessage = FlashMessageSupport.consumeString(session, FLASH_ERROR_MESSAGE);
        if (selectedPurchaseOrder != null && "rms".equalsIgnoreCase(resolvedSource) && isTrueFlag(fetchRms)) {
            try {
                fetchedReceipt = supplierIntegrationService.fetchRmsDeliveryDetails(selectedPurchaseOrder);
            }
            catch (RuntimeException e) {
                errorMessage = e.getMessage();
            }
        } else if (selectedPurchaseOrder != null && "rms".equalsIgnoreCase(resolvedSource)) {
            fetchedReceipt = parseFetchedReceipt(selectedPurchaseOrder);
        }

        Map<String, Integer> itemIdByCode = new LinkedHashMap<String, Integer>();
        if (selectedPurchaseOrder != null && selectedPurchaseOrder.getItems() != null) {
            for (org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem purchaseOrderItem : selectedPurchaseOrder.getItems()) {
                Item item = DeliveryReceiptSupport.resolveOrCreateItemByCode(pharmacyService,
                        purchaseOrderItem.getItemCode(), purchaseOrderItem.getItemName(), purchaseOrderItem.getProgram());
                if (item != null && purchaseOrderItem.getItemCode() != null) {
                    itemIdByCode.put(purchaseOrderItem.getItemCode(), item.getItemId());
                }
            }
        }
        if (fetchedReceipt != null && fetchedReceipt.getItems() != null) {
            for (DeliveryReceiptImportItem receiptItem : fetchedReceipt.getItems()) {
                if (receiptItem == null || receiptItem.getNpcCode() == null || itemIdByCode.containsKey(receiptItem.getNpcCode())) {
                    continue;
                }
                Item item = DeliveryReceiptSupport.resolveOrCreateItemByCode(pharmacyService,
                        receiptItem.getNpcCode(), receiptItem.getDescription());
                if (item != null) {
                    itemIdByCode.put(receiptItem.getNpcCode(), item.getItemId());
                }
            }
        }
        model.addAttribute("approvedPurchaseOrders", approvedPurchaseOrders);
        model.addAttribute("rmsPurchaseOrders", rmsPurchaseOrders);
        model.addAttribute("externalPurchaseOrders", externalPurchaseOrders);
        model.addAttribute("rmsPurchaseOrderCount", rmsPurchaseOrders.size());
        model.addAttribute("externalPurchaseOrderCount", externalPurchaseOrders.size());
        model.addAttribute("selectedPurchaseOrder", selectedPurchaseOrder);
        model.addAttribute("selectedView", resolvedView);
        model.addAttribute("selectedSource", resolvedSource);
        model.addAttribute("itemIdByCode", itemIdByCode);
        model.addAttribute("fetchedReceipt", fetchedReceipt);
        model.addAttribute("receiveRows", buildReceiveRows(pharmacyService, selectedPurchaseOrder, fetchedReceipt, itemIdByCode));
        model.addAttribute("received", FlashMessageSupport.consumeBoolean(session, FLASH_RECEIVED));
        model.addAttribute("uploadReceived", FlashMessageSupport.consumeBoolean(session, FLASH_UPLOAD_RECEIVED));
        model.addAttribute("deliveryReceiptId", FlashMessageSupport.consumeInteger(session, FLASH_DELIVERY_RECEIPT_ID));
        model.addAttribute("errorMessage", errorMessage);
    }

    public String post(@SpringBean("inventoryService") InventoryService inventoryService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "view", required = false) String view,
                       @RequestParam(value = "source", required = false) String source,
                       @RequestParam(value = "receiptNumber", required = false) String receiptNumber,
                       @RequestParam(value = "purchaseOrderId", required = false) Integer purchaseOrderId,
                       @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                       @RequestParam(value = "itemIds", required = false) Integer[] itemIds,
                       @RequestParam(value = "itemCodes", required = false) String[] itemCodes,
                       @RequestParam(value = "orderedQuantities", required = false) BigDecimal[] orderedQuantities,
                       @RequestParam(value = "quantities", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "acceptedQuantities", required = false) BigDecimal[] acceptedQuantities,
                       @RequestParam(value = "unitPrices", required = false) BigDecimal[] unitPrices,
                       @RequestParam(value = "confirmationItemNos", required = false) String[] confirmationItemNos,
                       @RequestParam(value = "batchNumbers", required = false) String[] batchNumbers,
                       @RequestParam(value = "expiryDates", required = false) String[] expiryDates,
                       @RequestParam(value = "rejectionReasonCodes", required = false) String[] rejectionReasonCodes,
                       @RequestParam(value = "rejectionReasonDetails", required = false) String[] rejectionReasonDetails,
                       @RequestParam(value = "lineNotes", required = false) String[] lineNotes,
                       @RequestParam(value = "deliveryDate", required = false) String deliveryDate,
                       @RequestParam(value = "sendConfirmation", required = false) Boolean sendConfirmation,
                       HttpServletRequest request,
                       HttpSession session,
                       UiUtils ui) throws ParseException, IOException {
        try {
            MultipartFile receiptFile = extractReceiptFile(request);
            DeliveryReceipt receipt;
            if ("uploadReceipt".equals(action)) {
                receipt = buildReceiptFromUpload(pharmacyService, receiptNumber, purchaseOrderId, pharmacyLocationId, receiptFile);
                inventoryService.receiveDelivery(receipt);
                if (Boolean.TRUE.equals(sendConfirmation)) {
                    supplierIntegrationService.confirmDelivery(receipt);
                }
                setSuccessFlash(session, FLASH_UPLOAD_RECEIVED, receipt);
                return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive",
                        successParams("receive", source, purchaseOrderId));
            }
            if ("receiveManual".equals(action)) {
                receipt = buildManualReceipt(pharmacyService, receiptNumber, purchaseOrderId, pharmacyLocationId, itemIds, itemCodes, orderedQuantities, quantities,
                        acceptedQuantities, unitPrices, confirmationItemNos, batchNumbers, expiryDates,
                        rejectionReasonCodes, rejectionReasonDetails, lineNotes, deliveryDate);
                inventoryService.receiveDelivery(receipt);
                if (shouldSendConfirmation(source, sendConfirmation)) {
                    supplierIntegrationService.confirmDelivery(receipt);
                }
                setSuccessFlash(session, FLASH_RECEIVED, receipt);
                return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive",
                        successParams("receive", source, purchaseOrderId));
            }
            if ("uploadDirectReceipt".equals(action)) {
                receipt = buildReceiptFromUpload(pharmacyService, receiptNumber, null, pharmacyLocationId, receiptFile);
                inventoryService.receiveDelivery(receipt);
                setSuccessFlash(session, FLASH_UPLOAD_RECEIVED, receipt);
                return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive",
                        successParams("direct", null, null));
            }
            receipt = buildManualReceipt(pharmacyService, receiptNumber, null, pharmacyLocationId, itemIds, itemCodes, orderedQuantities, quantities,
                    acceptedQuantities, unitPrices, confirmationItemNos, batchNumbers, expiryDates,
                    rejectionReasonCodes, rejectionReasonDetails, lineNotes, deliveryDate);
            inventoryService.receiveDelivery(receipt);
            setSuccessFlash(session, FLASH_RECEIVED, receipt);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive",
                    successParams("direct", null, null));
        }
        catch (IllegalArgumentException e) {
            java.util.Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
            FlashMessageSupport.set(session, FLASH_ERROR_MESSAGE, e.getMessage());
            params.put("view", errorView(view, action, purchaseOrderId));
            if (purchaseOrderId != null) {
                params.put("purchaseOrderId", purchaseOrderId);
            }
            if (source != null && !source.trim().isEmpty()) {
                params.put("source", source.trim());
            }
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive", params);
        }
    }

    private MultipartFile extractReceiptFile(HttpServletRequest request) {
        if (request instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest) request).getFile("receiptFile");
        }
        return null;
    }

    private DeliveryReceipt buildManualReceipt(PharmacyService pharmacyService, String receiptNumber, Integer purchaseOrderId,
                                              Integer pharmacyLocationId, Integer[] itemIds, String[] itemCodes, BigDecimal[] orderedQuantities, BigDecimal[] quantities,
                                              BigDecimal[] acceptedQuantities, BigDecimal[] unitPrices, String[] confirmationItemNos,
                                              String[] batchNumbers, String[] expiryDates, String[] rejectionReasonCodes,
                                              String[] rejectionReasonDetails, String[] lineNotes,
                                              String deliveryDate) throws ParseException {
        if ((itemIds == null || itemIds.length == 0) && (itemCodes == null || itemCodes.length == 0)) {
            throw new IllegalArgumentException("Add at least one item line before receiving stock");
        }
        DeliveryReceipt receipt = baseReceipt(pharmacyService, receiptNumber, purchaseOrderId, pharmacyLocationId);
        if (deliveryDate != null && !deliveryDate.trim().isEmpty()) {
            receipt.setDateCreated(parseFlexibleDate(deliveryDate));
        }
        List<DeliveryReceiptLine> lines = new ArrayList<DeliveryReceiptLine>();
        int lineCount = maxLength(itemIds, itemCodes, orderedQuantities, quantities, acceptedQuantities, unitPrices,
                confirmationItemNos, batchNumbers, expiryDates, rejectionReasonCodes, rejectionReasonDetails, lineNotes);
        for (int i = 0; i < lineCount; i++) {
            Integer itemId = itemIds != null && itemIds.length > i ? itemIds[i] : null;
            String itemCode = itemCodes != null && itemCodes.length > i ? trim(itemCodes, i) : "";
            String fallbackItemName = findPurchaseOrderItemName(receipt.getPurchaseOrder(), itemCode);
            String fallbackProgram = findPurchaseOrderItemProgram(receipt.getPurchaseOrder(), itemCode);
            Item item = itemId == null ? null : pharmacyService.getItem(itemId);
            if (item == null && !itemCode.isEmpty()) {
                item = DeliveryReceiptSupport.resolveOrCreateItemByCode(pharmacyService, itemCode, fallbackItemName, fallbackProgram);
            }
            BigDecimal quantity = quantities != null && quantities.length > i ? quantities[i] : null;
            BigDecimal orderedQuantity = orderedQuantities != null && orderedQuantities.length > i ? orderedQuantities[i] : quantity;
            BigDecimal acceptedQuantity = acceptedQuantities != null && acceptedQuantities.length > i ? acceptedQuantities[i] : quantity;
            if (item == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            validateAcceptedQuantity(item, quantity, acceptedQuantity, i);
            DeliveryReceiptLine line = new DeliveryReceiptLine();
            line.setItem(item);
            line.setBatchNumber(trim(batchNumbers, i));
            line.setOrderedQuantity(orderedQuantity);
            line.setReceivedQuantity(quantity);
            line.setAcceptedQuantity(acceptedQuantity);
            line.setUnitPrice(unitPrices != null && unitPrices.length > i ? unitPrices[i] : null);
            line.setConfirmationItemNo(trim(confirmationItemNos, i));
            line.setRejectionReasonCode(trim(rejectionReasonCodes, i));
            line.setRejectionReasonDetail(trim(rejectionReasonDetails, i));
            line.setNotes(trim(lineNotes, i));
            String expiryDate = trim(expiryDates, i);
            if (!isNotApplicable(expiryDate) && !expiryDate.isEmpty()) {
                line.setExpiryDate(parseFlexibleDate(expiryDate));
            }
            enforceRejectionMetadata(line, i);
            lines.add(line);
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one valid item with quantity greater than zero");
        }
        receipt.setLines(lines);
        return receipt;
    }

    private DeliveryReceipt buildReceiptFromUpload(PharmacyService pharmacyService, String receiptNumber,
                                                   Integer purchaseOrderId, Integer pharmacyLocationId,
                                                   MultipartFile receiptFile) throws IOException, ParseException {
        if (receiptFile == null || receiptFile.isEmpty()) {
            throw new IllegalArgumentException("Upload a CSV or Excel file with stock lines");
        }
        DeliveryReceipt receipt = baseReceipt(pharmacyService, receiptNumber, purchaseOrderId, pharmacyLocationId);
        List<DeliveryReceiptLine> lines = new ArrayList<DeliveryReceiptLine>();
        if (isCsvFile(receiptFile)) {
            parseCsvLines(pharmacyService, receiptFile, lines);
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Uploaded CSV file does not contain any valid stock lines");
            }
            receipt.setLines(lines);
            return receipt;
        }
        DataFormatter formatter = new DataFormatter();
        Workbook workbook = openWorkbook(receiptFile);
        try {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Uploaded Excel file does not contain a sheet");
            }
            int headerRowIndex = isFormatInstructionRow(sheet.getRow(0), formatter) ? 1 : 0;
            Map<String, Integer> headerIndexes = headerIndexes(sheet.getRow(headerRowIndex), formatter);
            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || rowIsBlank(row, formatter)) {
                    continue;
                }
                String itemCode = cellValue(row, headerIndexes, "itemcode", formatter, 0);
                Item item = DeliveryReceiptSupport.resolveOrCreateItemByCode(pharmacyService, itemCode);
                if (item == null) {
                    throw new IllegalArgumentException("Unknown item code in upload: " + itemCode + " at row " + (rowIndex + 1));
                }
                BigDecimal quantity = firstDecimalValue(row, formatter, headerIndexes, new String[]{"receivedquantity", "quantity", "deliveryquantity"}, 4);
                if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Quantity must be greater than zero at row " + (rowIndex + 1));
                }
                DeliveryReceiptLine line = new DeliveryReceiptLine();
                line.setItem(item);
                line.setBatchNumber(cellValue(row, headerIndexes, "batchnumber", formatter, 6));
                BigDecimal requestedQuantity = firstDecimalValue(row, formatter, headerIndexes, new String[]{"requestedquantity", "orderedquantity"}, 2);
                line.setOrderedQuantity(requestedQuantity == null ? quantity : requestedQuantity);
                line.setReceivedQuantity(quantity);
                line.setAcceptedQuantity(quantity);
                line.setConfirmationItemNo(String.valueOf(rowIndex));
                String unitPrice = firstCellValue(row, formatter, headerIndexes, new String[]{"receivingprice", "unitprice"}, 5);
                if (!unitPrice.isEmpty()) {
                    line.setUnitPrice(new BigDecimal(unitPrice));
                }
                String expiryDate = cellValue(row, headerIndexes, "expirydate", formatter, 7);
                if (!isNotApplicable(expiryDate) && !expiryDate.isEmpty()) {
                    line.setExpiryDate(parseFlexibleDate(expiryDate));
                }
                lines.add(line);
            }
        }
        finally {
            workbook.close();
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Uploaded Excel file does not contain any valid stock lines");
        }
        receipt.setLines(lines);
        return receipt;
    }

    private void parseCsvLines(PharmacyService pharmacyService, MultipartFile receiptFile, List<DeliveryReceiptLine> lines) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(receiptFile.getInputStream(), StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) {
            return;
        }
        int rowIndex = 1;
        List<String> firstLineValues = parseCsvLine(headerLine);
        if (isFormatInstructionCell(firstLineValues.isEmpty() ? "" : firstLineValues.get(0))) {
            headerLine = reader.readLine();
            rowIndex++;
            if (headerLine == null) {
                return;
            }
        }
        List<String> headerValues = parseCsvLine(headerLine);
        Map<String, Integer> headerIndexes = new LinkedHashMap<String, Integer>();
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < headerValues.size(); i++) {
            String header = normalizeHeader(headerValues.get(i));
            if (header.isEmpty() || seen.contains(header)) {
                continue;
            }
            headerIndexes.put(header, i);
            seen.add(header);
        }
        String rowLine;
        while ((rowLine = reader.readLine()) != null) {
            rowIndex++;
            if (rowLine.trim().isEmpty()) {
                continue;
            }
            List<String> rowValues = parseCsvLine(rowLine);
            if (csvRowBlank(rowValues)) {
                continue;
            }
            String itemCode = csvValue(rowValues, headerIndexes, "itemcode", 0);
            Item item = DeliveryReceiptSupport.resolveOrCreateItemByCode(pharmacyService, itemCode);
            if (item == null) {
                throw new IllegalArgumentException("Unknown item code in upload: " + itemCode + " at row " + rowIndex);
            }
            BigDecimal quantity = firstDecimalValue(rowValues, headerIndexes, new String[]{"receivedquantity", "quantity", "deliveryquantity"}, 4);
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero at row " + rowIndex);
            }
            DeliveryReceiptLine line = new DeliveryReceiptLine();
            line.setItem(item);
            line.setBatchNumber(csvValue(rowValues, headerIndexes, "batchnumber", 6));
            BigDecimal requestedQuantity = firstDecimalValue(rowValues, headerIndexes, new String[]{"requestedquantity", "orderedquantity"}, 2);
            line.setOrderedQuantity(requestedQuantity == null ? quantity : requestedQuantity);
            line.setReceivedQuantity(quantity);
            line.setAcceptedQuantity(quantity);
            line.setConfirmationItemNo(String.valueOf(rowIndex));
            String unitPrice = firstCellValue(rowValues, headerIndexes, new String[]{"receivingprice", "unitprice"}, 5);
            if (!unitPrice.isEmpty()) {
                line.setUnitPrice(new BigDecimal(unitPrice));
            }
            String expiryDate = csvValue(rowValues, headerIndexes, "expirydate", 7);
            if (!isNotApplicable(expiryDate) && !expiryDate.isEmpty()) {
                line.setExpiryDate(parseFlexibleDate(expiryDate));
            }
            lines.add(line);
        }
    }

    private DeliveryReceipt baseReceipt(PharmacyService pharmacyService, String receiptNumber, Integer purchaseOrderId,
                                        Integer pharmacyLocationId) {
        receiptNumber = normalizeReceiptNumber(receiptNumber, purchaseOrderId == null ? "DIRECT" : "PO-" + purchaseOrderId);
        PurchaseOrder purchaseOrder = purchaseOrderId == null ? null : pharmacyService.getPurchaseOrder(purchaseOrderId);
        if (purchaseOrderId != null && (purchaseOrder == null || !"APPROVED".equalsIgnoreCase(purchaseOrder.getApprovalStatus()))) {
            throw new IllegalArgumentException("Select an approved purchase order before receiving supplier stock");
        }
        if (pharmacyLocationId == null) {
            throw new IllegalArgumentException("Receive into location is required");
        }
        DeliveryReceipt receipt = new DeliveryReceipt();
        receipt.setReceiptNumber(receiptNumber.trim());
        receipt.setPurchaseOrder(purchaseOrder);
        receipt.setReceivedIntoLocation(pharmacyService.getPharmacyLocation(pharmacyLocationId));
        if (receipt.getReceivedIntoLocation() == null) {
            throw new IllegalArgumentException("Selected receive-into location was not found");
        }
        receipt.setStatus("RECEIVED");
        receipt.setCreator(Context.getAuthenticatedUser());
        receipt.setDateCreated(new Date());
        return receipt;
    }

    private List<PurchaseOrder> getApprovedPurchaseOrders(PharmacyService pharmacyService) {
        List<PurchaseOrder> approvedPurchaseOrders = new ArrayList<PurchaseOrder>();
        for (PurchaseOrder purchaseOrder : pharmacyService.getPurchaseOrders("APPROVED", null, null, 0, 500)) {
            if ("APPROVED".equalsIgnoreCase(purchaseOrder.getApprovalStatus()) && isOpenForReceiving(purchaseOrder)) {
                approvedPurchaseOrders.add(purchaseOrder);
            }
        }
        return approvedPurchaseOrders;
    }

    private boolean isOpenForReceiving(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            return false;
        }
        String dispatchStatus = purchaseOrder.getDispatchStatus() == null ? "" : purchaseOrder.getDispatchStatus().trim();
        return !"RECEIVED".equalsIgnoreCase(dispatchStatus)
                && !"SERVED".equalsIgnoreCase(dispatchStatus)
                && !"CLOSED".equalsIgnoreCase(dispatchStatus);
    }

    private boolean rowIsBlank(Row row, DataFormatter formatter) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (!cellValue(row, i, formatter).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean isFormatInstructionRow(Row row, DataFormatter formatter) {
        return row != null && isFormatInstructionCell(cellValue(row, 0, formatter));
    }

    private boolean isFormatInstructionCell(String value) {
        return value != null && value.trim().toLowerCase(Locale.ENGLISH).startsWith("allowed format:");
    }

    private String cellValue(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = row.getCell(cellIndex);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private BigDecimal decimalValue(Row row, int cellIndex, DataFormatter formatter) {
        String value = cellValue(row, cellIndex, formatter);
        return value.isEmpty() ? null : new BigDecimal(value);
    }

    private String cellValue(Row row, Map<String, Integer> headerIndexes, String headerName, DataFormatter formatter, int defaultIndex) {
        Integer index = headerIndexes.get(headerName);
        return cellValue(row, index == null ? defaultIndex : index, formatter);
    }

    private String firstCellValue(Row row, DataFormatter formatter, Map<String, Integer> headerIndexes, String[] headerNames, int defaultIndex) {
        for (String headerName : headerNames) {
            Integer index = headerIndexes.get(headerName);
            if (index != null) {
                String value = cellValue(row, index, formatter);
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return cellValue(row, defaultIndex, formatter);
    }

    private BigDecimal firstDecimalValue(Row row, DataFormatter formatter, Map<String, Integer> headerIndexes, String[] headerNames, int defaultIndex) {
        String value = firstCellValue(row, formatter, headerIndexes, headerNames, defaultIndex);
        return value.isEmpty() ? null : new BigDecimal(value);
    }

    private String firstCellValue(List<String> rowValues, Map<String, Integer> headerIndexes, String[] headerNames, int defaultIndex) {
        for (String headerName : headerNames) {
            Integer index = headerIndexes.get(headerName);
            if (index != null) {
                String value = csvValue(rowValues, index);
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return csvValue(rowValues, defaultIndex);
    }

    private BigDecimal firstDecimalValue(List<String> rowValues, Map<String, Integer> headerIndexes, String[] headerNames, int defaultIndex) {
        String value = firstCellValue(rowValues, headerIndexes, headerNames, defaultIndex);
        return value.isEmpty() ? null : new BigDecimal(value);
    }

    private Map<String, Integer> headerIndexes(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> headerIndexes = new LinkedHashMap<String, Integer>();
        if (headerRow == null) {
            return headerIndexes;
        }
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String header = cellValue(headerRow, i, formatter).trim().toLowerCase(Locale.ENGLISH);
            if (header.isEmpty() || seen.contains(header)) {
                continue;
            }
            headerIndexes.put(header, i);
            seen.add(header);
        }
        return headerIndexes;
    }

    private String trim(String[] values, int index) {
        if (values == null || values.length <= index || values[index] == null) {
            return "";
        }
        return values[index].trim();
    }

    private Workbook openWorkbook(MultipartFile receiptFile) throws IOException {
        String fileName = receiptFile.getOriginalFilename() == null ? "" : receiptFile.getOriginalFilename().toLowerCase(Locale.ENGLISH);
        if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(receiptFile.getInputStream());
        }
        throw new IllegalArgumentException("Upload a file in .csv or .xls format");
    }

    private boolean isCsvFile(MultipartFile receiptFile) {
        String fileName = receiptFile.getOriginalFilename() == null ? "" : receiptFile.getOriginalFilename().toLowerCase(Locale.ENGLISH);
        return fileName.endsWith(".csv");
    }

    private String normalizeHeader(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.startsWith("\ufeff")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private boolean csvRowBlank(List<String> rowValues) {
        for (String value : rowValues) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String csvValue(List<String> rowValues, Map<String, Integer> headerIndexes, String headerName, int defaultIndex) {
        Integer index = headerIndexes.get(headerName);
        return csvValue(rowValues, index == null ? defaultIndex : index);
    }

    private String csvValue(List<String> rowValues, int index) {
        if (index < 0 || index >= rowValues.size()) {
            return "";
        }
        return rowValues.get(index) == null ? "" : rowValues.get(index).trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<String>();
        if (line == null) {
            return values;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private PharmacyLocation getDefaultReceiveLocation(List<PharmacyLocation> pharmacyLocations) {
        if (pharmacyLocations == null || pharmacyLocations.isEmpty()) {
            return null;
        }
        for (PharmacyLocation pharmacyLocation : pharmacyLocations) {
            if (Boolean.TRUE.equals(pharmacyLocation.getMainStore())) {
                return pharmacyLocation;
            }
        }
        return pharmacyLocations.get(0);
    }

    private boolean isRmsOrder(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null || purchaseOrder.getSupplier() == null) {
            return false;
        }
        return Boolean.TRUE.equals(purchaseOrder.getSupplier().getApiEnabled());
    }

    private boolean containsIgnoreCase(String value, String token) {
        return value != null && token != null && value.toLowerCase(Locale.ENGLISH).contains(token.toLowerCase(Locale.ENGLISH));
    }

    private String normalizeView(String view, PurchaseOrder selectedPurchaseOrder) {
        if (selectedPurchaseOrder != null) {
            return "receive";
        }
        if ("rms".equalsIgnoreCase(view) || "supplier".equalsIgnoreCase(view) || "direct".equalsIgnoreCase(view)) {
            return view.toLowerCase(Locale.ENGLISH);
        }
        return "landing";
    }

    private String normalizeSource(String source, PurchaseOrder selectedPurchaseOrder) {
        if ("rms".equalsIgnoreCase(source) || "supplier".equalsIgnoreCase(source)) {
            return source.toLowerCase(Locale.ENGLISH);
        }
        if (selectedPurchaseOrder != null) {
            return isRmsOrder(selectedPurchaseOrder) ? "rms" : "supplier";
        }
        return "rms";
    }

    private String normalizeReceiptNumber(String receiptNumber, String prefix) {
        if (receiptNumber != null && !receiptNumber.trim().isEmpty()) {
            return receiptNumber.trim();
        }
        return prefix + "-RCV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ENGLISH);
    }

    private Date parseFlexibleDate(String value) throws ParseException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || isNotApplicable(text)) {
            return null;
        }
        if (text.contains("T")) {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(text);
        }
        return new SimpleDateFormat("yyyy-MM-dd").parse(text);
    }

    private boolean isNotApplicable(String value) {
        if (value == null) {
            return false;
        }
        String text = value.trim();
        return "N/A".equalsIgnoreCase(text) || "NA".equalsIgnoreCase(text) || "NOT APPLICABLE".equalsIgnoreCase(text);
    }

    private List<Map<String, Object>> buildReceiveRows(PharmacyService pharmacyService,
                                                       PurchaseOrder purchaseOrder,
                                                       DeliveryReceiptImportRequest fetchedReceipt,
                                                       Map<String, Integer> itemIdByCode) {
        if (purchaseOrder == null) {
            return Collections.emptyList();
        }
        Map<String, PurchaseOrderItem> purchaseOrderItemsByCode = new LinkedHashMap<String, PurchaseOrderItem>();
        if (purchaseOrder.getItems() != null) {
            for (PurchaseOrderItem purchaseOrderItem : purchaseOrder.getItems()) {
                if (purchaseOrderItem != null && purchaseOrderItem.getItemCode() != null) {
                    purchaseOrderItemsByCode.put(purchaseOrderItem.getItemCode(), purchaseOrderItem);
                }
            }
        }

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if (fetchedReceipt != null && fetchedReceipt.getItems() != null && !fetchedReceipt.getItems().isEmpty()) {
            for (DeliveryReceiptImportItem receiptItem : fetchedReceipt.getItems()) {
                if (receiptItem == null || receiptItem.getNpcCode() == null) {
                    continue;
                }
                PurchaseOrderItem matchingOrderItem = purchaseOrderItemsByCode.get(receiptItem.getNpcCode());
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("itemId", itemIdByCode.get(receiptItem.getNpcCode()));
                row.put("itemCode", receiptItem.getNpcCode());
                row.put("itemName", receiptItem.getDescription() != null ? receiptItem.getDescription()
                        : (matchingOrderItem == null ? "" : matchingOrderItem.getItemName()));
                row.put("unit", receiptItem.getUnits());
                row.put("purchaseUnitPrice", matchingOrderItem == null ? null : matchingOrderItem.getUnitPrice());
                row.put("orderedQuantity", matchingOrderItem == null ? receiptItem.getDeliveryQuantity() : matchingOrderItem.getQuantity());
                row.put("receivingUnitPrice", matchingOrderItem == null ? null : matchingOrderItem.getUnitPrice());
                row.put("batchNumber", receiptItem.getBatch());
                row.put("expiryDate", receiptItem.getExpiryDate());
                row.put("receivedQuantity", receiptItem.getDeliveryQuantity());
                row.put("confirmationItemNo", receiptItem.getItemNo());
                rows.add(row);
            }
        }

        if (!rows.isEmpty()) {
            return rows;
        }

        if (purchaseOrder.getItems() == null) {
            return rows;
        }
        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("itemId", itemIdByCode.get(item.getItemCode()));
            row.put("itemCode", item.getItemCode());
            row.put("itemName", item.getItemName());
            row.put("unit", null);
            row.put("purchaseUnitPrice", item.getUnitPrice());
            row.put("orderedQuantity", item.getQuantity());
            row.put("receivingUnitPrice", item.getUnitPrice());
            row.put("batchNumber", null);
            row.put("expiryDate", null);
            row.put("receivedQuantity", item.getQuantity());
            row.put("confirmationItemNo", null);
            rows.add(row);
        }
        return rows;
    }

    private boolean shouldSendConfirmation(String source, Boolean sendConfirmation) {
        return "rms".equalsIgnoreCase(source) || Boolean.TRUE.equals(sendConfirmation);
    }

    private String findPurchaseOrderItemName(PurchaseOrder purchaseOrder, String itemCode) {
        if (purchaseOrder == null || purchaseOrder.getItems() == null || itemCode == null || itemCode.trim().isEmpty()) {
            return null;
        }
        for (PurchaseOrderItem purchaseOrderItem : purchaseOrder.getItems()) {
            if (purchaseOrderItem != null && purchaseOrderItem.getItemCode() != null
                    && itemCode.trim().equalsIgnoreCase(purchaseOrderItem.getItemCode().trim())) {
                return purchaseOrderItem.getItemName();
            }
        }
        return null;
    }

    private String findPurchaseOrderItemProgram(PurchaseOrder purchaseOrder, String itemCode) {
        if (purchaseOrder == null || purchaseOrder.getItems() == null || itemCode == null || itemCode.trim().isEmpty()) {
            return null;
        }
        for (PurchaseOrderItem purchaseOrderItem : purchaseOrder.getItems()) {
            if (purchaseOrderItem != null && purchaseOrderItem.getItemCode() != null
                    && itemCode.trim().equalsIgnoreCase(purchaseOrderItem.getItemCode().trim())) {
                return purchaseOrderItem.getProgram();
            }
        }
        return null;
    }

    private void validateAcceptedQuantity(Item item, BigDecimal receivedQuantity, BigDecimal acceptedQuantity, int lineIndex) {
        if (acceptedQuantity == null) {
            throw new IllegalArgumentException("Accepted quantity is required for line " + (lineIndex + 1));
        }
        if (acceptedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Accepted quantity cannot be negative for " + itemLabel(item, lineIndex));
        }
        if (acceptedQuantity.compareTo(receivedQuantity) > 0) {
            throw new IllegalArgumentException("Accepted quantity cannot exceed received quantity for " + itemLabel(item, lineIndex));
        }
    }

    private void enforceRejectionMetadata(DeliveryReceiptLine line, int lineIndex) {
        if (line == null) {
            return;
        }
        BigDecimal rejectedQuantity = safe(line.getReceivedQuantity()).subtract(safe(line.getAcceptedQuantity()));
        if (rejectedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            line.setRejectionReasonCode(null);
            line.setRejectionReasonDetail(null);
            return;
        }
        if (line.getRejectionReasonCode() == null || line.getRejectionReasonCode().trim().isEmpty()) {
            line.setRejectionReasonCode("OTHER");
        }
        if (line.getRejectionReasonDetail() == null || line.getRejectionReasonDetail().trim().isEmpty()) {
            throw new IllegalArgumentException("Provide a rejection reason for line " + (lineIndex + 1));
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String itemLabel(Item item, int lineIndex) {
        if (item != null && item.getName() != null && !item.getName().trim().isEmpty()) {
            return item.getName().trim();
        }
        return "line " + (lineIndex + 1);
    }

    private boolean isTrueFlag(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private DeliveryReceiptImportRequest parseFetchedReceipt(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null || purchaseOrder.getFetchedReceiptPayload() == null
                || purchaseOrder.getFetchedReceiptPayload().trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(purchaseOrder.getFetchedReceiptPayload(), DeliveryReceiptImportRequest.class);
        }
        catch (Exception e) {
            return null;
        }
    }

    private void setSuccessFlash(HttpSession session, String flagKey, DeliveryReceipt receipt) {
        FlashMessageSupport.set(session, flagKey, Boolean.TRUE);
        FlashMessageSupport.set(session, FLASH_DELIVERY_RECEIPT_ID,
                receipt == null ? null : receipt.getDeliveryReceiptId());
    }

    private Map<String, Object> successParams(String view, String source, Integer purchaseOrderId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("view", view);
        if (purchaseOrderId != null) {
            params.put("purchaseOrderId", purchaseOrderId);
        }
        if (source != null && !source.trim().isEmpty()) {
            params.put("source", source);
        }
        return params;
    }

    private String errorView(String requestedView, String action, Integer purchaseOrderId) {
        if (purchaseOrderId != null || "receiveManual".equals(action) || "uploadReceipt".equals(action)) {
            return "receive";
        }
        if ("uploadDirectReceipt".equals(action) || "receiveDirectManual".equals(action)) {
            return "direct";
        }
        return requestedView == null || requestedView.trim().isEmpty() ? "landing" : requestedView;
    }

    private int maxLength(Object[]... arrays) {
        int max = 0;
        if (arrays == null) {
            return max;
        }
        for (Object[] array : arrays) {
            if (array != null && array.length > max) {
                max = array.length;
            }
        }
        return max;
    }
}
