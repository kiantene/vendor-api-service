package com.nextgen.gameaggregator.core.context;

import com.nextgen.gameaggregator.core.entity.AgentPlayer;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;

public abstract class BaseEnricher<T> {
    private final AgentPlayerDataService agentPlayerDataService;
    private final VendorPlayerDataService vendorPlayerDataService;

    protected BaseEnricher(AgentPlayerDataService agentPlayerDataService,
                           VendorPlayerDataService vendorPlayerDataService) {

        this.agentPlayerDataService = agentPlayerDataService;
        this.vendorPlayerDataService = vendorPlayerDataService;
    }

    public final void enrich(T target) {
        prepareContext(target);
        enrichVendorPlayer(target);
        doEnrich(target); // subclass-specific logic
    }

    protected void prepareContext(T target) {
        // override for logic before doEnrich
    }

    protected abstract void doEnrich(T target);

    private void enrichVendorPlayer(T target) {
        if (target instanceof VendorPlayerAware contextWithUsername && contextWithUsername.getVendorPlayerUsername() != null) {
            try {
                VendorPlayer vendorPlayer = vendorPlayerDataService.getByUsername(contextWithUsername.getVendorPlayerUsername());
                contextWithUsername.setAgentPlayerId(vendorPlayer.getAgentPlayerId());
                contextWithUsername.setVendorPlayerId(vendorPlayer.getId());
                contextWithUsername.setVendorId(vendorPlayer.getVendorId());
                contextWithUsername.setVendorLineId(vendorPlayer.getVendorLineId());
                contextWithUsername.setCurrencyId(vendorPlayer.getCurrencyId());

                AgentPlayer agentPlayer = agentPlayerDataService.get(vendorPlayer.getAgentPlayerId());
                contextWithUsername.setAgentPlayerUsername(agentPlayer.getUsername());
                contextWithUsername.setAgentId(agentPlayer.getAgentId());

            } catch (Exception ex) {
                // TODO: handle exception
            }
        }
    }
}
