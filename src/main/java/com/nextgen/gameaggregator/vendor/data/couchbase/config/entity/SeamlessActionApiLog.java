package com.nextgen.gameaggregator.vendor.data.couchbase.config.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document(expiry = 864000)
@Scope("log")
@Collection("seamless_action_api_log")
@Data
@AllArgsConstructor
public class SeamlessActionApiLog {

    @Id
    private String id;

    private String requestTimeDisplay;

    private Long requestTimeUnix;

    private String endpoint;

    private Long TimeTaken;

    private String requestParam;

    private String response;

    private Integer responseCode;

    private String header;

    private String domain;
}
