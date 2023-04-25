package com.nextgen.gameaggregator.vendor.spadegaming.api.betdetail;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UrlVo {
    @NotBlank(message = "url can not be blank")
    private String Url;
}
