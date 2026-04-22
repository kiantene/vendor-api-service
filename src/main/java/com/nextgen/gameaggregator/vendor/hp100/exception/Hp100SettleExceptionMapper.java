package com.nextgen.gameaggregator.vendor.hp100.exception;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.hp100.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.hp100.constant.ResponseCode;
import org.springframework.stereotype.Component;

@Component(Endpoints.CLASS_NAME + "settle")
public class Hp100SettleExceptionMapper extends Hp100ExceptionMapper {
    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return getErrorResponse(ResponseCode.DUPLICATE_SETTLE);
    }

    @Override
    public String getVendorClassName() {
        return Endpoints.CLASS_NAME + "settle";
    }

}
