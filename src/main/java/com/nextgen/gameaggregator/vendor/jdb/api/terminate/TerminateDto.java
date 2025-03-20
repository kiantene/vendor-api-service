package com.nextgen.gameaggregator.vendor.jdb.api.terminate;

import lombok.Data;

@Data
public class TerminateDto {

    private Integer action;
    private Long ts;
    private String parent;
    private String uid;

}
