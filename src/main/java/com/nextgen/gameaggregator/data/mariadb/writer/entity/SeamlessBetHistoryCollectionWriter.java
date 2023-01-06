package com.nextgen.gameaggregator.data.mariadb.writer.entity;

import com.nextgen.sas.core.db.bean.CommonEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "seamless_bet_history_collections")
@SQLDelete(sql = "UPDATE seamless_bet_history_collections SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted=false")
public class SeamlessBetHistoryCollectionWriter extends CommonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bet_history_id", nullable = false)
    private String betHistoryId;

    @Column(name = "vendor_bet_id")
    private String vendorBetId;

    @Column(name = "vendor_round_id")
    private String vendorRoundId;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBetHistoryId() {
        return betHistoryId;
    }

    public void setBetHistoryId(String betHistoryId) {
        this.betHistoryId = betHistoryId;
    }

    public String getVendorBetId() {
        return vendorBetId;
    }

    public void setVendorBetId(String vendorBetId) {
        this.vendorBetId = vendorBetId;
    }

    public String getVendorRoundId() {
        return vendorRoundId;
    }

    public void setVendorRoundId(String vendorRoundId) {
        this.vendorRoundId = vendorRoundId;
    }
}
