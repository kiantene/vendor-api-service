package com.nextgen.gameaggregator.service;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.entity.custom.IGameVendor;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.*;

@Service
public class VendorService extends BaseVendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorLanguageCodeRepository vendorLanguageCodeRepository;

    @Autowired
    private VendorCurrencyRepository vendorCurrencyRepository;

    @Autowired
    private LanguageRepository languageRepository;


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

    public List<IGameVendor> findAgentSupportedVendors(Language language, Agent agent){

        return vendorRepository.findByAgentSupportedVendorAndStatus(
                agent.getId(), agent.getCurrency().getId(), language.getId(), Status.ACTIVE.code);
    }

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

    public VendorCurrency findVendorCurrency(Vendor vendor, Currency currency) throws VendorCurrencyNotSupportException {
        VendorCurrency vendorCurrency = vendorCurrencyRepository.findByVendorIdAndCurrencyId(vendor.getId(), currency.getId());

        Optional.ofNullable(vendorCurrency).orElseThrow(VendorCurrencyNotSupportException::new);

        if (vendorCurrency.getStatus() == 0) {
            throw new VendorCurrencyNotSupportException();
        }

        return vendorCurrency;
    }
}
