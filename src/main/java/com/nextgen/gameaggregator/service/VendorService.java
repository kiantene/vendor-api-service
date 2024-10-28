package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.entity.ga.custom.IGameVendor;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.ga.reader.VendorReaderRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorCurrencyRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorLanguageCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    private final VendorReaderRepository vendorReaderRepository;
    private final VendorLanguageCodeRepository vendorLanguageCodeRepository;
    private final VendorCurrencyRepository vendorCurrencyRepository;
    private final CurrencyService currencyService;

    public VendorService(VendorReaderRepository vendorReaderRepository,
                         VendorLanguageCodeRepository vendorLanguageCodeRepository,
                         VendorCurrencyRepository vendorCurrencyRepository,
                         CurrencyService currencyService) {

        this.vendorReaderRepository = vendorReaderRepository;
        this.vendorLanguageCodeRepository = vendorLanguageCodeRepository;
        this.vendorCurrencyRepository = vendorCurrencyRepository;
        this.currencyService = currencyService;
    }

    @Cacheable(value = "Vendors", key = "{#id}", cacheManager = "cacheManager", unless = "#result == null")
    public Vendor getById(Integer id) throws InvalidVendorException {
        return vendorReaderRepository.findById(id).orElseThrow(InvalidVendorException::new);
    }

    public Vendor verifyVendorByCodeAndWalletType(String code, Integer walletType) throws InvalidVendorException, DisabledVendorException {

        Vendor vendor = vendorReaderRepository.findByCode(code);
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
        Vendor vendor = vendorReaderRepository.findByCode(vendorCode);
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
            Integer currencyId = vendorCurrency.getCurrencyId();
            try {
                Currency currency = currencyService.get(currencyId);
                vendorCurrencies.put(currencyId.toString(), currency);
            } catch (InvalidCurrencyException invalidCurrencyException) {
                // do nothing to suppress the error
            }
        }
        return vendorCurrencies;
    }

    @Cacheable(value = "Vendors", key = "{#agent.id, #language.id}", cacheManager = "cacheManager")
    public List<IGameVendor> findAgentSupportedVendors(Language language, Agent agent) {
        return vendorReaderRepository.findByAgentSupportedVendorAndStatus(
                agent.getId(), language.getId(), Status.ACTIVE.code);
    }

    @Cacheable(value = "Vendors", key = "{#agent.id, #language.id, #currency.id}", cacheManager = "cacheManager")
    public List<IGameVendor> findAgentSupportedVendors(Language language, Agent agent, Currency currency) {

        return vendorReaderRepository.findByAgentSupportedVendorAndStatusAndCurrency(
                agent.getId(), currency.getId(), language.getId(), Status.ACTIVE.code);
    }

    @Cacheable(value = "VendorLanguages", key = "{#languageId, #vendorId}", cacheManager = "cacheManager")
    public VendorLanguageCode findVendorLanguageCode(Integer vendorId, Integer languageId) throws VendorLanguageNotSupportedException {

        VendorLanguageCode vendorLanguageCode =
                vendorLanguageCodeRepository.findByVendorIdAndLanguageId(vendorId, languageId);
        Optional.ofNullable(vendorLanguageCode).orElseThrow(VendorLanguageNotSupportedException::new);

        if (vendorLanguageCode.getStatus() == 0) {
            throw new VendorLanguageNotSupportedException();
        }

        return vendorLanguageCode;
    }


    public VendorCurrency findVendorCurrency(Integer vendorId, Integer currencyId) throws VendorCurrencyNotSupportException {
        VendorCurrency vendorCurrency = vendorCurrencyRepository.findByVendorIdAndCurrencyId(vendorId, currencyId);

        Optional.ofNullable(vendorCurrency).orElseThrow(VendorCurrencyNotSupportException::new);

        if (vendorCurrency.getStatus() == 0) {
            throw new VendorCurrencyNotSupportException();
        }

        return vendorCurrency;
    }

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

    @Cacheable(value = "Vendors", key = "{#vendorId}", cacheManager = "cacheManager", unless = "#result == null")
    public Vendor getByVendorId(Integer vendorId, Vendor vendor) throws InvalidVendorException {
        if (vendor != null) return vendor;

        return this.getById(vendorId);
    }
}
