package com.nextgen.gameaggregator.vendor.aglive.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.aglive.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.aglive.api.endround.SettleService;
import com.nextgen.gameaggregator.vendor.aglive.api.refund.RefundService;
import com.nextgen.gameaggregator.vendor.aglive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.aglive.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    private final HttpService httpService;
    private final BetService betService;
    private final SettleService settleService;
    private final RefundService refundService;
    private final VendorService vendorService;

    @Autowired
    public GeneralAction(HttpService httpService, BetService betService, VendorService vendorService,
                         SettleService settleService, RefundService refundService) {
        this.httpService = httpService;
        this.betService = betService;
        this.vendorService = vendorService;
        this.settleService = settleService;
        this.refundService = refundService;
    }

    @PostMapping(path = EndPoints.POST_LIVE_GAME)
    public ResponseEntity<String> action(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        String xml = null;
        GeneralCommonDto generalCommonDto;

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            generalCommonDto = xmlMapper.readValue(body, GeneralCommonDto.class);

            // Handle the action and return the resulting value
            vo = this.actionHandling(generalCommonDto, traceId, httpRequestLog);

        } catch (JsonProcessingException | InvalidRequestException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_DATA);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            vo.setErrorResponse(ResponseCodes.ERROR);
            httpService.logError(httpRequestLog, e);

        } finally {
            try {
                vo.setXmlResponse(vendorService.generateXmlResponse(vo));
                xml = vo.getXmlResponse();
            } catch (Exception e) {
                vo.setErrorResponse(ResponseCodes.ERROR);
                httpService.logError(httpRequestLog, e);
            } finally {
                httpService.end(httpRequestLog, vo);
            }
        }
        return new ResponseEntity<>(xml, vo.getHeaders(), HttpStatusCode.valueOf(vo.getHttpStatus()));
    }

    private CommonVo actionHandling(GeneralCommonDto generalCommonDto, String traceId, HttpRequestLog httpRequestLog) throws InvalidRequestException {

        return switch (generalCommonDto.getGeneralDto().getTransactionType().toUpperCase()) {
            case "BET" -> betService.bet(httpRequestLog, traceId);
            case "WIN", "LOSE" -> settleService.settle(httpRequestLog, traceId);
            case "REFUND" -> refundService.refund(httpRequestLog, traceId);
            default -> throw new InvalidRequestException();

        };
    }

}
