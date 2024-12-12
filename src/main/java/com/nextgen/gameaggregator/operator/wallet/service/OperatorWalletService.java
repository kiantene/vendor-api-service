package com.nextgen.gameaggregator.operator.wallet.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.exception.*;

public interface OperatorWalletService {

    String DEBIT = "debit";
    String CREDIT = "credit";

    WalletRequest betDebit(WalletRequest walletRequest) throws InvalidRequestException, VendorCurrencyNotSupportException, BetNotAllowedException, InternalServerException, InsufficientBalanceException, InvalidOperatorResponseException;

    WalletRequest betCredit(WalletRequest walletRequest) throws InternalServerException, InsufficientBalanceException, InvalidOperatorResponseException, BetNotAllowedException;

    WalletRequest debitRefundByExternalTransactionId(WalletRequest walletRequest) throws InternalServerException, InsufficientBalanceException, InvalidOperatorResponseException, BetNotFoundException, BetNotAllowedException;
}
