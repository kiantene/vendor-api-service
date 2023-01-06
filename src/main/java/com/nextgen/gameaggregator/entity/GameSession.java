package com.nextgen.gameaggregator.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "game_sessions")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GameSession extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String token;
    private Integer agentId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private String vendorPlayerUsername;
    private Long vendorPlayerId;
    private Integer vendorId;
    private Integer vendorLineId;
    private Integer vendorGameId;
    private String vendorGameCode;
    private Integer gameCategoryId;
    private String language;
    private Integer currencyId;
    private String currencyCode;
    private String traceId;
    private Integer status;
}
