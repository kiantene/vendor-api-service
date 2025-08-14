package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "agent_api_versions")
@Data
public class AgentApiVersion {
    @Id
    private Integer agentId;
    private Integer apiVersion;

}
