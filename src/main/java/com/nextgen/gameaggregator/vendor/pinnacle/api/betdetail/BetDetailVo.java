package com.nextgen.gameaggregator.vendor.pinnacle.api.betdetail;

import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BetDetailVo implements BetDetailUrlVo {

    private String code;
    private String message;
    private String userCode;
    private String loginId;
    private String token;
    @NotNull(message = "Bet detail contain url cannot be null")
    private String loginUrl;
    private String updateDate;

    @Override
    public String getBetDetailUrl() {
        return this.getLoginUrl();
    }
}
