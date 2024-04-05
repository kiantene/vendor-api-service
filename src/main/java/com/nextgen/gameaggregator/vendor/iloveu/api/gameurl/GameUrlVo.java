package com.nextgen.gameaggregator.vendor.iloveu.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.iloveu.dto.DataDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {

    @JsonProperty("code")
    private String code;

    @JsonProperty("LoginUrl")
    private String loginUrl;

    @JsonProperty("data")
    private DataDto dataDto;

    @JsonProperty("isSuccessful")
    private Boolean isSuccessful;

    @NotBlank(message = "url can not be blank")
    private String url;

    public GameUrlVo(){
       this.setDataDto(new DataDto());
    }

    @Override
    public String getGameUrl() {
        return this.getUrl();
    }
}
