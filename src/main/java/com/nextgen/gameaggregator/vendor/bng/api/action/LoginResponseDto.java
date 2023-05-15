package com.nextgen.gameaggregator.vendor.bng.api.action;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponseDto implements HttpResponse {

    private String name;
    private String uid;
    private String token;
    private String session;
    private String game_id;
    private String game_name;
    private String provider_id;
    private String provider_name;
    private String c_at;
    private String sent_at;
    private ArgsDto args;

    @Override
    public boolean hasError() {
        return false;
    }
}
