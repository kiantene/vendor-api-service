package com.nextgen.gameaggregator.vendor.aglive.constant;

public class EndPoints {

    public static final String PATH = "api/v1/aglive";
    public static final String CHECK_AND_CREATE_ACCOUNT = "/doBusiness.do";
    public static final String SESSION_TOKEN = "/resource/player-tickets.ucs";
    public static final String LAUNCH_GAME = "/forwardGame.do";
    public static final String POST_LIVE_GAME = "/rest/integration/postTransfer";

    private EndPoints() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

}
