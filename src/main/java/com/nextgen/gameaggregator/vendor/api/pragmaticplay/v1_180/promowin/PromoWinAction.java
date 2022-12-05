package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.promowin;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.action.AbstractAction;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = "api/v1/prammaticplay/", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
public class PromoWinAction extends AbstractAction {

    @PostMapping(path = "promoWin")
    public PromoWinActionVo promoWin(PromoWinActionDto dto, WebRequestWrapper request)
    {
        //* Temporary solution to map into DTO
        dto = this.queryStringToDto(request.getBody(), PromoWinActionDto.class);
        PromoWinActionVo vo = new PromoWinActionVo();

        //* DTO Validation
        Map<String, String> dtoValidationResult = this.doValidation(dto, PromoWinActionDto.class);
        //* Verify validation result
        vo.verifyValidationResultAndManipulateErrorAndDescription(dtoValidationResult);

//        if (!validationResult.isEmpty()) {
//            System.out.println("Validation not passed!");
//            for (Map.Entry<String, String> entry : validationResult.entrySet()) {
//                System.out.println("key: " + entry.getKey());
//                System.out.println("value: " + entry.getValue());
//            }
//        }

        Map<String, String> voValidationResult = this.doValidation(vo, PromoWinActionVo.class);

        return vo;
    }
}
