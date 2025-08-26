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
        logContext.setVendorId(context.getVendorId());
        logContext.setAgentId(context.getAgentId());
        logContext.setUsername(context.getAgentPlayerUsername());
    }

    private void populateCurrency(PromoPayoutContext context) {
        try {
            Currency currency = currencyDataService.get(context.getCurrencyId());
            context.setCurrencyCode(currency.getCode());
        } catch (EntityNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }

    private void populateAgent(PromoPayoutContext context) {
        try {
            Agent agent = agentDataService.get(context.getAgentId());
            context.setMasterAgentId(agent.getMasterAgentId());
            context.setHouseId(agent.getHouseId());
        } catch (EntityNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }

    private void populateVendor(PromoPayoutContext context) {
        try {
            Vendor vendor = vendorDataService.get(context.getVendorId());
            context.setVendorCode(vendor.getCode());
        } catch (EntityNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }

    private void populateCampaign(PromoPayoutContext context) {
        if (Objects.isNull(context.getVendorCampaignCode())) {
            return;
        }

        try {
            Campaign campaign = campaignDataService.get(context.getVendorCampaignCode(), context.getVendorId(), context.getCurrencyCode());
            context.setCampaignUuid(campaign.getUuid());
        } catch (EntityNotFoundException e) {
            throw new InternalConfigurationException(e.getMessage());
        }
    }
}
