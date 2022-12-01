package com.nextgen.gameaggregator.vendor.api.vendor.servicecomponent.seamless;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class SeamlessVendorAdaptor {

    @Autowired
    private ApplicationContext context;

    public InterfaceSeamlessVendor seamlessVendor;

    public void getVendor(String classFileName){

        this.seamlessVendor =  (InterfaceSeamlessVendor) context.getBean(classFileName);
    }
}
