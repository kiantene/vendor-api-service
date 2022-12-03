package com.nextgen.gameaggregator.vendor.component.vendor;

import com.nextgen.gameaggregator.vendor.component.constant.Constant;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorCredentialValueReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorPlayerReader;
import com.nextgen.gameaggregator.vendor.util.NameUtils;

import java.util.HashMap;
import java.util.Map;

public class AbstractVendor extends VendorDataEntity {
    public Long vendorCredentialId;
    public Long vendorId;
    public Long CredentialLatestVersion;
    public Map<String, String> credentialMap = new HashMap<>();



    public void setVendorIdAndCredentialId(Long vendorId, Long vendorCredentialId) {
        this.vendorCredentialId = vendorCredentialId;
        this.vendorId = vendorId;
    }

    public void setCredential(){
        vendorCredentialReader = vendorCredentialReaderManager.findById(this.vendorCredentialId).orElse(null);
        this.CredentialLatestVersion = vendorCredentialReader.getLatestVersion();

        credentialMap = new HashMap<String, String>();
        vendorCredentialValueReader = vendorCredentialValueReaderManager.findByVendorCredentialIdAndVersion(
                vendorCredentialId, this.CredentialLatestVersion);

        for (VendorCredentialValueReader val : vendorCredentialValueReader) {
            credentialMap.put(val.getKey(), val.getValue());
        }
    }



    //region find vendor's platform for open game from vendor_platform_maps table
    public String findVendorPlatformCode(String platformCode) {
        vendorPlatformMapReader = vendorPlatformMapReaderManager.
                findByVendorIdAndPlatformCode(this.vendorId, platformCode);
        return vendorPlatformMapReader.getVendorPlatformCode();
    }
    //endregion

    //region find vendor's language code from vendor_language_maps table
    public String findVendorLanguageCode(String languageCode) {
        vendorLanguageMapReader = vendorLanguageMapReaderManager
                .findByVendorIdAndLanguageCode(this.vendorId, languageCode);
        return vendorLanguageMapReader.getVendorLanguageCode();
    }
    //endregion

    //region find vendor's game code for open game from vendor_games table
    public String findVendorGameCode(Long gameId, String languageCode, String platformCode) {
        vendorGameLanguageMapReader = vendorGameLanguageMapReaderManager
                .findByVendorGameIdAndLanguageCodeAndPlatformCode(gameId, languageCode, platformCode);
        return vendorGameLanguageMapReader.getVendorOpenGameCode();

    }
    //endregion

    //region find vendor's currency code from vendor_currency_maps table
    public String findVendorCurrencyCode(String currencyCode, Long vendorId) {
        vendorCurrencyMapReader = vendorCurrencyMapReaderManager.findByCurrencyCodeAndVendorId(currencyCode, this.vendorId);

        return vendorCurrencyMapReader.getVendorCurrencyCode();
    }
    //endregion

    public VendorPlayerReader findVendorPlayerUsername(
            Long agentPlayerId, Long agentId, String currencyCode, Boolean createNow) {
        vendorPlayerReader = vendorPlayerReaderManager.findByAgentPlayerIdAndVendorIdAndVendorCredentialIdAndCurrencyCode(
                agentPlayerId, this.vendorId, this.vendorCredentialId, currencyCode);
        if ((vendorPlayerReader == null) && (createNow)) {
            String vendorPlayerUsername = NameUtils.generateUsername(
                    Constant.USERNAME_SEPARATOR, this.vendorCredentialId, agentId, agentPlayerId);

            vendorPlayerWriter.setAgentPlayerId(agentPlayerId);
            vendorPlayerWriter.setVendorId(this.vendorId);
            vendorPlayerWriter.setVendorCredentialId(this.vendorCredentialId);
            vendorPlayerWriter.setCredentialsVersion(this.CredentialLatestVersion);
            vendorPlayerWriter.setVendorUsername(vendorPlayerUsername);
            vendorPlayerWriter.setCurrencyCode(currencyCode);
            vendorPlayerWriter.setStatus(true);
            vendorPlayerWriter.prepareSave(1L, Constant.USER_TYPE, "0.0.0.0");
            vendorPlayerWriterManager.save(vendorPlayerWriter);

            vendorPlayerReader = vendorPlayerReaderManager.findById(vendorPlayerWriter.getId()).orElse(null);
        }

        return vendorPlayerReader;
    }

    public void createPlayerAuthentication( Long walletType, Long agentPlayerId, Long vendorPlayerId,
                                           String vendorPlayerUsername, String platformCode,
                                           String vendorPlatformCode, String languageCode, String vendorLanguageCode,
                                           Long gameId, String vendorGameCode, Long agentId, String traceId,
                                           String currencyCode, String vendorCurrencyCode){

        vendorPlayerAuthenticationWriter.setVendorId(vendorId);
        vendorPlayerAuthenticationWriter.setWalletType(walletType);
        vendorPlayerAuthenticationWriter.setAgentPlayerId(agentPlayerId);
        vendorPlayerAuthenticationWriter.setVendorPlayerId(vendorPlayerId);
        vendorPlayerAuthenticationWriter.setVendorPlayerUsername(vendorPlayerUsername);
        vendorPlayerAuthenticationWriter.setPlatformCode(platformCode);
        vendorPlayerAuthenticationWriter.setVendorPlatformCode(vendorPlatformCode);
        vendorPlayerAuthenticationWriter.setLanguageCode(languageCode);
        vendorPlayerAuthenticationWriter.setVendorLanguageCode(vendorLanguageCode);
        vendorPlayerAuthenticationWriter.setGameId(gameId);
        vendorPlayerAuthenticationWriter.setVendorGameCode(vendorGameCode);
        vendorPlayerAuthenticationWriter.setAgentId(agentId);
        vendorPlayerAuthenticationWriter.setTraceId(traceId);
        vendorPlayerAuthenticationWriter.setCurrencyCode(currencyCode);
        vendorPlayerAuthenticationWriter.setVendorCurrencyCode(vendorCurrencyCode);
        vendorPlayerAuthenticationWriter.setStatus(true);
        vendorPlayerAuthenticationWriter.prepareSave(1L, Constant.USER_TYPE, "0.0.0.0");
        vendorPlayerAuthenticationWriterManager.save(vendorPlayerAuthenticationWriter);

    }

}
