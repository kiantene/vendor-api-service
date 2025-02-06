package com.nextgen.gameaggregator.vendor.bglive.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BgLiveGameUrlVo implements GameUrlVo {

    @NotBlank(message = "url can not be blank")
    private String data;

    @JsonProperty("result")
    private String result;

    @JsonProperty("error")
    private ErrorDto error;

    private ResultDto parsedResult;

    public boolean isSuccess() {
        return parsedResult != null && parsedResult.isSuccess();
    }

    public boolean isFailed() {
        return "0".equals(result);
    }

    @Override
    public String getGameUrl() {
        return this.data;
    }
}
