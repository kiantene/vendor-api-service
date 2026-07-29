package com.nextgen.gameaggregator.vendor.cockfight6.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestType {
    // Constants for request types
    public static final int REQ_TYPE_TRANSFER = 1; // api转入转出
    public static final int REQ_TYPE_BET = 3; // 下分
    public static final int REQ_TYPE_SETTLE = 4; // 派彩
    public static final int REQ_TYPE_ROLLBACK = 5; // 回滚
    public static final int REQ_TYPE_FIX = 6; // 修复
}
