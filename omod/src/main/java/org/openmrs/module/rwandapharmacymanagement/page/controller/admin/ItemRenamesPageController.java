package org.openmrs.module.rwandapharmacymanagement.page.controller.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.ItemRenameService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.ItemRenameProposal;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Review page for pending item renames from supplier sync and the consistency check.
 */
@Controller
public class ItemRenamesPageController {

    private static final Log log = LogFactory.getLog(ItemRenamesPageController.class);

    public void get(PageModel model,
                    @SpringBean("itemRenameService") ItemRenameService itemRenameService,
                    HttpSession session) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_MANAGE_SETTINGS);
        Map<Integer, Map<String, Object>> rowsByItemId = new LinkedHashMap<Integer, Map<String, Object>>();
        for (ItemRenameProposal proposal : itemRenameService.getPendingProposals()) {
            Item item = proposal.getItem();
            if (item == null || item.getItemId() == null) {
                continue;
            }
            Map<String, Object> row = rowsByItemId.get(item.getItemId());
            if (row == null) {
                row = new LinkedHashMap<String, Object>();
                row.put("itemId", item.getItemId());
                row.put("itemName", item.getName());
                row.put("itemCode", item.getCode());
                row.put("drugName", item.getDrug() == null ? null : item.getDrug().getName());
                row.put("candidates", new ArrayList<Map<String, Object>>());
                rowsByItemId.put(item.getItemId(), row);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) row.get("candidates");
            Map<String, Object> candidate = new LinkedHashMap<String, Object>();
            candidate.put("name", proposal.getProposedName());
            candidate.put("program", proposal.getProgram());
            candidate.put("source", ItemRenameProposal.SOURCE_SUPPLIER_SYNC.equals(proposal.getSource())
                    ? "Supplier" : "Consistency check");
            candidate.put("reason", proposal.getReason());
            candidates.add(candidate);
        }
        model.addAttribute("rows", new ArrayList<Map<String, Object>>(rowsByItemId.values()));
        model.addAttribute("message", session.getAttribute("rpmItemRenameMessage"));
        model.addAttribute("error", session.getAttribute("rpmItemRenameError"));
        session.removeAttribute("rpmItemRenameMessage");
        session.removeAttribute("rpmItemRenameError");
    }

    public String post(@SpringBean("itemRenameService") ItemRenameService itemRenameService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "itemId", required = false) Integer itemId,
                       @RequestParam(value = "chosenName", required = false) String chosenName,
                       @RequestParam(value = "customName", required = false) String customName,
                       HttpSession session,
                       UiUtils ui) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_MANAGE_SETTINGS);
        try {
            if ("approve".equals(action)) {
                String name = customName != null && !customName.trim().isEmpty() ? customName : chosenName;
                session.setAttribute("rpmItemRenameMessage", itemRenameService.approveRename(itemId, name));
            } else if ("reject".equals(action)) {
                itemRenameService.rejectRenames(itemId);
                session.setAttribute("rpmItemRenameMessage", "Proposed renames for item " + itemId + " were rejected.");
            } else if ("check".equals(action)) {
                int flagged = itemRenameService.runConsistencyCheck();
                session.setAttribute("rpmItemRenameMessage", "Consistency check flagged " + flagged + " item"
                        + (flagged == 1 ? "" : "s") + ".");
            }
        }
        catch (Exception e) {
            log.error("Item rename action failed. action=" + action + ", itemId=" + itemId, e);
            session.setAttribute("rpmItemRenameError", e.getMessage() == null
                    ? "The item rename action failed." : e.getMessage());
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/itemRenames");
    }
}
