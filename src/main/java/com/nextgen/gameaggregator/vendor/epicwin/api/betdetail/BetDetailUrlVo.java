package com.nextgen.gameaggregator.vendor.epicwin.api.betdetail;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private Integer Status;
    private String Description;
    private String ResponseDateTime;

    @NotNull(message = "url cannot be blank")
    private String Url;

    @Override
    public String getBetDetailUrl() {
        return this.getUrl();
    }
}
