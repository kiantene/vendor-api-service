package com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;

import javax.validation.constraints.NotBlank;

public class AbstractActionDto {

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String hash;

}
