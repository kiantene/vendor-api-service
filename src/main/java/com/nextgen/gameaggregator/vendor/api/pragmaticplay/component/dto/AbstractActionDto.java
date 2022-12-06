package com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import lombok.Data;

import javax.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbstractActionDto {

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String hash;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String token;

}
