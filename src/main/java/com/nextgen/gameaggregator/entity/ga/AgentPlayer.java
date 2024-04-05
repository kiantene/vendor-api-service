package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;

@Entity
@Table(name = "agent_players")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AgentPlayer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer agentId;
    private String username;
    private Integer status;
}
