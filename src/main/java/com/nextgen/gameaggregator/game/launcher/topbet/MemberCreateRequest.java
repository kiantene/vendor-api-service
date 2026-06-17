package com.nextgen.gameaggregator.game.launcher.topbet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberCreateRequest {

    private String pid;         // Merchant ID
    private String ver;         // Version number (Fixed string "2.0.0")
    private String method;      // Interface name (Fixed string "REGISTER")
    private String username;    // Player’s username (Must be unique across all sub-sites)
    private Integer org;        // Sub-site ID (org > 0 && org <= 65535)
    private String ip;          // Player’s IP address (IPv4)
    private String sign;
}
