package org.openmrs.module.rwandapharmacymanagement.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
@RequestMapping("/module/rwandapharmacymanagement/sms")
public class SmsDlrController {

    private final Log log = LogFactory.getLog(getClass());

    @RequestMapping(value = "/dlr", method = RequestMethod.POST, produces = "text/plain")
    @ResponseBody
    public String receiveDeliveryReport(HttpServletRequest request) {
        log.info("Intouch SMS DLR received: " + summarizeParameters(request.getParameterMap()));
        return "ACK/Jasmin";
    }

    private String summarizeParameters(Map<String, String[]> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append("=");
            String[] values = entry.getValue();
            builder.append(values == null || values.length == 0 ? "" : values[0]);
        }
        return builder.toString();
    }
}
