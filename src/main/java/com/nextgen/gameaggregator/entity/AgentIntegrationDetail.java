package com.nextgen.gameaggregator.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "agent_integration_details")
@Data
public class AgentIntegrationDetail extends BaseEntity {
    @Id
    private Integer id;

    private String username;

    private String gameCode;

    private Integer status;
}
