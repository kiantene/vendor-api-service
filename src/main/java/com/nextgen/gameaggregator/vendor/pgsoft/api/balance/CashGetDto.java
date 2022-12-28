package com.nextgen.gameaggregator.vendor.pgsoft.api.balance;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.ConstantValidationErrorMessage;
import com.nextgen.gameaggregator.vendor.pgsoft.dto.CommonDto;

import javax.validation.constraints.NotBlank;

public class CashGetDto extends CommonDto {
    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String playerName;

    //* Below are not mandatory
    private String operatorPlayerSession;
    private Integer gameId;
}
