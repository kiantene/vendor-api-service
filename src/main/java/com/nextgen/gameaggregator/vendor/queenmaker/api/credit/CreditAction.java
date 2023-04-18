package com.nextgen.gameaggregator.vendor.queenmaker.api.credit;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CreditAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.WALLET_CREDIT)
    public CreditVo CreditAction (HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        CreditVo creditVo = new CreditVo();
        String traceId = httpRequestLog.getTraceId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CreditDto creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            List<TransactionsVo> transactionsList = new LinkedList<>();

            for(TransactionsDto transaction : creditDto.getTransactions()) {
                TransactionsVo transactionsVo = new TransactionsVo();
                if(true){
                    transactionsVo.setTxid(traceId);
                    transactionsVo.setPtxid(transaction.getPtxid());
                    transactionsVo.setCur(transaction.getCur());
                    transactionsVo.setBal(BigDecimal.valueOf(100));
                    transactionsVo.setDup(false);
                }else{
                    transactionsVo.setTxid(traceId);
                    transactionsVo.setPtxid(transaction.getPtxid());
                    transactionsVo.setErr(900);
                    transactionsVo.setErrdesc("dasdasdasd");
                }
                transactionsList.add(transactionsVo);
            }
            creditVo.setTransactions(transactionsList);

        }  catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, creditVo);
        }

        return creditVo;
    }

}
