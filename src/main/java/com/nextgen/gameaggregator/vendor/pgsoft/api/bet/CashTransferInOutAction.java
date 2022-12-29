package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

@RestController
@RequestScope
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
public class CashTransferInOutAction {

}
