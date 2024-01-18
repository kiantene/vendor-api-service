package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "agent_api_credentials")
@Data
public class AgentApiCredential {
    @Id
    private Integer id;
    private String callbackUrl;
    private String algorithm;
    private String apiKey;
    private String apiSecret;
    private Integer status;

    @ManyToOne
    private Agent agent;
}
