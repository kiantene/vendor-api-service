package com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.vo;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Map;

@Data
public class AbstractActionVo {
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    @Size(min = 36, max = 36, message = ConstantErrorMessage.SIZE_MIN_MAX +" 36 and 36")
    public String traceId;
    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private Integer error;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String description;

    public AbstractActionVo() {
        this.setTraceId("");
        this.setErrorAndDescriptionByConstantResponseKey(ConstantErrorMessage.RESPONSE_KEY_INTERNAL_SERVER_ERROR);
    }

    public void verifyValidationResultAndManipulateErrorAndDescription(Map<String, String> validationResult) {
        if (validationResult.isEmpty()) {
            this.setErrorAndDescriptionByConstantResponseKey(ConstantErrorMessage.RESPONSE_KEY_SUCCESS);
        } else {
            this.setErrorAndDescriptionByConstantResponseKey(ConstantErrorMessage.RESPONSE_KEY_INVALID_PARAM);
        }
    }

    public void setErrorAndDescriptionByConstantResponseKey(String constantResponseKey) {
        this.setError(ConstantErrorMessage.RESPONSE_CODES.get(constantResponseKey));
        this.setDescription(ConstantErrorMessage.RESPONSE_MESSAGES.get(constantResponseKey));
    }

}