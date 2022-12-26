package com.nextgen.gameaggregator.entity;

import javax.persistence.*;
import lombok.Data;

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
