package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.evoplay.dto.BasicDto;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameUrlDto extends BasicDto {
    private String token;
    private String game;
    private SettingsDto settings;
    private String denomination;
    private String currency;
    private String return_url_info;
    private String callback_version;
}
