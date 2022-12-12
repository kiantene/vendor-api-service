package com.nextgen.gameaggregator.vendor.api.pgsoft.component.dto;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.ConstantValidationErrorMessage;

import javax.validation.constraints.*;

public class AbstractActionDto {
    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String operatorToken;
    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String secretKey;
}
