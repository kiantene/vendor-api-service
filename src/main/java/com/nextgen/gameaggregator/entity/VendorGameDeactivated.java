package com.nextgen.gameaggregator.entity;
import lombok.Data;

import jakarta.persistence.*;

@Entity
@Table(name = "vendor_game_deactivated")
@Data
public class VendorGameDeactivated extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer sasEntityId;
    private Integer sasEntityHierarchy;
    private Integer vendorGameId;
    private Integer isDeleted;
    private Integer agentId;
    private Integer masterAgentId;
    private Integer houseId;

}
