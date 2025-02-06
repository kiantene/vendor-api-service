package com.nextgen.gameaggregator.vendor.bglive.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    @NotNull
    private String random;

    @NotBlank
    private String digest;

    @NotNull
    private String sn;

    @NotBlank
    private String loginId;

    @NotBlank
    private String agentLoginId;

}
