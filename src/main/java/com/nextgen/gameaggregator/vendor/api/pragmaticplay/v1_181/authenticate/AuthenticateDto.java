package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto {
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String hash;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String token;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String gameId;
}
