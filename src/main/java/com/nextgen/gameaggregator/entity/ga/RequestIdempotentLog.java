package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@TypeAlias("request_idempotent_log")
@Collection("request_idempotent_log")
@Data
public class RequestIdempotentLog {
    @Id
    private String id;
    private Long createTime;

}
