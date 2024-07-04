package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameCodeRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VendorGameCodeService {

    private final VendorGameCodeRepository vendorGameCodeRepository;

    public VendorGameCodeService(VendorGameCodeRepository vendorGameCodeRepository) {
        this.vendorGameCodeRepository = vendorGameCodeRepository;
    }

    public VendorGameCode getByVendorGameIdAndPlatformIdAndLanguageId(Integer vendorGameId, Integer platformId, Integer languageId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeRepository.findByVendorGameIdAndPlatformIdAndLanguageIdAndStatus(vendorGameId, platformId, languageId, Status.ACTIVE.code);

        return Optional.ofNullable(vendorGameCode).orElseThrow(GameNotSupportedException::new);
    }

    @Cacheable(value = "VendorGameCode", key = "{#gameCode, #languageId, #platformId, #vendorId}", cacheManager = "cacheManager")
    public VendorGameCode getByBetGameCode(String gameCode, Integer languageId, Integer platformId, Integer vendorId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeRepository.findByBetGameCodeAndLanguageIdAndPlatformIdAndVendorId(gameCode, languageId, platformId, vendorId);

        if (vendorGameCode == null) throw new GameNotSupportedException();

        if (vendorGameCode.getStatus().equals(Status.INACTIVE.code)) {
            throw new GameNotSupportedException();
        }

        return vendorGameCode;
    }
}
