package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorGameCodeRepository extends JpaRepository<VendorGameCode, Integer> {
    VendorGameCode findByVendorGameIdAndPlatformIdAndLanguageId(Integer vendorGameId, Integer platformId, Integer languageId);

    @Cacheable(value = "VendorGameCode", key = "{#vendorGameId, #platformId, #languageId, #status}", cacheManager = "cacheManager")
    VendorGameCode findByVendorGameIdAndPlatformIdAndLanguageIdAndStatus(Integer vendorGameId, Integer platformId, Integer languageId, Integer status);

    @Cacheable(value = "VendorGameCode", key = "{#status, #languageId, #platformId, #openGameCode, #vendorId}", cacheManager = "cacheManager")
    VendorGameCode findByOpenGameCodeAndPlatformIdAndLanguageIdAndStatusAndVendorId(String openGameCode, Integer platformId, Integer languageId, Integer status, Integer vendorId);

    List<VendorGameCode> findByVendorGameId(Integer vendorGameId);

    List<VendorGameCode> findByVendorGameIdAndLanguageId(Integer vendorGameId, Integer languageId);

    VendorGameCode findTop1ByVendorGameIdAndStatus(Integer vendorGameId, Integer status);

    VendorGameCode findByBetGameCodeAndLanguageIdAndPlatformIdAndVendorId(String gameCode, Integer languageId, Integer platformId, Integer vendorId);

    VendorGameCode findByProductGameIdAndVendorIdAndPlatformIdAndLanguageId(Integer productGameId, Integer vendorId, Integer platformId, Integer languageId);
}
