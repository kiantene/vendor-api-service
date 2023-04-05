package com.nextgen.gameaggregator.vendor.joker.api.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenDto {

    private String token;
    private String ip;

    private Long timestamp;

    private String appid;

    private String hash;
}
