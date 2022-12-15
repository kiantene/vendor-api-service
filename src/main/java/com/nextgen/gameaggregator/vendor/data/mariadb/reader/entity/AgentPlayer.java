package com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "agent_players")
@SQLDelete(sql = "UPDATE agent_players SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")
@Data
public class AgentPlayer extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "username", nullable = false,  length = 50)
    private String username;

    @Column(name = "password", nullable = false,  length = 50)
    private String password;

    @Column(name = "master_agent_id", nullable = false)
    private Long masterAgentId;

    @Column(name = "house_id", nullable = false)
    private Long houseId;

    @Column(name = "status", nullable = false)
    private Boolean status;
}
