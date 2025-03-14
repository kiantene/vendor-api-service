package com.nextgen.gameaggregator.vendor.ag.constant;

public class EndPoints {

    public static final String PATH = "api/v1/agslot";
    public static final String CHECKANDCREATE_ACCOUNT = "/doBusiness.do";
    public static final String SESSION_TOKEN = "/resource/player-tickets.ucs";
    public static final String LAUNCH_GAME = "/forwardGame.do";
    public static final String POST_SLOT_GAME = "/rest/integration/slot";

    private EndPoints() {
        throw new UnsupportedOperationException("Cannot instantiate utility class.");
    }
}
