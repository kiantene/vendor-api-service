package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameCodeRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VendorGameCodeService {

    private final VendorGameCodeRepository vendorGameCodeRepository;

    public VendorGameCodeService(VendorGameCodeRepository vendorGameCodeRepository) {
        this.vendorGameCodeRepository = vendorGameCodeRepository;
    }

    @Cacheable(value = "VendorGameCode", key = "{#vendorGameId}", cacheManager = "cacheManager")
    public VendorGameCode getByTop1VendorGameId(Integer vendorGameId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeRepository.findTop1ByVendorGameIdAndStatus(vendorGameId, Status.ACTIVE.code);

        return Optional.ofNullable(vendorGameCode).orElseThrow(GameNotSupportedException::new);
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

    public List<VendorGameCode> getByVendorGameIdAndLanguageId(Integer vendorGameId, Integer languageId) {
        return vendorGameCodeRepository.findByVendorGameIdAndLanguageId(vendorGameId, languageId);
    }
}
