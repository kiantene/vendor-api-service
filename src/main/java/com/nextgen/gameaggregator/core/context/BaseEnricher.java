package com.nextgen.gameaggregator.core.context;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.entity.*;
import com.nextgen.gameaggregator.core.service.*;

public abstract class BaseEnricher<T> {
    private final AgentPlayerDataService agentPlayerDataService;
    private final VendorPlayerDataService vendorPlayerDataService;
    private final VendorGameDataService vendorGameDataService;
    private final CurrencyDataService currencyDataService;
    private final VendorCurrencyDataService vendorCurrencyDataService;

    protected BaseEnricher(AgentPlayerDataService agentPlayerDataService,
                           VendorPlayerDataService vendorPlayerDataService,
                           VendorGameDataService vendorGameDataService,
                           CurrencyDataService currencyDataService,
                           VendorCurrencyDataService vendorCurrencyDataService) {

        this.agentPlayerDataService = agentPlayerDataService;
        this.vendorPlayerDataService = vendorPlayerDataService;
        this.vendorGameDataService = vendorGameDataService;
        this.currencyDataService = currencyDataService;
        this.vendorCurrencyDataService = vendorCurrencyDataService;
    }

    public final void enrich(T target) {
        prepareContext(target);
        enrichVendorPlayer(target);
        enrichVendorGame(target);
        enrichVendorCurrency(target);
        doEnrich(target); // subclass-specific logic
    }

    protected void prepareContext(T target) {
        // override for logic before doEnrich
    }

    protected abstract void doEnrich(T target);

    private void enrichVendorPlayer(T target) {
        if (target instanceof VendorPlayerAware context && context.getVendorPlayerUsername() != null) {
            try {
                VendorPlayer vendorPlayer = vendorPlayerDataService.getByUsername(context.getVendorPlayerUsername());
                context.setAgentPlayerId(vendorPlayer.getAgentPlayerId());
                context.setVendorPlayerId(vendorPlayer.getId());
                context.setVendorId(vendorPlayer.getVendorId());
                context.setVendorLineId(vendorPlayer.getVendorLineId());
                context.setCurrencyId(vendorPlayer.getCurrencyId());

                AgentPlayer agentPlayer = agentPlayerDataService.get(vendorPlayer.getAgentPlayerId());
                context.setAgentPlayerUsername(agentPlayer.getUsername());
                context.setAgentId(agentPlayer.getAgentId());

            } catch (EntityNotFoundException ex) {
                throw new InternalConfigurationException(ex);
            }
        }
    }

    private void enrichVendorGame(T target) {
        if (target instanceof VendorGameAware context && context.getVendorGameCode() != null) {
            try {
                VendorGame vendorGame = vendorGameDataService.getByVendorGameCodeAndVendorId(
                        context.getVendorGameCode(),
                        context.getVendorId()
                );
                context.setVendorGameId(vendorGame.getId());
                context.setGameCode(vendorGame.getCode());
                context.setGameName(vendorGame.getName());
                context.setGameCategoryId(vendorGame.getGameCategoryId());

            } catch (EntityNotFoundException e) {
                throw new InternalConfigurationException(e.getMessage());
            }
        }
    }

    private void enrichVendorCurrency(T target) {
        if (target instanceof VendorCurrencyAware context && context.getVendorId() != null && context.getCurrencyId() != null) {
            try {
                Currency currency = currencyDataService.get(context.getCurrencyId());

                VendorCurrency vendorCurrency = vendorCurrencyDataService.getVendorIdAndCurrencyId(
                        context.getVendorId(),
                        context.getCurrencyId()
                );
                context.setCurrencyCode(currency.getCode());
                if (context.getVendorCurrency() == null) {
                    context.setVendorCurrency(vendorCurrency.getVendorCurrencyCode());
                }
                context.setFromVendorRate(vendorCurrency.getFromVendorRate());
                context.setToVendorRate(vendorCurrency.getToVendorRate());

            } catch (EntityNotFoundException e) {
                throw new InternalConfigurationException(e.getMessage());
            }
        }
    }
}
