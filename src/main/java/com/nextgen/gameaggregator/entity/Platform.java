package com.nextgen.gameaggregator.entity;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "platforms")
@Data
public class Platform {
    @Id
    private Integer id;
    private String code;
    private String name;
    private Integer status;
}
