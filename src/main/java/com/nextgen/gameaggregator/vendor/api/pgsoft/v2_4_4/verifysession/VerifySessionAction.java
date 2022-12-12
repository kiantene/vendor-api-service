package com.nextgen.gameaggregator.vendor.api.pgsoft.v2_4_4.verifysession;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.Constant;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

@RestController
@RequestScope
@RequestMapping(path = Constant.WEB_ACTION)
public class VerifySessionAction {

    @PostMapping(path = Constant.ACTION_VERIFY_SESSION)
    public VerifySessionActionVo test(VerifySessionActionDto dto, WebRequestWrapper request) {
        VerifySessionActionVo verifySessionActionVo = new VerifySessionActionVo();

        //* hardcoded response
        verifySessionActionVo.setPlayerName("alex19");
        verifySessionActionVo.setCurrency("CNY");
        return verifySessionActionVo;
    }

}