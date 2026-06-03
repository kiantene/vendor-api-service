package com.nextgen.gameaggregator.vendor.evolutionlive.api.authenticate;

import com.nextgen.gameaggregator.vendor.evolutionlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evolutionlive.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class CheckController {

    private final CheckActionHandler handler;

    public CheckController(CheckActionHandler handler) {
        this.handler = handler;
    }

    @PostMapping(path = EndPoints.CHECK)
    public ResponseVo check(HttpServletRequest request) {
        return handler.handleAction(request);
    }
}