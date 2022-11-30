package com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.constant;

public class ConstantErrorMessage {

    //region SUCCESS
    public static final String SUCCESS_CODE = "0";
    public static final String SUCCESS_MESSAGE = "Success";
    //endregion

    //region INVALID_PARAM
    public static final String INVALID_PARAM_CODE = "2";
    public static final String INVALID_PARAM = "Invalid parameter";
    //endregion

    //region UNEXPECTED_ERROR
    public static final String UNEXPECTED_ERROR_CODE = "1";

    public static final String UNEXPECTED_ERROR = "Unexpected UNEXPECTED";
    //endregion

    //region COMMON ERROR
    public static final String CANNOT_NULL = "cannot be empty";

    public static final String EXCEED_MAX = "cannot more than";
    public static final String MIN_REQUIRED = "cannot less than";

    public static final String NOT_INTEGER = "must be a number";

    public static final String NOT_FOUND = "not found";

    public static final String NOT_SUPPORT = "not support";

    public static final String DISABLE = "was disable";

    //endregion


    public static final String VENDOR_GROUP_NOT_VALID = "'s vendor group not valid";

    public static final String VENDOR_GROUP_DISABLE = "'s vendor group was disable";

    public static final String INVALID_TRACE_ID = "invalid UUID format";
}
