package com.nextgen.gameaggregator.entity;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "languages")
@Data
public class Language {
    @Id
    private Integer id;
    private String code;
    private String name;
    private Integer status;
}
