package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingsDto {
    private String user_id;
    private String exit_url;
    private String cash_url;
    private String language;
    private String https;
    private ExtraBonusDto extra_bonuses;
    private ExtraBonusesSettings extra_bonuses_settings;
    private String payout;
    private Integer reality_check;
}
