package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@Collection("bet_not_found_log")
@Data
@NoArgsConstructor
public class BetNotFoundLog extends BetInformation {

}
