package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.context.InvalidRequestContext;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.registry.VendorComponent;
import org.springframework.http.HttpStatus;

public interface VendorResponsePostProcessor extends VendorComponent {

    /**
     * Post Processing Vendor Error Response for Normal Flow (i.e: From Controller onwards)
     */
    VendorErrorResponse postProcessErrorResponse(VendorErrorResponse errorResponse, VendorExceptionContext errorContext);

    /**
     * Post Processing for MethodArgumentNotValidException only where RequestDto fails Bean Validation
     */
    default VendorErrorResponse postProcessInvalidRequest(InvalidRequestContext ctx){
        return new VendorErrorResponse(HttpStatus.BAD_REQUEST, ctx.getResponseBody());
    }

}
