package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDto {

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String token;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String hash;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String userId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String gameId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String roundId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;
}
