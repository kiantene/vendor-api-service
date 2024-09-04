package com.nextgen.gameaggregator.vendor.cg.api.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordVo implements HttpResponse {

    private DataDto data;
    private String channelId;
    private Integer errorCode;
    private String returnTime;

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
