package com.nextgen.gameaggregator.entity.wallet;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "access_keys")
@Data
public class AccessKey {

    @Id
    private Integer id;
    private String apiKey;
    private String apiSecret;
}
