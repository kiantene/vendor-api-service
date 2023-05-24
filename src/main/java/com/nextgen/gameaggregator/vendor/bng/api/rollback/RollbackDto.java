package com.nextgen.gameaggregator.vendor.bng.api.rollback;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import lombok.Data;

@Data
public class RollbackDto implements RollbackData {
    private String name;
    private String uid;
    private String token;
    private String session;
    private String game_id;
    private String game_name;
    private String provider_id;
    private String provider_name;
    private String c_at;
    private String sent_at;
    private RollbackArgsDto args;

    @Override
    public String getRollbackId() {
        return this.getArgs().getTransaction_uid();
    }

    @Override
    public Long getVendorSettledTime() {
        long timestampMillis = System.currentTimeMillis();

        return timestampMillis;
    }
}
