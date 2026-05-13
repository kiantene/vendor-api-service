package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "maintenance_game")
public class MaintenanceGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "vendor_game_id")
    private Integer vendorGameId;

    @Column(name = "status")
    private Integer status;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "maintenance_group_id", nullable = false, insertable = false, updatable = false)
    private MaintenanceGroup maintenanceGroup;
}
