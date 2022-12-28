package com.nextgen.gameaggregator.vendor.pgsoft.api.authenticate;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.ConstantValidationErrorMessage;
import com.nextgen.gameaggregator.vendor.pgsoft.dto.CommonDto;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class VerifySessionDto extends CommonDto {
    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String operatorPlayerSession;

    //* Below are not mandatory
    private String ip;
    private String customParameter;
    private Integer gameId;

}


