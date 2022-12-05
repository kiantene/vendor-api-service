package com.nextgen.gameaggregator.vendor.data.mariadb.writer.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vendor_players")
@SQLDelete(sql = "UPDATE vendor_players SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")
@Data
public class VendorPlayerWriter extends CommonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_player_id", nullable = false)
    private Long agentPlayerId;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "vendor_credential_id", nullable = false)
    private Long vendorCredentialId;

    @Column(name = "credentials_version", nullable = false)
    private Long credentialsVersion;

    @Column(name = "vendor_username", nullable = false,  length = 50)
    private String vendorUsername;

    @Column(name = "password", nullable = true,  length = 50)
    private String password;

    @Column(name = "balance", nullable = false,  length = 50)
    private BigDecimal balance;

    @Column(name = "currency_code", nullable = false,  length = 50)
    private String currencyCode;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "master_agent_id", nullable = false)
    private Long masterAgentId;

    @Column(name = "house_id", nullable = false)
    private Long houseId;

}
