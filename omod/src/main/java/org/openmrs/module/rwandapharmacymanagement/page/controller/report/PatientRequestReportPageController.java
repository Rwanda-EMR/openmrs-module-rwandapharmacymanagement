package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

@Controller
public class PatientRequestReportPageController {

    public void get(PageModel model,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "requestType", required = false) String requestTypeParam) {
        Date endDate = PatientRequestReportSupport.startOfDay(PatientRequestReportSupport.parseDate(endDateParam, new Date()));
        Date startDate = PatientRequestReportSupport.startOfDay(PatientRequestReportSupport.parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        RequestType requestType = PatientRequestReportSupport.parseRequestType(requestTypeParam);
        List<PatientDispensingRequest> requests = dispensingService.getPatientDispensingRequests(startDate,
                PatientRequestReportSupport.nextDay(endDate), pharmacyLocationId, requestType);
        List<PatientRequestReportSupport.Row> rows = PatientRequestReportSupport.buildRows(requests);
        model.addAttribute("rows", rows);
        model.addAttribute("totals", PatientRequestReportSupport.totals(rows));
        model.addAttribute("pharmacies", StockReportSupport.getActivePharmacies(pharmacyService));
        model.addAttribute("selectedPharmacyLocationId", pharmacyLocationId);
        model.addAttribute("requestTypes", RequestType.values());
        model.addAttribute("selectedRequestType", requestType == null ? "ALL" : requestType.name());
        model.addAttribute("startDate", PatientRequestReportSupport.formatDate(startDate));
        model.addAttribute("endDate", PatientRequestReportSupport.formatDate(endDate));
    }
}
