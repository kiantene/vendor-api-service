package com.nextgen.gameaggregator.vendor.cg.api.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordVo implements HttpResponse {

    @JsonProperty("data")
    private DataDto data;
    @JsonProperty("channelId")
    private String channelId;
    @JsonProperty("errorCode")
    private Integer errorCode;
    @JsonProperty("returnTime")
    private String returnTime;

    @JsonIgnore
    private String encrypt;

    public RecordVo() {
        this.data = new DataDto();
        this.data.setTarget(new Target());
        this.data.setBalance(new Balance());
        this.data.setStatus(new Status());
        this.data.setIncident(new Incident());
    }

    @Override
    public boolean hasError() {
        return false;
    }


}
