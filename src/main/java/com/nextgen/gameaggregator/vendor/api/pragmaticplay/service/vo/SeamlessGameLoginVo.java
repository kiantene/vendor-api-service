package com.nextgen.gameaggregator.vendor.api.pragmaticplay.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.grpc.constant.ConstantErrorMessage;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SeamlessGameLoginVo {

    @NotBlank(message = "error" + ConstantErrorMessage.NOT_BLANK)
    private String error;
    @NotBlank(message = "description" + ConstantErrorMessage.NOT_BLANK)
    private String description;
    @NotBlank(message = "gameURL" + ConstantErrorMessage.NOT_BLANK)
    private String gameURL;
}
