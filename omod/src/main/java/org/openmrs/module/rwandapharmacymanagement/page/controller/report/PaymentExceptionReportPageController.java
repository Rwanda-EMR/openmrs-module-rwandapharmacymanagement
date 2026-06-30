package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class PaymentExceptionReportPageController {

    public void get(PageModel model,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "requestType", required = false) String requestTypeParam) {
        Date endDate = PaymentExceptionReportSupport.startOfDay(PaymentExceptionReportSupport.parseDate(endDateParam, new Date()));
        Date startDate = PaymentExceptionReportSupport.startOfDay(PaymentExceptionReportSupport.parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        RequestType requestType = PaymentExceptionReportSupport.parseRequestType(requestTypeParam);
        List<PatientDispensingRequest> requests = dispensingService.getPaymentExceptionApprovedRequests(startDate,
                PaymentExceptionReportSupport.nextDay(endDate), pharmacyLocationId, requestType);
        List<DispenseRecord> dispenseRecords = requestIds(requests).isEmpty()
                ? java.util.Collections.<DispenseRecord>emptyList()
                : dispensingService.getDispenseRecordsForRequests(requestIds(requests));
        List<PaymentExceptionReportSupport.Row> rows = PaymentExceptionReportSupport.buildRows(requests, dispenseRecords);
        model.addAttribute("rows", rows);
        model.addAttribute("totals", PaymentExceptionReportSupport.totals(rows));
        model.addAttribute("pharmacies", StockReportSupport.getActivePharmacies(pharmacyService));
        model.addAttribute("selectedPharmacyLocationId", pharmacyLocationId);
        model.addAttribute("selectedRequestType", requestType == null ? "ALL" : requestType.name());
        model.addAttribute("requestTypes", RequestType.values());
        model.addAttribute("startDate", PaymentExceptionReportSupport.formatDate(startDate));
        model.addAttribute("endDate", PaymentExceptionReportSupport.formatDate(endDate));
    }

    private List<Integer> requestIds(List<PatientDispensingRequest> requests) {
        List<Integer> ids = new ArrayList<Integer>();
        if (requests == null) {
            return ids;
        }
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                ids.add(request.getPatientDispensingRequestId());
            }
        }
        return ids;
    }
}
