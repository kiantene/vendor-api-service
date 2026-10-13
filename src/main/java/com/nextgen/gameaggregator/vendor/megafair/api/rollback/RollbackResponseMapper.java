package com.nextgen.gameaggregator.vendor.megafair.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.megafair.constant.Formats;
import com.nextgen.gameaggregator.vendor.megafair.constant.Results;
import com.nextgen.gameaggregator.vendor.megafair.util.VendorUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<RollbackResponse> {
    private static final DateTimeFormatter VENDOR_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT).withLocale(Locale.ENGLISH);

    private final VendorUtil vendorUtil;

    public RollbackResponseMapper(VendorUtil vendorUtil) {
        this.vendorUtil = vendorUtil;
    }

    @Override
    public RollbackResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        BigDecimal balance = resolveBalance(context, balanceData);

        return RollbackResponse.builder()
                .time(VENDOR_DATE_FORMATTER.format(ZonedDateTime.now()))
                .response(
                        RollbackResponse.ResponseData.builder()
                                .result(Results.SUCCESS)
                                .extSystemTransactionId(context.getTransactionId())
                                .balance(balance)
                                .build()
                )
                .build();
    }

    private BigDecimal resolveBalance(BetRollbackContext context, PlayerBalanceData balanceData) {
        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;

        if (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
            return balance;
        }

        // Fallback balance lookup using vendorUtil if balance is null or zero
        if (context != null && context.getVendorPlayerUsername() != null) {
            BigDecimal playerBalance = vendorUtil.getPlayerBalanceDataByUserId(context.getVendorPlayerUsername());
            if (playerBalance != null) {
                return playerBalance;
            }
        }

        return balance;
    }
}
