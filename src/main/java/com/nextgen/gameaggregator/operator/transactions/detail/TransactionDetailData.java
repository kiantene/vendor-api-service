package com.nextgen.gameaggregator.operator.transactions.detail;

import com.nextgen.gameaggregator.entity.custom.IBetDetail;
import lombok.Data;

@Data
public class TransactionDetailData {

    private String detailUrl;
    private IBetDetail betDetail;
}
