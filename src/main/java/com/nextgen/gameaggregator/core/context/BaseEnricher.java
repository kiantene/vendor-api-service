package com.nextgen.gameaggregator.core.context;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.entity.AgentPlayer;
import com.nextgen.gameaggregator.core.entity.VendorGame;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;

public abstract class BaseEnricher<T> {
    private final AgentPlayerDataService agentPlayerDataService;
    private final VendorPlayerDataService vendorPlayerDataService;
    private final VendorGameDataService vendorGameDataService;

    protected BaseEnricher(AgentPlayerDataService agentPlayerDataService,
                           VendorPlayerDataService vendorPlayerDataService,
                           VendorGameDataService vendorGameDataService) {

        this.agentPlayerDataService = agentPlayerDataService;
        this.vendorPlayerDataService = vendorPlayerDataService;
        this.vendorGameDataService = vendorGameDataService;
    }

    public final void enrich(T target) {
        prepareContext(target);
        enrichVendorPlayer(target);
        enrichVendorGame(target);
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
}
