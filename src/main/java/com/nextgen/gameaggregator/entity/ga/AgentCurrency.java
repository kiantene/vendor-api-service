package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "agent_currencies")
@Data
public class AgentCurrency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "agent_id")
    private Integer agentId;

    @Column(name = "currency_id")
    private Integer currencyId;

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_by_id")
    private Long createById;
    @Column(name = "create_by_usertype")
    private String createByUserType;
    @Column(name = "create_by_ip")
    private String createByIp;
    @Column(name = "create_date")
    private Long createDate;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "currency_id", nullable = false, insertable = false, updatable = false)
    private Currency currency;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "agent_id", nullable = false, insertable = false, updatable = false)
    private Agent agent;
}
