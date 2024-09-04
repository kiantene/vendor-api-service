package com.nextgen.gameaggregator.vendor.cg.api.gameurl;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    @NotBlank
    private String gameUrl;
}
