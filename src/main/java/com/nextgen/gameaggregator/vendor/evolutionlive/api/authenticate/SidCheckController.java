package com.nextgen.gameaggregator.vendor.evolutionlive.api.authenticate;

import com.nextgen.gameaggregator.vendor.evolutionlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evolutionlive.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
//only testing env is true
@ConditionalOnProperty(name = "is-test-env", havingValue = "true")
public class SidCheckController {

    private final CheckActionHandler handler;

    public SidCheckController(CheckActionHandler handler) {
        this.handler = handler;
    }

    //only testing env is true
    @PostMapping(path = EndPoints.SID)
    public ResponseVo sid(HttpServletRequest request) {
        return handler.handleAction(request);
    }
}