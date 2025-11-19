package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import lombok.Data;

@Data
public class AgentMeta {
    @JsonProperty("agentId")
    private Integer agentId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("gameCode")
    private String gameCode;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("session")
    private String session;

    @JsonIgnore
    public static AgentMeta of(BetResultContext context, String sessionToken) {
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(context.getAgentId());
        agentMeta.setUsername(context.getAgentPlayerUsername());
        agentMeta.setCurrency(context.getCurrencyCode());
        agentMeta.setGameCode(context.getGameCode());
        agentMeta.setSession(sessionToken);

        return agentMeta;
    }

    @JsonIgnore
    public static AgentMeta ofGameSession(GameSession gameSession) {
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(gameSession.getAgentId());
        agentMeta.setUsername(gameSession.getAgentPlayerUsername());
        agentMeta.setCurrency(gameSession.getCurrencyCode());
        agentMeta.setGameCode(gameSession.getGameCode());
        agentMeta.setSession(gameSession.getToken());

        return agentMeta;
    }
}
