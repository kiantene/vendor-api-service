package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.core.service.data.CampaignDataService;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Service
public class PromoPayoutContextEnricher extends BaseEnricher<PromoPayoutContext> {
    private final VendorDataService vendorDataService;
    private final AgentDataService agentDataService;
    private final CampaignDataService campaignDataService;

    public PromoPayoutContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                      VendorPlayerDataService vendorPlayerDataService,
                                      VendorGameDataService vendorGameDataService,
                                      CurrencyDataService currencyDataService,
                                      VendorCurrencyDataService vendorCurrencyDataService,
                                      VendorDataService vendorDataService,
                                      AgentDataService agentDataService,
                                      CampaignDataService campaignDataService) {

        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService, currencyDataService, vendorCurrencyDataService);
        this.vendorDataService = vendorDataService;
        this.agentDataService = agentDataService;
        this.campaignDataService = campaignDataService;
    }

    @Override
    public void prepareContext(PromoPayoutContext context) {
        context.setTransactionId(UuidUtil.newUuidV7StringRaw());
        if (context.getHttpRequestLog() == null) {
            context.setHttpRequestLog(LogContextService.toHttpRequestLog(LogContextHolder.get()));
        }

        // if vendor player username is not set, then get the username from the first transaction in the list
        if (context.getVendorPlayerUsername() == null && context.getPayoutTransactions() != null) {
            context.setVendorPlayerUsername(context.getPayoutTransactions().get(0).getVendorPlayerUsername());
        }
    }

    @Override
    public void doEnrich(PromoPayoutContext context) {
        LogContext logContext = LogContextHolder.get();
        if (context.getTraceId() == null) {
            context.setTraceId(logContext.getTraceId());
        }

        // Some vendors (e.g. Spribe) omit a transaction timestamp in their callback payload.
        // Fall back to the HTTP request receive time so downstream processing always has a non-null value.
        if (context.getVendorTransactionTime() == null) {
            context.setVendorTransactionTime(logContext.getStart());
        }
        // Some vendors send transactions in batch (e.g. Facai); each child transaction may also omit a timestamp.
        if (context.getPayoutTransactions() != null) {
            context.getPayoutTransactions().forEach(txn -> {
                if (txn.getVendorTransactionTime() == null) {
                    txn.setVendorTransactionTime(logContext.getStart());
                }
            });
        }

        this.populateAgent(context);
        this.populateVendor(context);
        this.populateCampaign(context);

        logContext.setVendorId(context.getVendor().id());
        logContext.setAgentId(context.getAgent().id());
        logContext.setUsername(context.getAgent().playerUsername());

        if (context.getFromVendorRate() != null) {
            BigDecimal fromVendorRate = context.getFromVendorRate();
            if (context.getVendorPayoutAmount() != null) { // single mode
                context.setPayout(TxnAmount.of(
                        context.getVendorPayoutAmount(),
                        fromVendorRate
                ));
            } else if (context.getPayoutTransactions() != null && !context.getPayoutTransactions().isEmpty()) {
                context.getPayoutTransactions()
                        .forEach(txn -> txn.setPayout(TxnAmount.of(
                                txn.getVendorPayoutAmount(),
                                fromVendorRate
                        )));
            }
        }
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
        if (Objects.isNull(context.getVendorCampaignCode())) return;

        PromoPayoutConfig config = PromoPayoutContextHolder.getConfig();
        Campaign campaign = config.isPlayerUuidCampaignLookup()
                ? campaignDataService.getByPlayerUuid(context.getVendorCampaignCode())
                : campaignDataService.get(context.getVendorCampaignCode(), context.getVendor().lineId(),
                        Optional.ofNullable(context.getPromoType()).map(type -> type.id).orElse(null));

        if (campaign.getUuid() != null) {
            context.setCampaignUuid(campaign.getUuid());
        }
        if (campaign.getCampaignName() != null) {
            context.setVendorCampaignName(campaign.getCampaignName());
        }
    }
}
