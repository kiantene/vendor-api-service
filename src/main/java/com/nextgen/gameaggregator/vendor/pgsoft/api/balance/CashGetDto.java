package com.nextgen.gameaggregator.vendor.pgsoft.api.balance;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.ConstantValidationErrorMessage;
import com.nextgen.gameaggregator.vendor.pgsoft.dto.CommonDto;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CashGetDto extends CommonDto {
    @NotBlank
    @Size(max = 50)
    private String playerName;

    //* Below are not mandatory
    private String operatorPlayerSession;
    private Integer gameId;
}
