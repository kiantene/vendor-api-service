package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;

@Entity
@Table(name = "vendor_players")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VendorPlayer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private Long agentPlayerId;
    private Integer vendorId;
    private Integer vendorLineId;
    private Integer status;
    private Integer currencyId;
}
