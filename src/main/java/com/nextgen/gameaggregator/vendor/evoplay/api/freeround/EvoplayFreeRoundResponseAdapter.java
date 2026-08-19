package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseVo;
import org.springframework.stereotype.Component;

@Component
public class EvoplayFreeRoundResponseAdapter {

    public ResponseVo toV1(EvoplayFreeRoundPayoutResult result) {
        ResponseDataVo data = new ResponseDataVo();
        data.setBalance(result.getBalance());
        data.setCurrency(result.getCurrency());

        ResponseVo response = new ResponseVo();
        response.setData(data);
        return response;
    }
}
