package com.nextgen.gameaggregator.vendor.pgsoft.exception;

import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;

public class PGSoftExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
//        return Endpoints.CLASS_NAME;
        return null;
    }

    @Override
    public Object handle(Throwable ex) {
        return null;
    }
}
