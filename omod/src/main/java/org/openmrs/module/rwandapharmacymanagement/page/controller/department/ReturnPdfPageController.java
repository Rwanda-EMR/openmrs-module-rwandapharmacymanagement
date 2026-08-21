package org.openmrs.module.rwandapharmacymanagement.page.controller.department;

import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentStockReturnService;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturn;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.reporting.DepartmentStockReturnPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Controller("departmentReturnPdfPageController")
public class ReturnPdfPageController {

    public void get(@SpringBean("departmentStockReturnService") DepartmentStockReturnService departmentStockReturnService,
                    @RequestParam("returnId") Integer returnId,
                    HttpServletResponse response) throws IOException {
        DepartmentStockReturn stockReturn = departmentStockReturnService.getReturn(returnId);
        if (stockReturn == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Department return not found");
            return;
        }
        if (!canDownload(stockReturn)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
        }
        byte[] pdf = DepartmentStockReturnPdfRenderer.render(stockReturn,
                Context.getAdministrationService().getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL_NAME, ""));
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"department-return-" + returnId + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }

    private boolean canDownload(DepartmentStockReturn stockReturn) {
        if (Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK)) {
            return true;
        }
        if (Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_DEPARTMENT_STORE)
                && currentLocationMatchesDepartment(stockReturn.getDepartmentLocation())) {
            return true;
        }
        User user = Context.getAuthenticatedUser();
        Integer userId = user == null ? null : user.getUserId();
        return userId != null && (sameUser(userId, stockReturn.getCreator()) || sameUser(userId, stockReturn.getReceivedBy())
                || sameUser(userId, stockReturn.getRejectedBy()));
    }

    private boolean sameUser(Integer userId, User user) {
        return user != null && user.getUserId() != null && user.getUserId().equals(userId);
    }

    private boolean currentLocationMatchesDepartment(PharmacyLocation departmentLocation) {
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        return matchesExactOpenmrsLocation(departmentLocation, currentLocation)
                || matchesOpenmrsLocationTag(departmentLocation, currentLocation);
    }

    private boolean matchesExactOpenmrsLocation(PharmacyLocation pharmacyLocation, Location location) {
        if (pharmacyLocation == null || location == null || location.getLocationId() == null) {
            return false;
        }
        return pharmacyLocation.getLocation() != null && pharmacyLocation.getLocation().getLocationId() != null
                && pharmacyLocation.getLocation().getLocationId().equals(location.getLocationId());
    }

    private boolean matchesOpenmrsLocationTag(PharmacyLocation pharmacyLocation, Location location) {
        if (pharmacyLocation == null || location == null || location.getLocationId() == null
                || location.getTags() == null || location.getTags().isEmpty()) {
            return false;
        }
        Set<Integer> currentTagIds = new HashSet<Integer>();
        for (LocationTag tag : location.getTags()) {
            if (tag != null && tag.getLocationTagId() != null) {
                currentTagIds.add(tag.getLocationTagId());
            }
        }
        if (pharmacyLocation.getLocationTag() != null && pharmacyLocation.getLocationTag().getLocationTagId() != null
                && currentTagIds.contains(pharmacyLocation.getLocationTag().getLocationTagId())) {
            return true;
        }
        if (pharmacyLocation.getLocationTags() != null) {
            for (LocationTag tag : pharmacyLocation.getLocationTags()) {
                if (tag != null && tag.getLocationTagId() != null && currentTagIds.contains(tag.getLocationTagId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
