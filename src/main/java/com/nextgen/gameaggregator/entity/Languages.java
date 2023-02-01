package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "languages")
@Data
public class Languages {
    @Id
    private Integer id;
    private String code;
    private String name;
    private Integer status;
}
