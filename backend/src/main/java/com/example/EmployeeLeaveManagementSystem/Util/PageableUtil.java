package com.example.EmployeeLeaveManagementSystem.Util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

public final class PageableUtil {

    private static final int MAX_PAGE_SIZE = 100;

    private PageableUtil() {
    }

    /** Keeps only whitelisted sort properties (never lets clients sort by e.g. password) and caps page size. */
    public static Pageable restrict(Pageable pageable, Set<String> allowedProperties, Sort defaultSort) {
        List<Sort.Order> orders = pageable.getSort().stream()
                .filter(order -> allowedProperties.contains(order.getProperty()))
                .toList();
        Sort sort = orders.isEmpty() ? defaultSort : Sort.by(orders);
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), sort);
    }
}
