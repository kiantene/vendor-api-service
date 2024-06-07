package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    private Integer walletType;

    private Integer status;

    private Integer  seamlessType;

}
