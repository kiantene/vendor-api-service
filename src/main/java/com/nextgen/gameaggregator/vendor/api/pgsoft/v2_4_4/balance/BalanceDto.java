package com.nextgen.gameaggregator.vendor.api.pgsoft.v2_4_4.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.ConstantValidationErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pgsoft.component.dto.AbstractActionDto;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto extends AbstractActionDto {
    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String playerName;
    private String operatorPlayerSession;
    private Integer gameId;
}
