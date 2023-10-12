package com.nextgen.gameaggregator.vendor.ifg.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.ifg.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.ifg.api.bet.TransactionService;
import com.nextgen.gameaggregator.vendor.ifg.api.endround.CreditService;
import com.nextgen.gameaggregator.vendor.ifg.api.login.LoginService;
import com.nextgen.gameaggregator.vendor.ifg.api.rollback.RollBackService;
import com.nextgen.gameaggregator.vendor.ifg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ifg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ifg.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.ifg.service.VendorService;
import com.nextgen.gameaggregator.vendor.ifg.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
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
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private LoginService loginService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private CreditService creditService;
    @Autowired
    private RollBackService rollBackService;

    @PostMapping(path = EndPoints.ACTION)
    public ResponseEntity<String> action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        Integer httpStatus = HttpStatus.SC_OK; //default is 200 status
        ErrorVo errorVo = new ErrorVo();
        String xml = null;
        CommonDto dto = new CommonDto();

        try{
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            dto = xmlMapper.readValue(body, CommonDto.class);

            // Handle the action and return the resulting value
            vo = this.actionHandling(dto, traceId, httpRequestLog);

        } catch (JsonProcessingException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set vo
            vo.setTime(dto.getTime());
            vo.setSession(dto.getSession());
            vo.setError(errorVo);
        }
        finally{
            httpService.end(httpRequestLog, vo);

            try{
                // Serialize the object to XML in string format
                xml = xmlMapper.writeValueAsString(vo);
            }catch (JsonProcessingException e) {
                xml = null;
            }
        }

        return new ResponseEntity<>(xml, HttpStatusCode.valueOf(httpStatus));
    }

    private CommonVo actionHandling(CommonDto dto, String traceId, HttpRequestLog httpRequestLog){
        CommonVo vo = new CommonVo();

        // login
        if(dto.getEnter() != null){
            vo = loginService.login(httpRequestLog, traceId);
        }

        // get balance
        if(dto.getGetbalance() != null){
            vo = balanceService.balance(httpRequestLog, traceId);
        }

        // place bet
        if(dto.getRoundbet() != null){
            vo = transactionService.transaction(httpRequestLog, traceId);
        }

        // settle bet
        if(dto.getRoundwin() != null){
            vo = creditService.credit(httpRequestLog, traceId);
        }

        // refund
        if(dto.getRefund() != null){
            vo = rollBackService.rollback(httpRequestLog, traceId);
        }

        return vo;
    }

    // Utility method to check if a field (property) exists in an object
    private static boolean hasField(Object obj, String fieldName) {
        try {
            obj.getClass().getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
