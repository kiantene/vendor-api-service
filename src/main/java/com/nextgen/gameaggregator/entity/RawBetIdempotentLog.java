package com.nextgen.gameaggregator.entity;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;
import java.math.BigDecimal;

@Document
@Scope("raw")
@TypeAlias("bet_idempotent_log")
@Collection("bet_idempotent_log")
@Data
public class RawBetIdempotentLog {
    @Id
    private String id;
    private BigDecimal balance;

}
