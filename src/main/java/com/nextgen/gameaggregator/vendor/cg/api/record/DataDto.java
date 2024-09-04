package com.nextgen.gameaggregator.vendor.cg.api.record;

import lombok.Data;

@Data
public class DataDto {

    String transaction_id;
    String action;
    Target target;
    Balance balance;
    Status status;
    String currency;
    Incident incident;


}
