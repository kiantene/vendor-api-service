package com.nextgen.gameaggregator.vendor.api.pgsoft.callback.v2_4_4.cashget;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.ConstantValidationErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pgsoft.component.dto.AbstractActionDto;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceActionDto extends AbstractActionDto {
    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String playerName;
    //* Below are not mandatory
    private String operatorPlayerSession;
    private Integer gameId;
}
