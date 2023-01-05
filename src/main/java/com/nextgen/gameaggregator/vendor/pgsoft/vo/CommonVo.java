package com.nextgen.gameaggregator.vendor.pgsoft.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.annotation.Nullable;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommonVo {
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

    @JsonIgnore
    public boolean isError() {
        return this.error != null;
    }
}
