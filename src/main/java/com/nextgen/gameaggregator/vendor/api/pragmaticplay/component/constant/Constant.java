package com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant;

public class Constant {

    public static final String VENDOR_CODE = "PP";
    public static final String API_VERSION = "v1";
    public static final String WEB_ACTION = "api/" + API_VERSION + "/prammaticplay/";


    //region vendor incoming APIs
    public static final String AUTHENTICATE_ACTION = WEB_ACTION + "/authenticate";
    //endregion


    //region vendor outgoing APIs
    public static final String SEAMLESS_GAME_LOGIN = "/game/url";
    //endregion
}
