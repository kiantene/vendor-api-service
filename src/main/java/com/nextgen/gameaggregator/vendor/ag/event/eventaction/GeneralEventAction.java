package com.nextgen.gameaggregator.vendor.ag.event.eventaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.ag.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ag.event.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ag.event.eventdto.CommonEventDto;
import com.nextgen.gameaggregator.vendor.ag.event.eventrollback.EventRollBackService;
import com.nextgen.gameaggregator.vendor.ag.event.eventwithdraw.EventWithdrawService;
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
public class GeneralEventAction {

    private final HttpService httpService;
    private final EventRollBackService eventRollBackService;
    private final VendorService vendorService;
    private final EventWithdrawService eventWithdrawService;

    @Autowired
    public GeneralEventAction(HttpService httpService, EventRollBackService eventRollBackService, VendorService vendorService,
                              EventWithdrawService eventWithdrawService) {
        this.httpService = httpService;
        this.eventRollBackService = eventRollBackService;
        this.vendorService = vendorService;
        this.eventWithdrawService = eventWithdrawService;

    }

    @PostMapping(path = EndPoints.POST_SLOT_EVENT)
    public ResponseEntity<String> action(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        String xml = null;
        CommonEventDto commonEventDto;

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            commonEventDto = xmlMapper.readValue(body, CommonEventDto.class);

            // Handle the action and return the resulting value
            vo = this.actionHandling(commonEventDto, traceId, httpRequestLog);


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

    private CommonVo actionHandling(CommonEventDto commonEventDto, String traceId, HttpRequestLog httpRequestLog) throws
            InvalidRequestException {


        return switch (commonEventDto.getEventDto().getTransactionType().toUpperCase()) {
            case "WITHDRAW" -> eventWithdrawService.withdraw(httpRequestLog, traceId);
            case "ROLLBACK" -> eventRollBackService.rollback(httpRequestLog, traceId);
            default -> throw new InvalidRequestException();
        };
    }
}

