package com.nextgen.gameaggregator.vendor.api.vendor.servicecomponent.seamless;

import com.nextgen.gameaggregator.vendor.component.constant.Constant;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractSeamlessVendor {
    //TODO REMOTE IP WHEN CREATE NEW RECORD
    //TODO CURRENT USERID WHEN CREATE NEW RECORD
    private Long agent;
    private Long credentialId;
    private String currency;


    @Autowired
    private VendorPlayerWriterManager vendorPlayerWriterManager;

    private VendorPlayerWriter vendorPlayerWriter = new VendorPlayerWriter();

    @Autowired
    private VendorPlayerAuthenticationWriterManager vendorPlayerAuthenticationWriterManager;

    private VendorPlayerAuthenticationWriter vendorPlayerAuthenticationWriter = new VendorPlayerAuthenticationWriter();

    @Autowired
    private VendorReaderManager vendorReaderManager;

    private VendorReader vendorReader = new VendorReader();

    @Autowired
    private VendorPlayerReaderManager vendorPlayerReaderManager;

    private VendorPlayerReader vendorPlayerReader = new VendorPlayerReader();

    @Autowired
    private VendorCredentialReaderManager vendorCredentialReaderManager;

    private VendorCredentialReader vendorCredentialReader = new VendorCredentialReader();

    @Autowired
    private VendorCurrencyMapReaderManager vendorCurrencyMapReaderManager;

    private VendorCurrencyMapReader vendorCurrencyMapReader = new VendorCurrencyMapReader();

    @Autowired
    private VendorLanguageMapReaderManager vendorLanguageMapReaderManager;

    private VendorLanguageMapReader vendorLanguageMapReader = new VendorLanguageMapReader();

    @Autowired
    private VendorGameLanguageMapReaderManager vendorGameLanguageMapReaderManager;

    private VendorGameLanguageMapReader vendorGameLanguageMapReader = new VendorGameLanguageMapReader();

    @Autowired
    private VendorCredentialValueReaderManager vendorCredentialValueReaderManager;

    private List<VendorCredentialValueReader> vendorCredentialValueReader;

    @Autowired
    private VendorPlatformMapReaderManager vendorPlatformMapReaderManager;

    private VendorPlatformMapReader vendorPlatformMapReader = new VendorPlatformMapReader();

    @Autowired
    private VendorPlayerAuthenticationReaderManager vendorPlayerAuthenticationReaderManager;

    private VendorPlayerAuthenticationReader vendorPlayerAuthenticationReader = new VendorPlayerAuthenticationReader();

    @Autowired
    private SeamlessBetHistoryCollectionReaderManager seamlessBetHistoryCollectionReaderManager;

    private SeamlessBetHistoryCollectionReader seamlessBetHistoryCollectionReader = new SeamlessBetHistoryCollectionReader();

    @Autowired
    private SeamlessBetHistoryCollectionWriterManager seamlessBetHistoryCollectionWriterManager;

    private SeamlessBetHistoryCollectionWriter seamlessBetHistoryCollectionWriter = new SeamlessBetHistoryCollectionWriter();

    @Autowired
    private SeamlessBetHistoryRequestRepository seamlessBetHistoryRequestRepository;

    @Autowired
    private SeamlessBetHistoryResultRepository seamlessBetHistoryResultRepository;

    @Autowired
    private BetHistorySeamlessRequestRepository betHistorySeamlessRequestRepository;

    @Autowired
    private BetHistorySeamlessResultRepository betHistorySeamlessResultRepository;


    //region find specific vendor player username from vendor_players table
    public VendorPlayerReader findVendorPlayerUsername(Long agentPlayerId, Long vendorId, Long vendorCredentialId, String currencyCode){
        vendorPlayerReader = vendorPlayerReaderManager.findByAgentPlayerIdAndVendorIdAndVendorCredentialIdAndCurrencyCode(
                agentPlayerId, vendorId, vendorCredentialId, currencyCode);

        return vendorPlayerReader;
    }
    //endregion

    //region find specific vendor credential's latest version from vendor_credentials table
    public Long findVendorLatestCredentialVersion(Long vendorCredentialId, Long vendorId, Long houseId){
        vendorCredentialReader = vendorCredentialReaderManager.findByIdAndVendorIdAndHouseId(vendorCredentialId,
                vendorId, houseId);

        return vendorCredentialReader.getLatestVersion();
    }
    //endregion

    //region find specific vendor credential's latest version from vendor_credentials table
    public String findVendorPlayerUserNameByVendorPlayerId(Long vendorPlayerId){
        vendorPlayerReader = vendorPlayerReaderManager.findById(vendorPlayerId).orElse(null);

        return vendorPlayerReader.getVendorUsername();
    }
    //endregion

    //region find vendor's currency code from vendor_currency_maps table
    public String findVendorCurrencyCode(String currencyCode, Long vendorId){
        vendorCurrencyMapReader = vendorCurrencyMapReaderManager.findByCurrencyCodeAndVendorId(currencyCode, vendorId);

        return vendorCurrencyMapReader.getVendorCurrencyCode();
    }
    //endregion

    //region find vendor's language code from vendor_language_maps table
    public String findVendorLanguageCode(Long vendorId, String languageCode){
        vendorLanguageMapReader = vendorLanguageMapReaderManager.findByVendorIdAndLanguageCode(vendorId, languageCode);

        return vendorLanguageMapReader.getVendorLanguageCode();
    }
    //endregion

    //region find vendor's game code for open game from vendor_games table
    public String findVendorGameCode(Long gameId, String languageCode, String platformCode){
        vendorGameLanguageMapReader = vendorGameLanguageMapReaderManager.findByVendorGameIdAndLanguageCodeAndPlatformCode(
                gameId, languageCode, platformCode);

        return vendorGameLanguageMapReader.getVendorOpenGameCode();
    }
    //endregion

    //region find vendor's platform for open game from vendor_platform_maps table
    public String findVendorPlatformCode(Long vendorId, String platformCode){
        vendorPlatformMapReader = vendorPlatformMapReaderManager.findByVendorIdAndPlatformCode(vendorId, platformCode);

        return vendorPlatformMapReader.getVendorPlatformCode();
    }
    //endregion

    //region match the trace id (token) from vendor and vendor_player_authentication table then get all data
    public VendorPlayerAuthenticationReader findTraceId(String traceId){
        vendorPlayerAuthenticationReader = vendorPlayerAuthenticationReaderManager.findByTraceId(traceId);

        return vendorPlayerAuthenticationReader;
    }

    //region find vendor's latest credentials key and value from vendor_credential_values table
    public Map<String, String> findVendorCredentialKeyAndValue(Long vendorCredentialId, Long latestVersion){
        Map<String, String> credentialMap = new HashMap<String, String>();
        vendorCredentialValueReader = vendorCredentialValueReaderManager.findByVendorCredentialIdAndVersion(
                vendorCredentialId, latestVersion);

        for(VendorCredentialValueReader val : vendorCredentialValueReader) {
            credentialMap.put(val.getKey(), val.getValue());
        }

        return credentialMap;
    }
    //endregion

    //region create player authentication session to vendor_player_authentications table
    public Long createAndGetTraceId(Long vendorId, Long walletType, Long agentPlayerId, Long vendorPlayerId,
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
        return vendorPlayerAuthenticationWriterManager.save(vendorPlayerAuthenticationWriter).getId();

    }
    //endregion

    //region create specific vendor's player to vendor_players table
    public VendorPlayerWriter createAndGetVendorPlayerUsername(Long agentPlayerId, Long vendorId, Long vendorCredentialId,
                                                  Long credentialVersion, String playerUsername, String currencyCode) {

        vendorPlayerWriter.setAgentPlayerId(agentPlayerId);
        vendorPlayerWriter.setVendorId(vendorId);
        vendorPlayerWriter.setVendorCredentialId(vendorCredentialId);
        vendorPlayerWriter.setCredentialsVersion(credentialVersion);
        vendorPlayerWriter.setVendorUsername(playerUsername);
        vendorPlayerWriter.setCurrencyCode(currencyCode);
        vendorPlayerWriter.setStatus(true);
        vendorPlayerWriter.prepareSave(1L, Constant.USER_TYPE, "0.0.0.0");
        return vendorPlayerWriterManager.save(vendorPlayerWriter);
    }
    //endregion

    //region find bet_history_id by vendor_bet_id in seamless_bet_history_collections mariadb table
    public String findBetHistoryIdByVendorBetId (String vendorBetId){
        seamlessBetHistoryCollectionReader = seamlessBetHistoryCollectionReaderManager.findByVendorBetId(vendorBetId);

        return (seamlessBetHistoryCollectionReader == null)?null:seamlessBetHistoryCollectionReader.getBetHistoryId();
    }
    //endregion

    //region create seamless_bet_history_collections mariadb record
    public String createSeamlessBetHistoryCollectionRecord(String betHistoryId, String vendorBetId, String vendorRoundId)
    {
        seamlessBetHistoryCollectionWriter.setBetHistoryId(betHistoryId);
        seamlessBetHistoryCollectionWriter.setVendorBetId(vendorBetId);
        seamlessBetHistoryCollectionWriter.setVendorRoundId(vendorRoundId);
        seamlessBetHistoryCollectionWriter.prepareSave(1L, Constant.USER_TYPE, "0.0.0.0");

        return seamlessBetHistoryCollectionWriterManager.save(seamlessBetHistoryCollectionWriter).getBetHistoryId();
    }
    //endregion

    //region find vendorBetId with service code on couchbase from seamless_bet_history_request collection
    public SeamlessBetHistoryRequest findServiceCodeWithVendorBetIdFromLogSeamlessBetHistoryRequest(String serviceVendorBetId){
        SeamlessBetHistoryRequest seamlessBetHistoryRequest;
        seamlessBetHistoryRequest = this.seamlessBetHistoryRequestRepository.findById(serviceVendorBetId).orElse(null);

        return seamlessBetHistoryRequest;
    }

    //endregion

    //region create seamless_bet_history_request couchbase record
    public String createLogSeamlessBetHistoryRequestCouchBase(String serviceVendorBetId, String vendorBetId, String betHistoryId, String vendorRoundId,
                                                              Double betAmount, Long betTime, Long receivedTime, String requestType,
                                                              String gameCategory, String rawResponse, String vendorCode)
    {
        SeamlessBetHistoryRequest dataSet = new SeamlessBetHistoryRequest(serviceVendorBetId, vendorBetId,
                betHistoryId, vendorRoundId, betAmount, betTime, receivedTime, requestType, gameCategory, rawResponse,
                vendorCode);

        return seamlessBetHistoryRequestRepository.save(dataSet).getBetHistoryId();
    }
    //endregion

    //region find vendorBetId with service code on couchbase from seamless_bet_history_result collection
    public SeamlessBetHistoryResult findServiceCodeWithVendorBetIdFromLogSeamlessBetHistoryResult(String serviceVendorBetId){
        SeamlessBetHistoryResult seamlessBetHistoryResult;
        seamlessBetHistoryResult = this.seamlessBetHistoryResultRepository.findById(serviceVendorBetId).orElse(null);

        return seamlessBetHistoryResult;
    }
    //endregion

    //region create seamless_bet_history_result couchbase record
    public String createLogSeamlessBetHistoryResultCouchBase(String serviceVendorBetId, String vendorBetId, String betHistoryId, String vendorRoundId,
                                                             Double betAmount, Double winLoss, Long betTime, Long settledTime, Long receivedTime,
                                                             String requestType, String gameCategory, String rawResponse, String vendorCode)
    {
        SeamlessBetHistoryResult dataSet = new SeamlessBetHistoryResult(serviceVendorBetId, vendorBetId, betHistoryId,
                vendorRoundId, betAmount, winLoss, betTime, settledTime, receivedTime, requestType, gameCategory,
                rawResponse, vendorCode);

        return seamlessBetHistoryResultRepository.save(dataSet).getBetHistoryId();
    }
    //endregion

    public void createRawBetHistorySeamlessRequestCouchBase(String betHistoryId, String type, String categoryCode,
                                                              String vendorCode, String vendorCurrencyCode, String rawResponse,
                                                              String aggregatorRequestStartMs)
    {
        BetHistorySeamlessRequest dataSet = new BetHistorySeamlessRequest(betHistoryId, type, categoryCode, vendorCode,
                vendorCurrencyCode, rawResponse, aggregatorRequestStartMs);

        this.betHistorySeamlessRequestRepository.save(dataSet);
    }

    public void createRawBetHistorySeamlessResultCouchBase(String betHistoryId, String type, String categoryCode,
                                                            String vendorCode, String vendorCurrencyCode, String rawResponse,
                                                            String aggregatorRequestStartMs)
    {
        BetHistorySeamlessResult dataSet = new BetHistorySeamlessResult(betHistoryId, type, categoryCode, vendorCode,
                vendorCurrencyCode, rawResponse, aggregatorRequestStartMs);

        this.betHistorySeamlessResultRepository.save(dataSet);
    }
}

