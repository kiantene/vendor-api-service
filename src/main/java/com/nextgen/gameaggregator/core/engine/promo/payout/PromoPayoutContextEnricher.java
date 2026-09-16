package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignResolveStrategy;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.core.service.data.CampaignDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class PromoPayoutContextEnricher extends BaseEnricher<PromoPayoutContext> {
    private final VendorDataService vendorDataService;
    private final AgentDataService agentDataService;
    private final CampaignDataService campaignDataService;
    private final VendorGameDataService vendorGameDataService;
    private final GameSessionDataService gameSessionDataService;

    public PromoPayoutContextEnricher(AgentPlayerDataService agentPlayerDataService,
                                      VendorPlayerDataService vendorPlayerDataService,
                                      VendorGameDataService vendorGameDataService,
                                      CurrencyDataService currencyDataService,
                                      VendorCurrencyDataService vendorCurrencyDataService,
                                      VendorDataService vendorDataService,
                                      AgentDataService agentDataService,
                                      CampaignDataService campaignDataService,
                                      GameSessionDataService gameSessionDataService) {

        super(agentPlayerDataService, vendorPlayerDataService, vendorGameDataService, currencyDataService, vendorCurrencyDataService);
        this.vendorDataService = vendorDataService;
        this.agentDataService = agentDataService;
        this.campaignDataService = campaignDataService;
        this.vendorGameDataService = vendorGameDataService;
        this.gameSessionDataService = gameSessionDataService;
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
        this.populateGame(context);

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
        PromoPayoutConfig config = PromoPayoutContextHolder.getConfig();
        // Skip when there's nothing to resolve: no strategy configured and no campaign code to look up by.
        if (config.getCampaignResolveStrategy() == null && Objects.isNull(context.getVendorCampaignCode())) {
            return;
        }

        Campaign campaign = resolveCampaign(context, config);

        if (campaign.getUuid() != null) {
            context.setCampaignUuid(campaign.getUuid());
        }
        if (campaign.getCampaignName() != null) {
            context.setVendorCampaignName(campaign.getCampaignName());
        }
    }
    private void populateGame(PromoPayoutContext context) {
        String vendorGameCode = context.getVendorGameCode();
        Integer vendorId = context.getVendor().id();
        if (vendorGameCode == null || vendorGameCode.isBlank() || vendorId == null) {
            return;                                  // gameCode stays null → omitted from the DTO
        }
        try {
            context.setGameCode(vendorGameDataService
                    .getByVendorGameCodeAndVendorId(vendorGameCode, vendorId).getCode());
        } catch (EntityNotFoundException e) {
            log.warn("[PROMO_GAME_UNRESOLVED] traceId={} vendorId={} vendorGameCode={}",
                    context.getTraceId(), vendorId, vendorGameCode);
        }
        verifyAgainstSession(context, vendorGameCode);
    }

    /**
     * Reconciles the request's game against the launched session. Diagnostic only - the request's game
     * is what reaches the operator either way, because a promo payout legitimately settles outside the
     * current session: a tournament prize paid hours after play, or a vendor lobby game switch.
     *
     * <p>A mismatch is therefore not automatically an error. Some vendors launch from a lobby and leave
     * a placeholder code on the session (see the PGSoft promo mapper), which warns on every payout for
     * those players - check the vendor's mapper before treating this tag as an alert.
     */
    private void verifyAgainstSession(PromoPayoutContext context, String vendorGameCode) {
        try {
            GameSession session = gameSessionDataService.getGameSession(context);
            if (!vendorGameCode.equals(session.getVendorGameCode())) {
                log.warn("[PROMO_GAME_MISMATCH] traceId={} session={} request={} - forwarding request's game",
                        context.getTraceId(), session.getVendorGameCode(), vendorGameCode);
            }
        } catch (Exception e) {
            log.debug("[PROMO_GAME_CHECK] no session to reconcile, traceId={}", context.getTraceId());
        }
    }

    private Campaign resolveCampaign(PromoPayoutContext context, PromoPayoutConfig config) {
        CampaignResolveStrategy strategy = config.getCampaignResolveStrategy();
        // strategy takes precedence over playerUuidCampaignLookup
        if (strategy != null) {
            return campaignDataService.getByRef(strategy, buildResolveParams(context, strategy));
        }
        if (config.isPlayerUuidCampaignLookup()) {
            return campaignDataService.getByPlayerUuid(context.getVendorCampaignCode());
        }
        return campaignDataService.get(context.getVendorCampaignCode(), context.getVendor().lineId(),
                Optional.ofNullable(context.getPromoType()).map(type -> type.id).orElse(null));
    }

    private Map<String, String> buildResolveParams(PromoPayoutContext context, CampaignResolveStrategy strategy) {
        return switch (strategy) {
            case USERNAME_AND_BONUS_ID -> {
                String username = context.getVendorPlayerUsername();
                String freeRoundBonusId = context.getVendorFreeRoundBonusId();
                if (username == null || freeRoundBonusId == null) {
                    throw new IllegalStateException("username and vendorFreeRoundBonusId are required for USERNAME_AND_BONUS_ID");
                }
                yield Map.of("username", username, "freeRoundBonusId", freeRoundBonusId);
            }
            case PLAYER_UUID -> {
                // vendorCampaignCode carries the playerUuid for this strategy
                String playerUuid = context.getVendorCampaignCode();
                if (playerUuid == null) {
                    throw new IllegalStateException("vendorCampaignCode (playerUuid) is required for PLAYER_UUID strategy");
                }
                yield Map.of("playerUuid", playerUuid);
            }
            case VENDOR_LINE_AND_CODE -> {
                String campaignCode = context.getVendorCampaignCode();
                if (campaignCode == null) {
                    throw new IllegalStateException("vendorCampaignCode is required for VENDOR_LINE_AND_CODE strategy");
                }
                Map<String, String> params = new HashMap<>();
                params.put("vendorLineId", String.valueOf(context.getVendor().lineId()));
                params.put("vendorCampaignCode", campaignCode);
                if (context.getPromoType() != null) {
                    params.put("campaignType", String.valueOf(context.getPromoType().id));
                }
                yield params;
            }
        };
    }
}
