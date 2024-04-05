package com.nextgen.gameaggregator.vendor.ambslot.api.betdetail;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DataVo {
    @NotBlank(message="url can not be blank")
    private String url;
}
