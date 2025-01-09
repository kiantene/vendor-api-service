package com.nextgen.gameaggregator.vendor.ag.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.ag.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.ag.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.ag.api.endround.SettleService;
import com.nextgen.gameaggregator.vendor.ag.api.rollback.RollBackService;
import com.nextgen.gameaggregator.vendor.ag.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ag.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ag.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.ag.service.VendorService;
import com.nextgen.gameaggregator.vendor.ag.vo.CommonVo;
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
    private final BalanceService balanceService;
    private final BetService betService;
    private final SettleService settleService;
    private final RollBackService rollBackService;
    private final VendorService vendorService;

    @Autowired
    public GeneralAction(HttpService httpService, BalanceService balanceService, BetService betService,
                         SettleService settleService, RollBackService rollBackService, VendorService vendorService) {
        this.httpService = httpService;
        this.balanceService = balanceService;
        this.betService = betService;
        this.settleService = settleService;
        this.rollBackService = rollBackService;
        this.vendorService = vendorService;

    }

    @PostMapping(path = EndPoints.POST_SLOT_GAME)
    public ResponseEntity<String> action(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        String xml = null;
        CommonDto dto;

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            dto = xmlMapper.readValue(body, CommonDto.class);

            // Handle the action and return the resulting value
            vo = this.actionHandling(dto, traceId, httpRequestLog);


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

    private CommonVo actionHandling(CommonDto commonDto, String traceId, HttpRequestLog httpRequestLog) throws
            InvalidRequestException {


        return switch (commonDto.getRecordDto().getTransactionType().toUpperCase()) {
            case "BALANCE" -> balanceService.balance(httpRequestLog, traceId);
            case "WITHDRAW" -> betService.bet(httpRequestLog, traceId);
            case "DEPOSIT" -> settleService.settle(httpRequestLog, traceId);
            case "ROLLBACK" -> rollBackService.rollback(httpRequestLog, traceId);
            default -> throw new InvalidRequestException();
        };
    }
}

