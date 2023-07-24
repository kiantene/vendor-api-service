package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.playngo.constant.EndPoints;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ReserveAction {

    @Autowired
    private HttpService httpService;

    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = EndPoints.RESERVE)
    public String reserve(HttpServletRequest request) throws InvalidRequestException, JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        ReserveVo reserveVo = new ReserveVo();
        reserveVo.setReal("1000.00");
        reserveVo.setStatusCode("0");

        //Retrieve request body in original string format
        String body = httpRequestLog.getRequestBody();

        //Convert original request body into commonDto
        XmlMapper xmlMapper = new XmlMapper();
        //BalanceDto authDto = xmlMapper.readValue(body, BalanceDto.class);

        //Validate request parameters from vendor (Non-database related)
        //this.doValidation(authDto);

        String authVoXml = xmlMapper.writeValueAsString(reserveVo);
        httpService.end(httpRequestLog, reserveVo);
        return authVoXml;
    }

}
