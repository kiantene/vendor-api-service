package com.nextgen.gameaggregator.vendor.bglive.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private ResultDto parsedResult; // 额外存储解析后的对象

    public boolean isSuccess() {
        return parsedResult != null && parsedResult.isSuccess();
    }

    public boolean isFailed() {
        return "0".equals(result); // 失败的情况
    }

    public void parseResult(ObjectMapper objectMapper) {
        if (result != null && !"0".equals(result)) {
            try {
                this.parsedResult = objectMapper.readValue(result, ResultDto.class);
            } catch (Exception e) {
                this.parsedResult = null;
            }
        }
    }


    @Override
    public String getGameUrl() {
        return this.data;
    }
}
