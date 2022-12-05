package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.authenticate;

//import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto.AbstractActionDto;
import lombok.Data;

import javax.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationDto extends AbstractActionDto {
    private String token;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;
}
