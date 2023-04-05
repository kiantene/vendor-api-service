package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "vendor_language_codes")
@Data
public class VendorLanguageCode {
    @Id
    private Integer id;
    private Integer vendorId;
//    private Integer languageId;
    private String languageCode;
    private Integer status;

    @ManyToOne
    private Language language;
}
