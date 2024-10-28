package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "product_game_deactivated")
public class ProductGameDeactivated {
    @Id
    private Integer id;
    private Integer sasEntityId;
    private Integer sasEntityHierarchyId;
    private Integer productGameId;
    private Integer isDeleted;
    private Integer agentId;
    private Integer masterAgentId;
    private Integer houseId;
}
