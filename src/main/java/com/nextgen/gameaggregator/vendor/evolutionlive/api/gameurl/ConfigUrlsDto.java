package com.nextgen.gameaggregator.vendor.evolutionlive.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigUrlsDto {
    private String cashier;
    private String responsibleGaming;
    private String lobby;
    private String sessionTimeout;
}
