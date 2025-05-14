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
    private Object result;

    @JsonProperty("error")
    private ErrorDto error;
    
    public boolean isSuccess() {
        if (result instanceof ResultDto dto) {
            return dto.isSuccess();
        }
        return false;
    }

    public boolean isFailed() {
        if (result instanceof String str) {
            return "0".equals(str);
        }
        return !isSuccess();
    }


    @Override
    public String getGameUrl() {
        return this.data;
    }
}
