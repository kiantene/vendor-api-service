package com.nextgen.gameaggregator.vendor.cq9.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import lombok.Data;

import java.util.Optional;

@Data
public class GameUrlVendorResponseVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private GameUrlVo data;
    private StatusVo status;
    @JsonIgnore
    private String leaveUrl;

    @Override
    public String getGameUrl() {
        // CQ9 can reply HTTP 200 with no `data` (e.g. its outage body {"data":null,"status":{...}}).
        // Return null so BaseGameUrlService raises InvalidVendorResponseException("cannot get game url")
        // instead of throwing a NullPointerException here (GA-14464).
        if (this.data == null || this.data.getUrl() == null) {
            return null;
        }

        String leaveUrlValue = Optional.ofNullable(this.getLeaveUrl())
                .filter(url -> !url.isEmpty())
                .orElse(null);

        return this.data.getUrl() + (leaveUrlValue == null ? "" : "&leaveUrl=" + leaveUrlValue);
    }
}
