package com.nextgen.gameaggregator.entity;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
