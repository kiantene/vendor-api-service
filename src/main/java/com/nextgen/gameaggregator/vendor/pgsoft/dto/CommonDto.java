package com.nextgen.gameaggregator.vendor.pgsoft.dto;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.ConstantValidationErrorMessage;
import lombok.Data;

import javax.validation.constraints.*;

@Data
public class CommonDto {
    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String operatorToken;
    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String secretKey;
}
