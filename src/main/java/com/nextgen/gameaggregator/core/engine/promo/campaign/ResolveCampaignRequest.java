package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Request body for the promo-engine's generic {@code /internal/resolveCampaign} endpoint.
 * {@code strategy} selects the resolution path; {@code params} carries the keys that strategy expects.
 */
@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResolveCampaignRequest {
    private String traceId;
    private CampaignResolveStrategy strategy;
    private Map<String, String> params;
}
