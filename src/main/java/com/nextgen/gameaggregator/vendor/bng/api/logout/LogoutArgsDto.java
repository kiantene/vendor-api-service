package com.nextgen.gameaggregator.vendor.bng.api.logout;

import lombok.Data;

@Data
public class LogoutArgsDto {
    private String reason;
    private Object player;
    private String tag;
}
