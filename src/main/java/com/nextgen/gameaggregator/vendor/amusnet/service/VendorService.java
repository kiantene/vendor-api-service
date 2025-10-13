package com.nextgen.gameaggregator.vendor.amusnet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.amusnet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.amusnet.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.amusnet.vo.ResponseVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class VendorService extends BaseVendorService {

    public static String validateCredential(String value) throws InvalidVendorLineException {
        return Optional.ofNullable(value)
                .filter(val -> !val.isEmpty()) // Check if the string is non-null and non-empty
                .orElseThrow(InvalidVendorLineException::new); // Set the value using the setter if it is non-null and non-empty
    }

    public void buildResponseVo(ResponseVo vo) {
        String voXml = "";
        try {
            voXml = new XmlMapper().writeValueAsString(vo);
        } catch (JsonProcessingException jsonProcessingException) {
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);
        }
        vo.setResponseXMLFormat(voXml);
    }

    public String checkGameCodeIsOpenEQGame(String categoryCodeList, String vendorGameCode, String portalCodeEQ, String portalCode) {

        // Create an ArrayList to hold the string categories
        List<String> categoryList = new ArrayList<>();

        // Split the categoryCodeList string into individual codes and add them to the list
        String[] gameCodes = categoryCodeList.split(",");

        // Use Collections.addAll to add gameCodes to the category list
        Collections.addAll(categoryList, gameCodes);

        // Check if vendorGameCode is equal to any code in the categoryList
        for (String verifyGameCode : categoryList) {
            if (vendorGameCode.equals(verifyGameCode)) {
                return portalCodeEQ;  // Return "EQ" if a match is found
            }
        }

        // Return the appropriate portal code if no match is found
        return portalCode;  // Or return null, depending on your needs
    }

    public String getPortalCode(GameSession gameSession) {
        String portalCode;
        if (gameSession.getGameCode().contains("EQ")) {
            portalCode = Credentials.PORTAL_CODE_EQ;
        } else {
            portalCode = Credentials.PORTAL_CODE;
        }
        return portalCode;
    }

}
