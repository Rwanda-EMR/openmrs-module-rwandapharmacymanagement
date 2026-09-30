package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportExcelRenderer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ReportExportSummarySupport {

    private ReportExportSummarySupport() {
    }

    static List<List<String>> rows(String... labelValues) {
        List<List<String>> rows = new ArrayList<List<String>>();
        if (labelValues == null) {
            return rows;
        }
        for (int i = 0; i + 1 < labelValues.length; i += 2) {
            rows.add(Arrays.asList(labelValues[i], labelValues[i + 1]));
        }
        return rows;
    }

    static String integer(int value) {
        return String.valueOf(value);
    }

    static String decimal(BigDecimal value) {
        return value == null ? "0" : StockReportExcelRenderer.decimal(value);
    }
}
