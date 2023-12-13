package com.nextgen.gameaggregator.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.entity.custom.IGameVendor;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.*;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorLanguageCodeRepository vendorLanguageCodeRepository;

    @Autowired
    private VendorCurrencyRepository vendorCurrencyRepository;

    @Autowired
    private VendorGameService vendorGameService;


    public Vendor verifyVendorByCodeAndWalletType(String code, Integer walletType) throws InvalidVendorException, DisabledVendorException {

        Vendor vendor = vendorRepository.findByCode(code);
        Optional.ofNullable(vendor).orElseThrow(InvalidVendorException::new);

        final Integer INACTIVE = Status.INACTIVE.code;
        if (vendor == null || vendor.getStatus().equals(INACTIVE)) {
            throw new DisabledVendorException();
        }

        if (walletType == 1 && vendor.getIsSupportSeamless() == 0) {
            throw new InvalidVendorException();
        } else if (walletType == 2 && vendor.getIsSupportTransfer() == 0) {
            throw new InvalidVendorException();
        }

        return vendor;
    }

    public Vendor findVendorByCode(String vendorCode) throws InvalidVendorException {
        Vendor vendor = vendorRepository.findByCode(vendorCode);
        Optional.ofNullable(vendor).orElseThrow(InvalidVendorException::new);
        return vendor;
    }

    public HashMap<String, Language> findVendorSupportedLanguages(Integer VendorId) throws InvalidLanguageException {
        List<VendorLanguageCode> vendorLanguageCodes = vendorLanguageCodeRepository.findByVendorId(VendorId);
        Optional.ofNullable(vendorLanguageCodes).orElseThrow(InvalidLanguageException::new);

        HashMap<String, Language> vendorLanguages = new HashMap<>();
        for (VendorLanguageCode vendorLanguageCode : vendorLanguageCodes) {

            vendorLanguages.put(vendorLanguageCode.getLanguage().getId().toString(), vendorLanguageCode.getLanguage());
        }
        return vendorLanguages;
    }


    public HashMap<String, Currency> findVendorSupportedCurrencies(Integer VendorId) throws CurrencyNotSupportedException {
        List<VendorCurrency> VendorCurrencyCodes = vendorCurrencyRepository.findByVendorId(VendorId);
        Optional.ofNullable(VendorCurrencyCodes).orElseThrow(CurrencyNotSupportedException::new);

        HashMap<String, Currency> vendorCurrencies = new HashMap<>();
        for (VendorCurrency vendorCurrency : VendorCurrencyCodes) {
            vendorCurrencies.put(vendorCurrency.getCurrency().getId().toString(), vendorCurrency.getCurrency());
        }
        return vendorCurrencies;
    }

    @Cacheable(value = "Vendors", key = "{#agent.id, #language.id}", cacheManager = "cacheManager")
    public List<IGameVendor> findAgentSupportedVendors(Language language, Agent agent) {
        return vendorRepository.findByAgentSupportedVendorAndStatus(
                agent.getId(), language.getId(), Status.ACTIVE.code);
    }

    @Cacheable(value = "Vendors", key = "{#agent.id, #language.id, #currency.id}", cacheManager = "cacheManager")
    public List<IGameVendor> findAgentSupportedVendors(Language language, Agent agent, Currency currency) {

        return vendorRepository.findByAgentSupportedVendorAndStatusAndCurrency(
                agent.getId(), currency.getId(), language.getId(), Status.ACTIVE.code);
    }

    @Cacheable(value = "VendorLanguages", key = "{#language.id, #vendor.id}", cacheManager = "cacheManager")
    public VendorLanguageCode findVendorLanguageCode(Vendor vendor, Language language) throws VendorLanguageNotSupportedException {

        VendorLanguageCode vendorLanguageCode =
                vendorLanguageCodeRepository.findByVendorIdAndLanguageId(vendor.getId(), language.getId());
        Optional.ofNullable(vendorLanguageCode).orElseThrow(VendorLanguageNotSupportedException::new);

        if (vendorLanguageCode.getStatus() == 0) {
            throw new VendorLanguageNotSupportedException();
        }

        return vendorLanguageCode;
    }

    public VendorLanguageCode getFirstVendorLanguageCode(Vendor vendor) throws VendorLanguageNotSupportedException {
        VendorLanguageCode vendorLanguageCode = vendorLanguageCodeRepository.findTop1ByVendorIdAndStatus(vendor.getId(), Status.ACTIVE.code);
        Optional.ofNullable(vendorLanguageCode).orElseThrow(VendorLanguageNotSupportedException::new);

        return vendorLanguageCode;
    }

    @Cacheable(value = "VendorCurrencies", key = "{#vendorId, #currencyId}", cacheManager = "cacheManager")
    public VendorCurrency findVendorCurrency(Integer vendorId, Integer currencyId) throws VendorCurrencyNotSupportException {
        VendorCurrency vendorCurrency = vendorCurrencyRepository.findByVendorIdAndCurrencyId(vendorId, currencyId);

        Optional.ofNullable(vendorCurrency).orElseThrow(VendorCurrencyNotSupportException::new);

        if (vendorCurrency.getStatus() == 0) {
            throw new VendorCurrencyNotSupportException();
        }

        return vendorCurrency;
    }

//    public GameSession verifyAndRegenerateNewVendorGameCodeForGameSession(String vendorGameCode, GameSession gameSession) throws GameNotSupportedException {
//
//        //if vendorGameCode is not matched with gameSession vendorGameCode, then regenerate the new vendorGameCode details
//        if (vendorGameCode != gameSession.getVendorGameCode()) {
//            VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(vendorGameCode, gameSession.getVendorId());
//            gameSession.setGameCode(vendorGame.getCode());
//            gameSession.setVendorGameId(vendorGame.getId());
//            gameSession.setVendorGameCode(vendorGame.getVendorGameCode());
//            gameSession.setGameCategoryId(vendorGame.getGameCategory().getId());
//
//        }
//        return gameSession;
//    }

    public VendorCurrency getCurrencyConversionRate(GameSession gameSession, String traceId) throws VendorCurrencyNotSupportException {
        BigDecimal defaultConversionRateAsOne = BigDecimal.ONE;
        VendorCurrency vendorCurrency = this.findVendorCurrency(gameSession.getVendorId(), gameSession.getCurrencyId());

        //if unset or zero for FromVendorRate, will be set as 1
        if (vendorCurrency.getFromVendorRate() == null || vendorCurrency.getFromVendorRate().compareTo(BigDecimal.ZERO) == 0) {
            vendorCurrency.setFromVendorRate(defaultConversionRateAsOne);
            log.info("Currency conversion failed, FromVendorRate() is zero or empty for vendorId : " + gameSession.getVendorId() + " | currencyId = " + gameSession.getVendorId() + " ｜ traceId = " + traceId);

        }

        //if unset or zero for ToVendorRate, will be set as 1
        if (vendorCurrency.getToVendorRate() == null || vendorCurrency.getToVendorRate().compareTo(BigDecimal.ZERO) == 0) {
            vendorCurrency.setToVendorRate(defaultConversionRateAsOne);
            log.info("Currency conversion failed, ToVendorRate() is zero or empty for vendorId : " + gameSession.getVendorId() + " | currencyId = " + gameSession.getVendorId() + " ｜ traceId = " + traceId);

        }

        return vendorCurrency;
    }
}
