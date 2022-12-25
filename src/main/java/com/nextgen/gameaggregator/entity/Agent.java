package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "agents")
@Data
public class Agent {
    @Id
    private Integer id;
    private Integer sasEntityId;
    private Integer masterAgentId;
    private Integer houseId;

    @ManyToOne
    private Currency currency;
}
