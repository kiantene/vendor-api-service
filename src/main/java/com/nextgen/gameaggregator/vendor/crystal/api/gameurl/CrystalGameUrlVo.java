package com.nextgen.gameaggregator.vendor.crystal.api.gameurl;

import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import lombok.Data;

@Data
public class CrystalGameUrlVo implements GameUrlVo {

    private String gameUrl;

    @Override
    public String getGameUrl() {
        return this.gameUrl;
    }
}
