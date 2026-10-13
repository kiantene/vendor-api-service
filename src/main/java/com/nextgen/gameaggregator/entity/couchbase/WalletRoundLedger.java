package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nextgen.gameaggregator.enums.GameRoundState;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Round-level lifecycle doc for the debit/credit family. Holds aggregate state for one
 * vendor-round across N {@link DebitTxn} / {@link CreditTxn} legs.
 *
 * <p><strong>Not a {@link WalletTxn}.</strong> A ledger isn't a transaction — different
 * collection ({@code wallet_round_ledgers}), different doc-id shape, different lifecycle.
 *
 * <p>Doc id: {@code <vendorClassName>::<vendorPlayerUsername>::<vendorRoundId>}.
 *
 * <p>Field-name convention: same as {@link WalletTxn} ({@code vendor*}, {@code agent*},
 * no prefix for platform-internal state). {@link #vendorLastBalance} mirrors the
 * legacy {@code GameRound.lastBalance} semantic — vendor-facing (post-FX), updated
 * on every successful txn append.
 *
 * <p>No bet/win aggregates — deferred until a real consumer demands them.
 */
@Data
public class WalletRoundLedger {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

    @JsonProperty("vendorClassName")
    private String vendorClassName;

    @JsonProperty("vendorPlayerUsername")
    private String vendorPlayerUsername;

    @JsonProperty("vendorRoundId")
    private String vendorRoundId;

    @JsonProperty("vendorId")
    private Integer vendorId;

    @JsonProperty("vendorGameCode")
    private String vendorGameCode;

    /** Agent/platform-side currency (post-FX). NOT vendor currency. */
    @JsonProperty("agentCurrency")
    private String agentCurrency;

    /** Round lifecycle: {@link GameRoundState#UNSETTLED} → {@link GameRoundState#SETTLED}. */
    @JsonProperty("state")
    private GameRoundState state;

    /** True once {@code markSettled} has fired. */
    @JsonProperty("isEnded")
    private Boolean isEnded;

    /** Count of refs appended to {@link #transactions}. Maintained via CAS {@code increment}. */
    @JsonProperty("txnCount")
    private Integer txnCount;

    /** Discovery refs — snapshot-at-append (see {@link WalletRoundTxnRef}). */
    @JsonProperty("transactions")
    private List<WalletRoundTxnRef> transactions;

    /** Vendor-facing balance after the latest successful txn (post-FX). Mirrors {@code GameRound.lastBalance}. */
    @JsonProperty("vendorLastBalance")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal vendorLastBalance;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("createdTs")
    private Long createdTs;

    public WalletRoundLedger() {
        this.state = GameRoundState.UNSETTLED;
        this.isEnded = false;
        this.txnCount = 0;
        this.transactions = new ArrayList<>();
    }

    public static WalletRoundLedger of(String vendorClassName,
                                       String vendorPlayerUsername,
                                       String vendorRoundId,
                                       Integer vendorId,
                                       String vendorGameCode,
                                       String agentCurrency,
                                       long createdTs) {
        WalletRoundLedger ledger = new WalletRoundLedger();
        ledger.setVendorClassName(vendorClassName);
        ledger.setVendorPlayerUsername(vendorPlayerUsername);
        ledger.setVendorRoundId(vendorRoundId);
        ledger.setVendorId(vendorId);
        ledger.setVendorGameCode(vendorGameCode);
        ledger.setAgentCurrency(agentCurrency);
        ledger.setCreatedTs(createdTs);
        ledger.setCreatedAt(DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(createdTs)));
        return ledger;
    }

    public static String createDocId(String vendorClassName, String vendorPlayerUsername, String vendorRoundId) {
        return vendorClassName + "::" + vendorPlayerUsername + "::" + vendorRoundId;
    }

    @JsonIgnore
    public String getId() {
        return createDocId(vendorClassName, vendorPlayerUsername, vendorRoundId);
    }
}
