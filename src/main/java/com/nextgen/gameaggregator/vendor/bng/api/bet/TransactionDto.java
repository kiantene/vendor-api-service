package com.nextgen.gameaggregator.vendor.bng.api.bet;

import lombok.Data;

@Data
public class TransactionDto {
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
    private TransactionArgsDto args;
}
