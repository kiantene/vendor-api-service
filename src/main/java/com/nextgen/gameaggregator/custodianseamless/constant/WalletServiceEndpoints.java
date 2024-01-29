package com.nextgen.gameaggregator.custodianseamless.constant;

public class WalletServiceEndpoints {



    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_SIGNATURE = "X-Signature";

    public static final String WALLET_DEPOSIT = "/wallet/deposit";
    public static final String WALLET_WITHDRAW = "/wallet/withdraw";
    public static final String WALLET_BALANCE = "/wallet/balance";


    public static final String OPERATOR_ENDPOINT = "cash";
    public static final String OPERATOR_DEPOSIT = "deposit";
    public static final String OPERATOR_WITHDRAW = "withdraw";
    public static final String OPERATOR_BALANCE = "balance";
    public static final String OPERATOR_GET_SINGLE_TRANSACTION = "getsingletransaction";
}
