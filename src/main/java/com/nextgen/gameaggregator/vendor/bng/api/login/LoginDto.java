package com.nextgen.gameaggregator.vendor.bng.api.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.bng.api.action.ArgsDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.checkerframework.checker.index.qual.Positive;

@Data
public class LoginDto {

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
}
