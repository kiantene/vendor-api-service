package com.nextgen.gameaggregator.vendor.megafair.api.betresult;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.vendor.megafair.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.megafair.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.megafair.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.megafair.api.result.BetResultService;
import com.nextgen.gameaggregator.vendor.megafair.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.megafair.request.CommonRequest;
import com.nextgen.gameaggregator.vendor.megafair.response.CommonResponse;
import com.nextgen.gameaggregator.vendor.megafair.util.VendorUtil;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultController {

    private final BetService betService;
    private final BetResultService betResultService;
    private final VendorUtil vendorUtil;

    protected BetResultController(BetService betService,
                                  BetResultService betResultService,
                                  VendorUtil vendorUtil) {
        this.betService = betService;
        this.betResultService = betResultService;
        this.vendorUtil = vendorUtil;
    }

    @PostMapping(path = EndPoints.BETRESULT, produces = MediaType.APPLICATION_XML_VALUE)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> bet(@Valid @ModelAttribute CommonRequest commonRequest) {
        vendorUtil.parseFormattedFields(commonRequest);
        if (isWinRequest(commonRequest)) {
            BetResultRequest request = vendorUtil.convertDto(commonRequest, BetResultRequest.class);
            CommonResponse response = betResultService.result(request);
            return ResponseEntity.ok(enrichResponse(response, commonRequest));
        } else {
            BetRequest request = vendorUtil.convertDto(commonRequest, BetRequest.class);
            CommonResponse response = betService.bet(request);
            return ResponseEntity.ok(enrichResponse(response, commonRequest));
        }
    }

    private boolean isWinRequest(CommonRequest request) {
        return request.getWin() != null;
    }

    private CommonResponse enrichResponse(CommonResponse response, CommonRequest request) {
        if (response.getRequest() == null) {
            response.setRequest(CommonResponse.RequestData.builder().build());
        }

        CommonResponse.RequestData requestData = response.getRequest();

        requestData.setUserId(request.getUserId());
        requestData.setBet(request.getBet());
        requestData.setWin(request.getWin());
        requestData.setRoundId(request.getRoundId());
        requestData.setGameId(request.getGameId());
        requestData.setGameSessionId(request.getGameSessionId());
        requestData.setKind(request.getKind());
        requestData.setHash(request.getHash());
        requestData.setIsRoundFinished(request.getIsRoundFinished());

        return response;
    }
}
