package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.Currency;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.core.service.data.CampaignDataService;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class PromoPayoutContextEnricher extends BaseEnricher<PromoPayoutContext> {
    private final CurrencyDataService currencyDataService;
    private final VendorDataService vendorDataService;
    private final AgentDataService agentDataService;
    private final CampaignDataService campaignDataService;

    public PromoPayoutContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                      VendorPlayerDataService vendorPlayerDataService,
                                      CurrencyDataService currencyDataService,
                                      VendorDataService vendorDataService,
                                      VendorGameDataService vendorGameDataService,
                                      AgentDataService agentDataService,
                                      CampaignDataService campaignDataService) {

        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService);
        this.currencyDataService = currencyDataService;
        this.vendorDataService = vendorDataService;
        this.agentDataService = agentDataService;
        this.campaignDataService = campaignDataService;
    }

    @Override
    public void prepareContext(PromoPayoutContext context) {
        if (context.getHttpRequestLog() == null) {
            context.setHttpRequestLog(LogContextService.toHttpRequestLog(LogContextHolder.get()));
        }
    }

    public void doEnrich(PromoPayoutContext context) {
        this.populateAgent(context);
        this.populateCurrency(context);
        this.populateVendor(context);
        this.populateCampaign(context);
        LogContext logContext = LogContextHolder.get();
        logContext.setVendorId(context.getVendor().id());
        logContext.setAgentId(context.getAgent().id());
        logContext.setUsername(context.getAgent().playerUsername());

        if (context.getTraceId() == null) {
            context.setTraceId(logContext.getTraceId());
        }
    }

    private void populateCurrency(PromoPayoutContext context) {
        Currency currency = currencyDataService.get(context.getCurrencyId());
        context.setCurrencyCode(currency.getCode());
    }

    private void populateAgent(PromoPayoutContext context) {
        Agent agent = agentDataService.get(context.getAgent().id());
        context.getAgent().masterAgentId(agent.getMasterAgentId());
        context.getAgent().houseId(agent.getHouseId());
    }

    private void populateVendor(PromoPayoutContext context) {
        Vendor vendor = vendorDataService.get(context.getVendor().id());
        context.getVendor().code(vendor.getCode());
    }

    private void populateCampaign(PromoPayoutContext context) {
        if (Objects.isNull(context.getVendorCampaignCode())) {
            return;
        }

        try {
            Campaign campaign = campaignDataService.get(context.getVendorCampaignCode(), context.getVendor().id(), context.getCurrencyCode());
            context.setCampaignUuid(campaign.getUuid());
        } catch (EntityNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }
}
