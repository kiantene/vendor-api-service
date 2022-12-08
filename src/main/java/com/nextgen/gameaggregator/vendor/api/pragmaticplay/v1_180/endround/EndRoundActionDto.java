package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto.AbstractActionDto;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundActionDto extends AbstractActionDto {

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String userId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String gameId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String roundId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;
}
