package com.nextgen.gameaggregator.vendor.queenmaker.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizeDto {

    private String authtoken;
    private Boolean isnew;

}
