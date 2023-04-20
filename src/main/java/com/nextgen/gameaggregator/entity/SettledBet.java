package com.nextgen.gameaggregator.entity;

import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import jakarta.persistence.Id;
import java.math.BigDecimal;

@Document
@Scope("raw")
@Collection("settled_bet")
@Data
public class SettledBet extends BetInformation {

}
