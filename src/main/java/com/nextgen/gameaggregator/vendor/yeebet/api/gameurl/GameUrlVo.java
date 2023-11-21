package com.nextgen.gameaggregator.vendor.yeebet.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {

    //check the status code while access vendor's game
    @NotNull(message = "result cannot be blank")
    private Integer result;

    //msg about the current status
    @NotNull(message = "desc can not be blank")
    private String desc;

    //vendor's game url
    private String openurl;

    @Override
    public String getGameUrl() {
        if(this.getResult().equals(0) && this.getOpenurl() != null){
            return this.getOpenurl();
        }else{
            return null;
        }
    }
}