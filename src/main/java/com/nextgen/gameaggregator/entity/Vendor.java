package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "vendors")
@Data
public class Vendor {
    @Id
    private Integer id;
    private String code;
    private String name;
    private String className;
    private Integer status;
}
