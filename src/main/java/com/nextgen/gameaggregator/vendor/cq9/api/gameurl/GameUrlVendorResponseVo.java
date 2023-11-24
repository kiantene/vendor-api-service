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
        String leaveUrl = Optional.ofNullable(this.getLeaveUrl())
                .filter(url -> !url.isEmpty())
                .orElse(null);

        return this.data.getUrl() + (leaveUrl == null ? "" : "&leaveUrl=" + leaveUrl);
    }
}
