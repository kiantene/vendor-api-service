package com.nextgen.gameaggregator.vendor.component.vendor;

import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.VendorReaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class VendorAdaptor {
    private static final Logger logger = LoggerFactory.getLogger(VendorAdaptor.class);
    @Autowired
    private ApplicationContext context;

    public InterfaceSeamlessVendor seamlessVendor;

    public InterfaceTransferVendor transferVendor;

    @Autowired
    private VendorReaderManager vendorReaderManager;
    private VendorReader vendorReader;


    public Boolean getVendor(Long vendorId, Integer walletType, Long vendorCredentialId){

        vendorReader = vendorReaderManager.findById(vendorId).orElse(null);
        try{
            if(vendorReader !=null){
                if(walletType == 1){
                    //TODO need separate class name
                    this.seamlessVendor =
                            (InterfaceSeamlessVendor) context.getBean(""+vendorReader.getClassFile(), vendorId, vendorCredentialId);
                }else{
                    //TODO need separate class name
                    this.transferVendor =
                            (InterfaceTransferVendor) context.getBean(""+vendorReader.getClassFile(), vendorId, vendorCredentialId);
                }
                return true;
            }
        }catch (Exception exception){
            logger.error(exception.getMessage());
            return false;
        }
        return false;
    }
}
