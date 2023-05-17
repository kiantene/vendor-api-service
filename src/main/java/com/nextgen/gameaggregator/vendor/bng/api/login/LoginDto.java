package com.nextgen.gameaggregator.vendor.bng.api.login;


import lombok.Data;

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
