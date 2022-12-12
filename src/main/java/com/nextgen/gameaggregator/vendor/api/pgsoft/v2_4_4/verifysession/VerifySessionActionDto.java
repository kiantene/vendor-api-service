package com.nextgen.gameaggregator.vendor.api.pgsoft.v2_4_4.verifysession;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.ConstantValidationErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pgsoft.component.dto.AbstractActionDto;
import lombok.Data;

import javax.validation.constraints.*;

@Data
public class VerifySessionActionDto extends AbstractActionDto {

    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String operator_player_session;

    //* Below are not mandatory
    private String ip;
    private String custom_parameter;
    private Integer gameId;

}
