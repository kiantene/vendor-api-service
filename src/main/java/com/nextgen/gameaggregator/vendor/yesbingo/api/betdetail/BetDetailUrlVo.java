package com.nextgen.gameaggregator.vendor.yesbingo.api.betdetail;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BetDetailUrlVo implements com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo {
    private String status;

    @NotNull(message = "url cannot be blank")
    private List<UrlVo> data;

    @Override
    public String getBetDetailUrl() {
        if (data.size() > 0 && data.get(0) != null && data.get(0).getPath() != null) {
            return data.get(0).getPath();
        }

        return null;
    }
}
