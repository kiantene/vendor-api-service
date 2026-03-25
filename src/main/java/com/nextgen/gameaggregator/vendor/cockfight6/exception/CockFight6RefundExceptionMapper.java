package com.nextgen.gameaggregator.vendor.cockfight6.exception;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.ResponseCode;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME + "rollback")
public class CockFight6RefundExceptionMapper extends CockFight6ExceptionMapper {
    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return getErrorResponse(ResponseCode.DUPLICATE_REFUND);
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME + "rollback";
    }
}
