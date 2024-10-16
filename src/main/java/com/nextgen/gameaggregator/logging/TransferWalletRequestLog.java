package com.nextgen.gameaggregator.logging;

import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class TransferWalletRequestLog extends BaseApiLog {

    public static final String BALANCE = "Balance";
    public static final String DEPOSIT = "Deposit";
    public static final String WITHDRAWAL = "Withdrawal";
    public static final String GET_TXN = "TxnInfo";

    private Integer agentId;
    private String apiKey;
    private String signature;
    private ResponseCodes.Status responseStatus;

    private String username;
    private String currency;
    private BigDecimal amount;

    private Object walletData;
    private String walletResponse;
    private ResponseCodes.Status walletResponseStatus;
    private Integer walletHttpStatusCode;

    private Long walletStart;
    private Long walletEnd;
    private Long walletTimeTaken;

    public TransferWalletRequestLog() {
        super();
    }

    public void setWalletEnd(Long walletEnd) {
        this.walletEnd = walletEnd;
        if (this.walletStart != null && this.walletStart > 0) {
            this.walletTimeTaken = this.walletEnd - this.walletStart;
        }
    }

    @Override
    public void setEnd(Long end) {
        this.end = end;
        this.timeTaken = this.end - this.start;

        if (this.getWalletTimeTaken() != null && this.getWalletTimeTaken() > 0L) {
            this.gaTimeTaken = this.timeTaken - this.getWalletTimeTaken();
        }
    }
}
