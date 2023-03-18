package com.nextgen.gameaggregator.vendor.jdb.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    @NotBlank
    private String path;

    @NotBlank
    @Size(min = 4, max = 4)
    private String status;

    @NotBlank
    @Size(min = 1, max = 255)
    private String err_text;

    @Override
    public String getGameUrl() {
        return path;
    }
}
