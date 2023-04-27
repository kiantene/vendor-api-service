package com.nextgen.gameaggregator.vendor.jdb.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    @NotBlank
    private String path;

    @NotBlank
    @Size(min = 4, max = 4)
    private String status;

    @Size(min = 1, max = 255)
    private String err_text;

    @Override
    public String getGameUrl() {
        return path;
    }
}
