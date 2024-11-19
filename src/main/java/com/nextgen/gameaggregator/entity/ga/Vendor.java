package com.nextgen.gameaggregator.entity.ga;

import lombok.Data;

import jakarta.persistence.*;

@Entity
@Table(name = "vendors")
@Data
public class Vendor {
    @Id
    private Integer id;
    private String code;
    private String name;
    private String className;
    @Column(name = "is_support_seamless")
    private Integer isSupportSeamless;
    @Column(name = "is_support_transfer")
    private Integer isSupportTransfer;
    private Integer status;
    private Integer productId;

    // TODO : Will re-enable after the new game list import is deployed
    // @ManyToOne(fetch = FetchType.EAGER, optional = false)
    // @JoinColumn(name = "product_id", nullable = false, insertable = false, updatable = false)
    // private Product product;
}
