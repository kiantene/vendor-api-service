package com.nextgen.gameaggregator.vendor.jdb.api.betdetail;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private String status;
    @NotEmpty(message = "Empty Array")
    private List<UrlVo> data;
    private String err_text;

    @Override
    public String getBetDetailUrl() { return data.get(0).getPath(); }
}
