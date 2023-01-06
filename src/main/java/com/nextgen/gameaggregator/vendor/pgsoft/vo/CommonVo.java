package com.nextgen.gameaggregator.vendor.pgsoft.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommonVo implements HttpResponse {
    // This variable will be null when there is no error.
//    @Nullable
    private CommonErrorVo error = null;

    public void setErrorCode(Integer code) {
        if (this.error == null) {
            this.setError(new CommonErrorVo());
        }
        this.error.setCode(code);
    }

    public void setErrorMessage(String message) {
        if (this.error == null) {
            this.setError(new CommonErrorVo());
        }
        this.error.setMessage(message);
    }

    @Override
    public boolean hasError() {
        return this.error != null;
    }
}
