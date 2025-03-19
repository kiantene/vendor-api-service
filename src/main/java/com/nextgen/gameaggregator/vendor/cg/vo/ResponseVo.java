package com.nextgen.gameaggregator.vendor.cg.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {

    @JsonProperty("channelId")
    public String channelId; //"52772"
    @JsonProperty("accountId")
    public String accountId; //"abctesting123"
    @JsonProperty("balance")
    public BigDecimal balance; //1000.00
    @JsonProperty("currency")
    public String currency; //"USD"
    @JsonProperty("errorCode")
    public Integer errorCode; //0,
    @JsonProperty("returnTime")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{2}:\\d{2}$")
    public String returnTime;
    @JsonIgnore
    public String encrypt;

    @Override
    public boolean hasError() {
        return false;
    }
}
