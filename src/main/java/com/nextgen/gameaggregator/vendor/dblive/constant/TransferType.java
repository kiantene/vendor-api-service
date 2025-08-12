package com.nextgen.gameaggregator.vendor.dblive.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TransferType {

    //Settle
    public static final String CANCEL = "CANCEL";
    public static final String REPAYOUT = "REPAYOUT";

    //ActivityPayout
    public static final String DEDUCTION = "DEDUCTION"; //Deduct money consider as settle lose
    public static final String PAYOUT = "PAYOUT"; //Add money consider as settle win
    public static final String ROLLBACK = "ROLLBACK"; //if add/deduct money fail will perform rollback
}
