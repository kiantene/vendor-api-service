package com.nextgen.gameaggregator.vendor.winfinity.api.gameurl;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GameUrlDataVo {
    @NotBlank
    private String frameUrl;
    private String masterSessionId;
}
