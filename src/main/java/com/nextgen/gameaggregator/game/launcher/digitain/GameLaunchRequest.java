package com.nextgen.gameaggregator.game.launcher.digitain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GameLaunchRequest {
    private Integer dvt;
    private String gid;
    private String hmu;
    private String lng;
    private String oid;
    private Integer plm;
    private String tkn;
}
