package com.nextgen.gameaggregator.vendor.alize.api.gameurl;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GameUrlDataVo {
    @NotBlank(message = "url can not be blank")
    private String gameUrl;

    private String data;
    
    private String token;
}
