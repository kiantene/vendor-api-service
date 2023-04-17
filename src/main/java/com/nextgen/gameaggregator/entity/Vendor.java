package com.nextgen.gameaggregator.entity;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
}
