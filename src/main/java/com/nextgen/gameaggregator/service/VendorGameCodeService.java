package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VendorGameCodeService {

    @Autowired
    private VendorGameCodeRepository vendorGameCodeRepository;

    public VendorGameCode getByVendorGameIdAndPlatformIdAndLanguageId(Integer vendorGameId, Integer platformId, Integer languageId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeRepository.findByVendorGameIdAndPlatformIdAndLanguageIdAndStatus(vendorGameId, platformId, languageId, Status.ACTIVE.code);
        Optional.ofNullable(vendorGameCode).orElseThrow(GameNotSupportedException::new);

        return vendorGameCode;
    }

    public List<VendorGameCode> getByBetGameCodeAndLanguageIdAndPlatformIdAndVendorId(String betGameCode, Integer languageId, Integer platformId, Integer vendorId) throws GameNotSupportedException {
        List<VendorGameCode> vendorGameCodeResultSet = vendorGameCodeRepository.findByBetGameCodeAndLanguageIdAndPlatformIdAndVendorId(betGameCode, languageId, platformId, vendorId);

        Optional.ofNullable(vendorGameCodeResultSet).orElseThrow(GameNotSupportedException::new);

        return vendorGameCodeResultSet;
    }
}
