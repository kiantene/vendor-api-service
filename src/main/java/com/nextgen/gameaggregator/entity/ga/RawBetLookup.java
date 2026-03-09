package com.nextgen.gameaggregator.entity.ga;

import org.springframework.data.annotation.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.time.Instant;

@Document
@Scope("raw")
@Collection("bet_lookup")
@Data
@NoArgsConstructor
public class RawBetLookup {
    @Id
    private String id;
    private String vendorBetId;
    private Integer vendorGameId;
    private String roundId;
    private Long timestamp;

    public static RawBetLookup of(String vendorBetId, String externalTransactionId, String roundId, Integer vendorGameId, Long vendorPlayerId) {
        RawBetLookup lookup = new RawBetLookup();
        lookup.id = generateId(vendorPlayerId, externalTransactionId);
        lookup.vendorBetId = vendorBetId;
        lookup.vendorGameId = vendorGameId;
        lookup.roundId = roundId;
        lookup.timestamp = Instant.now().toEpochMilli();
        return lookup;
    }

    public static String generateId(Long vendorPlayerId, String externalTransactionId) {
        return vendorPlayerId + "_" + externalTransactionId;
    }

    public String toDocumentId(Long vendorPlayerId) {
        return this.getVendorBetId() + '_' + this.getRoundId() + '_' + this.getVendorGameId() + '_' + vendorPlayerId;
    }
}
