package com.nextgen.gameaggregator.promoengine.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    public static final String FETCH_CAMPAIGN = "/internal/fetchCampaign";
    public static final String FETCH_CAMPAIGN_BY_PLAYER = "/internal/fetchCampaignByPlayer";
    public static final String RESOLVE_CAMPAIGN = "/internal/resolveCampaign";
    public static final String FETCH_PLAYER_ACTIVE_CAMPAIGN = "/internal/fetchPlayerActiveCampaign";
}

