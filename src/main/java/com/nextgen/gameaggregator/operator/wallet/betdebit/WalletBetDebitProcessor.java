package com.nextgen.gameaggregator.operator.wallet.betdebit;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InternalServerException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.WalletTransactionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletBetDebitProcessor {

    private final WalletRequestService walletRequestService;
    private final WalletTransactionService walletTransactionService;

    public WalletBetDebitProcessor(WalletRequestService walletRequestService,
                                   WalletTransactionService walletTransactionService) {

        this.walletRequestService = walletRequestService;
        this.walletTransactionService = walletTransactionService;
    }

    public WalletRequest process(WalletRequest walletRequest) throws InternalServerException, BetNotAllowedException, InsufficientBalanceException, InvalidOperatorResponseException {

        walletRequestService.initialise(walletRequest);

        WalletTransaction walletTransaction = walletTransactionService.prepareEntity(walletRequest, OperatorWalletService.DEBIT);

        walletTransactionService.save(walletTransaction);

        WalletBetDebitDto dto = this.prepareOperatorRequestData(walletRequest);

        new WalletBetDebitAction().callToOperator(walletRequest, dto);

        walletTransaction.setOperatorStatus(walletRequest.getOperatorResponseStatus().code);
        walletTransactionService.save(walletTransaction);

        return walletRequest;
    }

    public WalletBetDebitDto prepareOperatorRequestData(WalletRequest walletRequest) {
        WalletBetDebitDto dto = new WalletBetDebitDto();
        BigDecimal amount = walletRequest.getTransferAmount();

        boolean isAmountPositive = amount != null && amount.compareTo(BigDecimal.ZERO) > 0;

        if (isAmountPositive) {
            dto.setAmount(amount.multiply(walletRequest.getFromVendorRate()));
        }

        dto.setTraceId(walletRequest.getTraceId());
        dto.setTransactionId(walletRequest.getTransactionId());
        dto.setUsername(walletRequest.getOperatorUsername());
//        dto.setExternalTransactionId(walletRequest.getExternalTransactionId());
//        dto.setTakeAll(walletRequest.getTakeAll());
        dto.setGameCode(walletRequest.getGameCode());
        dto.setCurrency(walletRequest.getCurrencyCode());
        dto.setRoundId(walletRequest.getRoundId());
        dto.setTimestamp(walletRequest.getTimestamp());
        dto.setToken(walletRequest.getToken());

        return dto;
    }
}
