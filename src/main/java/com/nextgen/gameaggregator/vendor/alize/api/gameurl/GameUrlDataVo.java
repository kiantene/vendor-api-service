package com.nextgen.gameaggregator.vendor.alize.api.gameurl;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GameUrlDataVo {
    @NotBlank(message = "url cannot be blank")
    private String gameUrl;

    private String data;
    
    @NotBlank(message = "token cannot be blank")
    private String token;
}
