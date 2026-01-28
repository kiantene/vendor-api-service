package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.registry.VendorComponent;

public interface VendorResponsePostProcessor extends VendorComponent {

    VendorErrorResponse postProcessErrorResponse(VendorErrorResponse errorResponse, VendorExceptionContext errorContext);

}
