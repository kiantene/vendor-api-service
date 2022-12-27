package com.nextgen.gameaggregator.entity;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vendors")
@Data
public class Vendor {
    @Id
    private Integer id;
    private String code;
    private String name;
    private Integer status;
}
