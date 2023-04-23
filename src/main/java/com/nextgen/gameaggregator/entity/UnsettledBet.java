package com.nextgen.gameaggregator.entity;

import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@Collection("unsettled_bet")
@Data
public class UnsettledBet extends BetInformation {

}
