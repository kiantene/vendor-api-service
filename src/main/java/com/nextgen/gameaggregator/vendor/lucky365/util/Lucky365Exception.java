package com.nextgen.gameaggregator.vendor.lucky365.util;

import com.nextgen.gameaggregator.vendor.lucky365.constant.EndPoints;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class Lucky365Exception {

    private Lucky365Exception() {}

    public static boolean isList() {
        ServletRequestAttributes attr =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr == null) {
            return false;
        }
        return attr.getRequest().getRequestURI().contains(EndPoints.BALANCE);
    }

    public static boolean isListResponse() {
        return !isList();
    }
}