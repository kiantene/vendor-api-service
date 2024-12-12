package com.nextgen.gameaggregator.entity.ga;

import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;

@Document
@Scope("raw")
@Collection("wallet_transaction")
@Data
public class WalletTransaction {

    @NotBlank(message = "id cannot be blank")
    private String id;
    @NotNull(message = "vendorId cannot be null")
    private Integer vendorId;
    @NotBlank(message = "vendorPlayerUsername cannot be blank")
    private String vendorPlayerUsername;
    @NotBlank(message = "token cannot be blank")
    private String token;
    @NotBlank(message = "vendorGameCode cannot be blank")
    private String vendorGameCode;
    @NotNull(message = "currencyId cannot be null")
    private Integer currencyId;
    @NotBlank(message = "transactionId cannot be blank")
    private String transactionId;
    @NotBlank(message = "betId cannot be blank")
    private String betId;
    @NotBlank(message = "externalTransactionId cannot be blank")
    private String externalTransactionId;
    @NotBlank(message = "vendorBetId cannot be blank")
    private String vendorBetId;
    @NotBlank(message = "roundId cannot be blank")
    private String roundId;
    @NotBlank(message = "action cannot be blank")
    private String action;
    @NotNull(message = "takeAll cannot be null")
    private Integer takeAll;
    @NotNull(message = "transferAmount cannot be null")
    private BigDecimal transferAmount;
    @NotNull(message = "balance cannot be null")
    private BigDecimal balance;
    @NotNull(message = "operatorStatus cannot be null")
    private Integer operatorStatus;
    @NotNull(message = "timestamp cannot be null")
    private Long timestamp;
    @NotNull(message = "createDate cannot be null")
    private Long createdDate;

    public WalletTransaction() {
        this.operatorStatus = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        this.balance = BigDecimal.ZERO;
        this.createdDate = System.currentTimeMillis();
    }
}
