package com.nextgen.gameaggregator.vendor.evoplay.api.v2.freeround;

import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutResult;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import org.springframework.stereotype.Component;

@Component
public class EvoplayV2FreeRoundResponseAdapter {

    public ResponseVo toV2(EvoplayFreeRoundPayoutResult result) {
        ResponseDataVo data = new ResponseDataVo();
        data.setBalance(result.getBalance());
        data.setCurrency(result.getCurrency());

        ResponseVo response = new ResponseVo();
        response.setData(data);
        return response;
    }
}
