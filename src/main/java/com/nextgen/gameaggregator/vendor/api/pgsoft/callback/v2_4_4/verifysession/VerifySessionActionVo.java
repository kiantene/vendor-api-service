package com.nextgen.gameaggregator.vendor.api.pgsoft.callback.v2_4_4.verifysession;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.vo.AbstractActionVo;
import lombok.Data;

@Data
public class VerifySessionActionVo extends AbstractActionVo {

    private ResponseData data;

    public VerifySessionActionVo() {
        this.data = new ResponseData();
    }

    @Data
    private class ResponseData {
        private String playerName;
//        private String nickname;
        private String currency;
    }

    public void setPlayerName(String playerName) {
        this.data.setPlayerName(playerName);
    }

    public void setCurrency(String currency) {
        this.data.setCurrency(currency);
    }

}
