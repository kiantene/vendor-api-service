package com.nextgen.gameaggregator.vendor.aasexyv2.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String status;
    
    @NotBlank(message = "url can not be blank")
    private String url;


    @Override
    public String getGameUrl() {
        return url;
    }
}
