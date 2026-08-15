package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class NotBillableItemsReportPageController {

    public void get(PageModel model,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "requestType", required = false) String requestTypeParam) {
        Date endDate = NotBillableItemsReportSupport.startOfDay(NotBillableItemsReportSupport.parseDate(endDateParam, new Date()));
        Date startDate = NotBillableItemsReportSupport.startOfDay(NotBillableItemsReportSupport.parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        RequestType requestType = NotBillableItemsReportSupport.parseRequestType(requestTypeParam);
        List<PatientDispensingRequestLine> lines = dispensingService.getNotBillableRequestLines(startDate,
                NotBillableItemsReportSupport.nextDay(endDate), pharmacyLocationId, requestType);
        List<DispenseRecord> dispenseRecords = requestIds(lines).isEmpty()
                ? java.util.Collections.<DispenseRecord>emptyList()
                : dispensingService.getDispenseRecordsForRequests(requestIds(lines));
        List<NotBillableItemsReportSupport.Row> rows = NotBillableItemsReportSupport.buildRows(lines, dispenseRecords);
        model.addAttribute("rows", rows);
        model.addAttribute("totals", NotBillableItemsReportSupport.totals(rows));
        model.addAttribute("pharmacies", StockReportSupport.getActivePharmacies(pharmacyService));
        model.addAttribute("selectedPharmacyLocationId", pharmacyLocationId);
        model.addAttribute("selectedRequestType", requestType == null ? "ALL" : requestType.name());
        model.addAttribute("requestTypes", RequestType.values());
        model.addAttribute("startDate", NotBillableItemsReportSupport.formatDate(startDate));
        model.addAttribute("endDate", NotBillableItemsReportSupport.formatDate(endDate));
    }

    private List<Integer> requestIds(List<PatientDispensingRequestLine> lines) {
        List<Integer> ids = new ArrayList<Integer>();
        if (lines == null) {
            return ids;
        }
        for (PatientDispensingRequestLine line : lines) {
            if (line == null || line.getPatientDispensingRequest() == null
                    || line.getPatientDispensingRequest().getPatientDispensingRequestId() == null) {
                continue;
            }
            Integer requestId = line.getPatientDispensingRequest().getPatientDispensingRequestId();
            if (!ids.contains(requestId)) {
                ids.add(requestId);
            }
        }
        return ids;
    }
}
