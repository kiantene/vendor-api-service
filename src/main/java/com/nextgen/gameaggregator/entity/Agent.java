package com.nextgen.gameaggregator.entity;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "agents")
@Data
public class Agent {
    @Id
    private Integer id;
    private Integer sasEntityId;
    private Integer masterAgentId;
    private Integer houseId;

    private Integer walletType;

    @ManyToOne
    private Currency currency;
}
