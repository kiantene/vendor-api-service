package com.nextgen.gameaggregator.vendor.component.vendor;

import com.nextgen.gameaggregator.vendor.component.constant.Constant;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorCredentialValueReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.writer.entity.VendorPlayerWriter;
import com.nextgen.gameaggregator.vendor.util.NameUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class AbstractVendor extends VendorDataEntity {
    public Long vendorCredentialId;
    public Long vendorId;
    public Long CredentialLatestVersion;
    public Map<String, String> credentialMap = new HashMap<>();

    @Autowired
    private VendorPlayerAuthenticationRepository vendorPlayerAuthenticationRepository;


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

    public void findVendorPlayerUsername(
            Long agentPlayerId, Long agentId, Long masterAgentId, Long houseId,String currencyCode, Boolean createNow) {
        vendorPlayerReader = vendorPlayerReaderManager.findByAgentPlayerIdAndVendorIdAndVendorCredentialIdAndCurrencyCode(
                agentPlayerId, this.vendorId, this.vendorCredentialId, currencyCode);
        if ((vendorPlayerReader == null) && (createNow)) {
            String vendorPlayerUsername = NameUtils.generateUsername(
                    Constant.USERNAME_SEPARATOR, this.vendorCredentialId, agentId, agentPlayerId);

             vendorPlayerWriter = new VendorPlayerWriter();

            vendorPlayerWriter.setAgentPlayerId(agentPlayerId);
            vendorPlayerWriter.setAgentId(agentId);
            vendorPlayerWriter.setMasterAgentId(masterAgentId);
            vendorPlayerWriter.setPassword(null);
            vendorPlayerWriter.setBalance(new BigDecimal(0));
            vendorPlayerWriter.setHouseId(houseId);
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
    }

    public void createPlayerAuthentication(Integer walletType, Long agentPlayerId, Long vendorPlayerId,
                                           String vendorPlayerUsername, String platformCode,
                                           String vendorPlatformCode, String languageCode, String vendorLanguageCode,
                                           Long gameId, String vendorGameCode, Long agentId, String traceId,
                                           String currencyCode, String vendorCurrencyCode, Long createdAt, Long vendorCredentialId){
//        vendorPlayerAuthenticationWriter = new VendorPlayerAuthenticationWriter();
        VendorPlayerAuthentication vendorPlayerAuthentication = new VendorPlayerAuthentication();

        vendorPlayerAuthentication.setId(traceId);
        vendorPlayerAuthentication.setVendorId(this.vendorId);
        vendorPlayerAuthentication.setWalletType(walletType);
        vendorPlayerAuthentication.setAgentPlayerId(agentPlayerId);
        vendorPlayerAuthentication.setVendorPlayerId(vendorPlayerId);
        vendorPlayerAuthentication.setVendorPlayerUsername(vendorPlayerUsername);
        vendorPlayerAuthentication.setPlatformCode(platformCode);
        vendorPlayerAuthentication.setVendorPlatformCode(vendorPlatformCode);
        vendorPlayerAuthentication.setLanguageCode(languageCode);
        vendorPlayerAuthentication.setVendorLanguageCode(vendorLanguageCode);
        vendorPlayerAuthentication.setGameId(gameId);
        vendorPlayerAuthentication.setVendorGameCode(vendorGameCode);
        vendorPlayerAuthentication.setAgentId(agentId);
        vendorPlayerAuthentication.setTraceId(traceId);
        vendorPlayerAuthentication.setCurrencyCode(currencyCode);
        vendorPlayerAuthentication.setVendorCurrencyCode(vendorCurrencyCode);
        vendorPlayerAuthentication.setStatus(true);
        vendorPlayerAuthentication.setCreatedAt(createdAt);
        vendorPlayerAuthentication.setVendorCredentialId(vendorCredentialId);
//        vendorPlayerAuthentication.prepareSave(1L, Constant.USER_TYPE, "0.0.0.0");
//        vendorPlayerAuthenticationWriterManager.save(vendorPlayerAuthenticationWriter);
        vendorPlayerAuthenticationRepository.save(vendorPlayerAuthentication);

    }

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

    //region create specific vendor's player to vendor_players table
    public VendorPlayerWriter createAndGetVendorPlayerUsername(Long agentPlayerId, Long vendorId, Long vendorCredentialId,
                                                               Long credentialVersion, String playerUsername, String currencyCode,
                                                               Long agentId, Long masterAgentId, Long houseId) {

        vendorPlayerWriter.setAgentPlayerId(agentPlayerId);
        vendorPlayerWriter.setVendorId(vendorId);
        vendorPlayerWriter.setVendorCredentialId(vendorCredentialId);
        vendorPlayerWriter.setCredentialsVersion(credentialVersion);
        vendorPlayerWriter.setVendorUsername(playerUsername);
        vendorPlayerWriter.setCurrencyCode(currencyCode);
        vendorPlayerWriter.setStatus(true);
        vendorPlayerWriter.setAgentId(agentId);
        vendorPlayerWriter.setMasterAgentId(masterAgentId);
        vendorPlayerWriter.setHouseId(houseId);
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
    public SeamlessBetHistoryRequest findIdWithVendorBetIdFromLogSeamlessBetHistoryRequest(String serviceVendorBetId){
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
    public SeamlessBetHistoryResult findIdWithVendorBetIdFromLogSeamlessBetHistoryResult(String serviceVendorBetId){
        SeamlessBetHistoryResult seamlessBetHistoryResult;
        seamlessBetHistoryResult = this.seamlessBetHistoryResultRepository.findById(serviceVendorBetId).orElse(null);

        return seamlessBetHistoryResult;
    }
    //endregion

    //region find vendorBetId with service code on couchbase from seamless_bet_history_others collection
    public SeamlessBetHistoryOthersRequest findIdWithVendorBetIdFromLogSeamlessBetHistoryOthers(String serviceVendorBetId){
        SeamlessBetHistoryOthersRequest seamlessBetHistoryOthersRequest;
        seamlessBetHistoryOthersRequest = this.seamlessBetHistoryOthersRequestRepository.findById(serviceVendorBetId).orElse(null);

        return seamlessBetHistoryOthersRequest;
    }
    //endregion

    //region find vendorBetId with service code on couchbase from seamless_bet_history_result collection
    public SeamlessBetHistoryResult findIdAndRequestTypeWithVendorBetIdFromLogSeamlessBetHistoryResult(String serviceVendorBetId,
                                                                                                       String requestType){
        SeamlessBetHistoryResult seamlessBetHistoryResult;
        seamlessBetHistoryResult = this.seamlessBetHistoryResultRepository.findByServiceVendorBetIdAndRequestType(
                serviceVendorBetId, requestType);

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

    //region create seamless_bet_history_others couchbase record
    public String createLogSeamlessBetHistoryOthersCouchBase(String serviceVendorBetId, String vendorBetId, String betHistoryId, String vendorRoundId,
                                                             Double betAmount, Double winLoss, Long betTime, Long settledTime, Long receivedTime,
                                                             String requestType, String gameCategory, String rawResponse, String vendorCode)
    {
        SeamlessBetHistoryOthersRequest dataSet = new SeamlessBetHistoryOthersRequest(serviceVendorBetId, vendorBetId, betHistoryId,
                vendorRoundId, betAmount, winLoss, betTime, settledTime, receivedTime, requestType, gameCategory,
                rawResponse, vendorCode);

        return seamlessBetHistoryOthersRequestRepository.save(dataSet).getBetHistoryId();
    }
    //endregion

    //region create raw.bet_history_seamless_request data to couch base
    public void createRawBetHistorySeamlessRequestCouchBase(String betHistoryId, String type, String categoryCode,
                                                            String vendorCode, String vendorCurrencyCode, String rawResponse,
                                                            String aggregatorRequestStartMs)
    {
        BetHistorySeamlessRequest dataSet = new BetHistorySeamlessRequest(betHistoryId, type, categoryCode, vendorCode,
                vendorCurrencyCode, rawResponse, aggregatorRequestStartMs);

        this.betHistorySeamlessRequestRepository.save(dataSet);
    }
    //endregion

    //region create raw.bet_history_seamless_result data to couch base
    public void createRawBetHistorySeamlessResultCouchBase(String betHistoryId, String type, String categoryCode,
                                                           String vendorCode, String vendorCurrencyCode, String rawResponse,
                                                           String aggregatorRequestStartMs)
    {
        BetHistorySeamlessResult dataSet = new BetHistorySeamlessResult(betHistoryId, type, categoryCode, vendorCode,
                vendorCurrencyCode, rawResponse, aggregatorRequestStartMs);

        this.betHistorySeamlessResultRepository.save(dataSet);
    }
    //endregion

    //region create raw.bet_history_seamless_others data to couch base
    public void createRawBetHistorySeamlessOthersCouchBase(String betHistoryId, String type, String categoryCode,
                                                           String vendorCode, String vendorCurrencyCode, String rawResponse,
                                                           String aggregatorRequestStartMs)
    {
        BetHistorySeamlessOthersRequest dataSet = new BetHistorySeamlessOthersRequest(betHistoryId, type, categoryCode, vendorCode,
                vendorCurrencyCode, rawResponse, aggregatorRequestStartMs);

        this.betHistorySeamlessOthersRequestRepository.save(dataSet);
    }
    //endregion

    //region create log.seamless_end_round data to couch base, logging purposes
    public void createLogSeamlessEndRoundCouchBase(String serviceVendorBetId, String status, Long receivedTime,
                                                   String rawResponse)
    {
        SeamlessEndRoundRequest dataSet = new SeamlessEndRoundRequest(serviceVendorBetId, status, receivedTime,
                rawResponse);

        this.seamlessEndRoundRequestRepository.save(dataSet);
    }
    //endregion

    //region find vendor_bet_id with service code is exists in seamless_refund_log couchbase
    public String findVendorBetIdAndServiceCodeFromSeamlessRefundLogCB(String vendorBetId, String serviceCode){
        SeamlessRefundLogRequest result;
        result = this.seamlessRefundLogRequestRepository.findByVendorBetIdAndVendorCode(vendorBetId, serviceCode);
        return (result != null)?result.getVendorBetId():null;
    }
    //endregion

    //region find vendor_bet_id is exists in seamless_bet_history_request couchbase
    public SeamlessBetHistoryRequest findVendorBetIdFromSeamlessBetHistoryRequest(String vendorBetId){
        SeamlessBetHistoryRequest result;
        result = this.seamlessBetHistoryRequestRepository.findByVendorBetId(vendorBetId);
        return result;
    }
    //endregion

    //region find vendor_bet_id is exists in seamless_bet_history_result couchbase
    public SeamlessBetHistoryResult findVendorBetIdFromSeamlessBetHistoryResult(String vendorBetId){
        SeamlessBetHistoryResult result;
        result = this.seamlessBetHistoryResultRepository.findByVendorBetId(vendorBetId);
        return result;
    }
    //endregion

    //region create log.seamless_end_round ERROR data to couch base, logging purposes
    public void createSeamlessEndRoundErrorCouchBase (String id, Long aggregatorRequestStartMs, String rawRequest){
        SeamlessEndRoundErrorRequest dataSet = new SeamlessEndRoundErrorRequest(id, aggregatorRequestStartMs, rawRequest);
        this.seamlessEndRoundErrorRequestRepository.save(dataSet);
    }
    //endregion
}
