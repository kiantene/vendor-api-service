package com.nextgen.gameaggregator.vendor.component.vendor;

import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.writer.entity.SeamlessBetHistoryCollectionWriter;
import com.nextgen.gameaggregator.vendor.data.mariadb.writer.entity.VendorPlayerAuthenticationWriter;
import com.nextgen.gameaggregator.vendor.data.mariadb.writer.entity.VendorPlayerWriter;
import com.nextgen.gameaggregator.vendor.data.mariadb.writer.manager.SeamlessBetHistoryCollectionWriterManager;
import com.nextgen.gameaggregator.vendor.data.mariadb.writer.manager.VendorPlayerAuthenticationWriterManager;
import com.nextgen.gameaggregator.vendor.data.mariadb.writer.manager.VendorPlayerWriterManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class VendorDataEntity {
    //region Database object
    @Autowired
    public VendorCredentialReaderManager vendorCredentialReaderManager;
    public VendorCredentialReader vendorCredentialReader;

    @Autowired
    public VendorCredentialValueReaderManager vendorCredentialValueReaderManager;
    public List<VendorCredentialValueReader> vendorCredentialValueReader;

    @Autowired
    public VendorPlatformMapReaderManager vendorPlatformMapReaderManager;
    public VendorPlatformMapReader vendorPlatformMapReader;

    @Autowired
    public VendorLanguageMapReaderManager vendorLanguageMapReaderManager;
    public VendorLanguageMapReader vendorLanguageMapReader;

    @Autowired
    public VendorGameLanguageMapReaderManager vendorGameLanguageMapReaderManager;
    public VendorGameLanguageMapReader vendorGameLanguageMapReader ;

    @Autowired
    public VendorCurrencyMapReaderManager vendorCurrencyMapReaderManager;
    public VendorCurrencyMapReader vendorCurrencyMapReader;

    @Autowired
    public VendorPlayerReaderManager vendorPlayerReaderManager;
    public VendorPlayerReader vendorPlayerReader;

    @Autowired
    public VendorPlayerWriterManager vendorPlayerWriterManager;
    public VendorPlayerWriter vendorPlayerWriter;

    @Autowired
    public VendorPlayerAuthenticationWriterManager vendorPlayerAuthenticationWriterManager;
    public VendorPlayerAuthenticationWriter vendorPlayerAuthenticationWriter;
    //endregion

    @Autowired
    public VendorReaderManager vendorReaderManager;

    public VendorReader vendorReader = new VendorReader();

    @Autowired
    public AgentPlayerManager agentPlayerManager;

    public AgentPlayer agentPlayer = new AgentPlayer();

    @Autowired
    public SeamlessBetHistoryCollectionReaderManager seamlessBetHistoryCollectionReaderManager;

    public SeamlessBetHistoryCollectionReader seamlessBetHistoryCollectionReader = new SeamlessBetHistoryCollectionReader();

    @Autowired
    public SeamlessBetHistoryCollectionWriterManager seamlessBetHistoryCollectionWriterManager;

    public SeamlessBetHistoryCollectionWriter seamlessBetHistoryCollectionWriter = new SeamlessBetHistoryCollectionWriter();

    @Autowired
    public SeamlessBetHistoryRequestRepository seamlessBetHistoryRequestRepository;

    @Autowired
    public SeamlessBetHistoryResultRepository seamlessBetHistoryResultRepository;

    @Autowired
    public BetHistorySeamlessRequestRepository betHistorySeamlessRequestRepository;

    @Autowired
    public BetHistorySeamlessResultRepository betHistorySeamlessResultRepository;

    @Autowired
    public SeamlessEndRoundRequestRepository seamlessEndRoundRequestRepository;

    @Autowired
    public SeamlessEndRoundErrorRequestRepository seamlessEndRoundErrorRequestRepository;

    @Autowired
    public SeamlessRefundLogRequestRepository seamlessRefundLogRequestRepository;

    @Autowired
    public SeamlessBetHistoryOthersRequestRepository seamlessBetHistoryOthersRequestRepository;

    @Autowired
    public BetHistorySeamlessOthersRequestRepository betHistorySeamlessOthersRequestRepository;

}
