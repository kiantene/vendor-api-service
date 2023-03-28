package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.Vendor;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledVendorException;
import com.nextgen.gameaggregator.exception.InvalidVendorException;
import com.nextgen.gameaggregator.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;


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
}
