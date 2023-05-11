package com.nextgen.gameaggregator.vendor.bng.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckPlayerVo implements HttpResponse {

    private String uid;      // Identifier of the user within the Casino Operator’s system
    private PlayerVo playerVo;    // Currency of the player
    private BalanceVo balanceVo;    // Real balance of the player
    private String tag;       // Token/session of the player

    @Override
    public boolean hasError() {
        return false;
    }
}