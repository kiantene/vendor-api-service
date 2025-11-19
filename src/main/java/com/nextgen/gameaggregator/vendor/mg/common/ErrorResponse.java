package com.nextgen.gameaggregator.vendor.mg.common;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class ErrorResponse implements HttpResponse {
    @Override
    public boolean hasError() {
        return false;
    }
}
