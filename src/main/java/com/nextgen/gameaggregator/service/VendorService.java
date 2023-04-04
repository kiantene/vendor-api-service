package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.entity.custom.IGameVendor;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.CurrencyNotSupportedException;
import com.nextgen.gameaggregator.exception.DisabledVendorException;
import com.nextgen.gameaggregator.exception.InvalidLanguageException;
import com.nextgen.gameaggregator.exception.InvalidVendorException;
import com.nextgen.gameaggregator.operator.game.vendor.GameVendorDto;
import com.nextgen.gameaggregator.repository.LanguageRepository;
import com.nextgen.gameaggregator.repository.VendorCurrencyRepository;
import com.nextgen.gameaggregator.repository.VendorLanguageCodeRepository;
import com.nextgen.gameaggregator.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class VendorService {

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

    public HashMap<String, Language>  findVendorSupportedLanguage(Integer VendorId) throws InvalidLanguageException {
        List<VendorLanguageCode> vendorLanguageCodes= vendorLanguageCodeRepository.findByVendorId(VendorId);
        Optional.ofNullable(vendorLanguageCodes).orElseThrow(InvalidLanguageException::new);

        HashMap<String, Language> vendorLanguages = new HashMap<>();
        for (VendorLanguageCode vendorLanguageCode: vendorLanguageCodes) {

            vendorLanguages.put(vendorLanguageCode.getLanguage().getId().toString(), vendorLanguageCode.getLanguage());
        }
        return vendorLanguages;
    }


    public HashMap<String, Currency>  findVendorSupportedCurrency(Integer VendorId) throws CurrencyNotSupportedException {
        List<VendorCurrency> VendorCurrencyCodes= vendorCurrencyRepository.findByVendorId(VendorId);
        Optional.ofNullable(VendorCurrencyCodes).orElseThrow(CurrencyNotSupportedException::new);

        HashMap<String, Currency> vendorCurrencies = new HashMap<>();
        for (VendorCurrency vendorCurrency: VendorCurrencyCodes) {
            vendorCurrencies.put(vendorCurrency.getCurrency().getId().toString(), vendorCurrency.getCurrency());
        }
        return vendorCurrencies;
    }

    public List<IGameVendor> findAgentSupportedVendorList(GameVendorDto dto, Agent agent) throws InvalidLanguageException {
        Language language = languageRepository.findByCode(dto.getDisplayLanguage());
        Optional.ofNullable(language).orElseThrow(InvalidLanguageException::new);


        return vendorRepository.findByAgentSupportedVendorAndStatus(
                agent.getId(),agent.getCurrency().getId(), language.getId(), Status.ACTIVE.code);
    }

}
