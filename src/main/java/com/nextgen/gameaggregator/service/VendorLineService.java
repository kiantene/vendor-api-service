package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.AgentVendorLine;
import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.entity.VendorLineCredential;
import com.nextgen.gameaggregator.entity.VendorLineCurrency;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.AgentVendorLineRepository;
import com.nextgen.gameaggregator.repository.VendorLineCredentialRepository;
import com.nextgen.gameaggregator.repository.VendorLineCurrencyRepository;
import com.nextgen.gameaggregator.repository.VendorLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VendorLineService {
    @Autowired
    private VendorLineRepository vendorLineRepository;
    @Autowired
    private VendorLineCredentialRepository vendorLineCredentialRepository;
    @Autowired
    private AgentVendorLineRepository agentVendorLineRepository;

    @Autowired
    private VendorLineCurrencyRepository vendorLineCurrencyRepository;

    public VendorLine getVendorLineByAgent(Integer agentId, Integer vendorId, Integer currencyId) throws NoAvailableLineException, InvalidVendorLineException {
        AgentVendorLine agentVendorLine = agentVendorLineRepository.findByAgentIdAndVendorIdAndCurrencyId(agentId, vendorId, currencyId);
        Optional.ofNullable(agentVendorLine).orElseThrow(InvalidVendorLineException::new);

        VendorLine vendorLine = agentVendorLine.getVendorLine();
        final Integer INACTIVE = Status.INACTIVE.code;
        if (vendorLine == null || agentVendorLine.getStatus().equals(INACTIVE) || vendorLine.getStatus().equals(INACTIVE)) {
            throw new NoAvailableLineException();
        }
        return vendorLine;
    }

    @Cacheable(value = "VendorLineCredentials", key = "{#vendorLineId, #name}", cacheManager = "cacheManager")
    public String getCredentialValueByName(Integer vendorLineId, String name) throws CredentialNotFoundException {
        final Integer ACTIVE = Status.ACTIVE.code;
        VendorLineCredential credential = vendorLineCredentialRepository.findByVendorLineIdAndNameAndStatus(vendorLineId, name, ACTIVE);
        Optional.ofNullable(credential).orElseThrow(CredentialNotFoundException::new);

        if (credential.getValue().isEmpty()) {
            throw new CredentialNotFoundException();
        }
        return credential.getValue();
    }

    @Cacheable(value = "VendorLineCredentials", key = "{#name, #value}", cacheManager = "cacheManager")
    public Integer getVendorLineIdByNameAndValue(String name, String value) throws CredentialNotFoundException {
        final Integer ACTIVE = Status.ACTIVE.code;
        VendorLineCredential credential = vendorLineCredentialRepository.findByNameAndValueAndStatus(name, value, ACTIVE);
        Optional.ofNullable(credential).orElseThrow(CredentialNotFoundException::new);

        if (credential.getValue().isEmpty()) {
            throw new CredentialNotFoundException();
        }
        return credential.getVendorLineId();
    }

    @Cacheable(value = "VendorLines", key = "#vendorLineId", cacheManager = "cacheManager")
    public Integer verifyVendorLineStatus(Integer vendorLineId) throws DisabledVendorLineException {
        VendorLine vendorLine = vendorLineRepository.findByIdAndStatus(vendorLineId, Status.ACTIVE.code);
        Optional.ofNullable(vendorLine).orElseThrow(DisabledVendorLineException::new);

        return vendorLine.getId();
    }


    public VendorLineCurrency checkVendorLineSupportedCurrency(Integer vendorLineId, Integer currencyId) throws CurrencyNotSupportedException {
        final Integer ACTIVE = Status.ACTIVE.code;
        VendorLineCurrency entity = vendorLineCurrencyRepository.findByVendorLineIdAndCurrencyIdAndStatus(vendorLineId, currencyId, ACTIVE);
        Optional.ofNullable(entity).orElseThrow(CurrencyNotSupportedException::new);

        if (entity.getVendorCurrencyCode().isEmpty()) {
            throw new CurrencyNotSupportedException();
        }
        return entity;
    }

    public Map<String, String> toCredentialMap(VendorLine vendorLine) {
        return vendorLine.getCredentials().stream()
                .filter(v -> v.getStatus().equals(Status.ACTIVE.code))
                .collect(Collectors.toMap(VendorLineCredential::getName, VendorLineCredential::getValue));
    }

    public VendorLine getVendorLineById(Integer vendorLineId) throws InvalidVendorLineException {
        VendorLine vendorLine = vendorLineRepository.findByIdAndStatus(vendorLineId, Status.ACTIVE.code);
        Optional.ofNullable(vendorLine).orElseThrow(InvalidVendorLineException::new);

        return vendorLine;
    }
}
