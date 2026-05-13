package org.openmrs.module.rwandapharmacymanagement.page.controller.support;

import org.openmrs.ui.framework.page.PageModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PaginationSupport {

    public static final int DEFAULT_PAGE_SIZE = 25;
    private static final int[] ALLOWED_PAGE_SIZES = new int[]{10, 25, 50, 100};

    private PaginationSupport() {
    }

    public static int sanitizePage(Integer page) {
        return page == null || page.intValue() < 1 ? 1 : page.intValue();
    }

    public static int sanitizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        for (int allowed : ALLOWED_PAGE_SIZES) {
            if (allowed == pageSize.intValue()) {
                return allowed;
            }
        }
        return DEFAULT_PAGE_SIZE;
    }

    public static <T> List<T> slice(List<T> source, int page, int pageSize) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        int startIndex = Math.max(0, (page - 1) * pageSize);
        if (startIndex >= source.size()) {
            return Collections.emptyList();
        }
        int endIndex = Math.min(source.size(), startIndex + pageSize);
        return new ArrayList<T>(source.subList(startIndex, endIndex));
    }

    public static int clampPage(int page, int pageSize, int totalItems) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / (double) pageSize));
        return Math.min(Math.max(page, 1), totalPages);
    }

    public static void addAttributes(PageModel model, String prefix, int page, int pageSize, int totalItems) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / (double) pageSize));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int startItem = totalItems == 0 ? 0 : ((currentPage - 1) * pageSize) + 1;
        int endItem = totalItems == 0 ? 0 : Math.min(totalItems, currentPage * pageSize);

        model.addAttribute(prefix + "Page", currentPage);
        model.addAttribute(prefix + "PageSize", pageSize);
        model.addAttribute(prefix + "TotalItems", totalItems);
        model.addAttribute(prefix + "TotalPages", totalPages);
        model.addAttribute(prefix + "HasPrevious", currentPage > 1);
        model.addAttribute(prefix + "HasNext", currentPage < totalPages);
        model.addAttribute(prefix + "StartItem", startItem);
        model.addAttribute(prefix + "EndItem", endItem);

        List<Integer> pageSizes = new ArrayList<Integer>();
        for (int allowed : ALLOWED_PAGE_SIZES) {
            pageSizes.add(allowed);
        }
        model.addAttribute(prefix + "PageSizes", pageSizes);
    }
}
