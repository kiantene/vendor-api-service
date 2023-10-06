package com.nextgen.gameaggregator.vendor.queenmaker.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizeDto {

    @NotBlank(message = "Token needed to build game URL")
    private String authtoken;
    private Boolean isnew;

}
