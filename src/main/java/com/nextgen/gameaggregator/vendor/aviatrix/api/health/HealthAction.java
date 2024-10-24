package com.nextgen.gameaggregator.vendor.aviatrix.api.health;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatrix.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EndPoints.PATH)
public class HealthAction {

    private final HttpService httpService;

    @Autowired
    public HealthAction(HttpService httpService) {
        this.httpService = httpService;
    }

    //This end point is only to response OK status
    @GetMapping(EndPoints.HEALTH)
    public ResponseEntity<ResponseVo> healthCheck(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();

        httpService.end(httpRequestLog, responseVo);

        return new ResponseEntity<>(responseVo, HttpStatus.OK);
    }
}
