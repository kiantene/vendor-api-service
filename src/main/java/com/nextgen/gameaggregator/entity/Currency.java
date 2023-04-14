package com.nextgen.gameaggregator.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "currencies")
@Data
public class Currency {

    @Id
    private Integer id;
    private String code;
    private String name;
    private Integer status;
}
