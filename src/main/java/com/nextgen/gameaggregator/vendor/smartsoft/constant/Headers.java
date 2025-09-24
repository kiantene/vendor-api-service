package com.nextgen.gameaggregator.vendor.smartsoft.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Headers {
    public static final String REQUEST_SIGNATURE = "X-Signature";
    public static final String SESSION_ID = "X-SessionId";
    public static final String USER_NAME = "X-UserName";
    public static final String CLIENT_EXTERNAL_KEY = "X-ClientExternalKey";
    public static final String ERROR_MESSAGE = "X-ErrorMessage";
    public static final String ERROR_CODE = "X-ErrorCode";
}