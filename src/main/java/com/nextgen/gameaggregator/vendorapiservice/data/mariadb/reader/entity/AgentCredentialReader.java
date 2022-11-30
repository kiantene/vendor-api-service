package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "agent_credentials")
@SQLDelete(sql = "UPDATE agent_credentials SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")

public class AgentCredentialReader extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "master_agent_id", nullable = false)
    private Long masterAgentId;

    @Column(name = "house_id", nullable = false)
    private Long houseId;

    @Column(name = "agent_api_url", nullable = false)
    private String agentApiUrl;

    @Column(name = "algorithm", nullable = false)
    private String algorithm;

    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getMasterAgentId() {
        return masterAgentId;
    }

    public void setMasterAgentId(Long masterAgentId) {
        this.masterAgentId = masterAgentId;
    }

    public Long getHouseId() {
        return houseId;
    }

    public void setHouseId(Long houseId) {
        this.houseId = houseId;
    }

    public String getAgentApiUrl() {
        return agentApiUrl;
    }

    public void setAgentApiUrl(String agentApiUrl) {
        this.agentApiUrl = agentApiUrl;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
