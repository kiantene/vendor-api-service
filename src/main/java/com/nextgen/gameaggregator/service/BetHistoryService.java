package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.data.mariadb.config.GaServiceWriterDataSourceConfig;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.transactions.detail.*;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.ga.writer.BetHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Map;


@Service
@Slf4j
public class BetHistoryService {
    @Value("${spring.datasource.clickhouse-default.enable:false}")
    private Boolean enableClickHouse;

    @Value("${aws.s3.bet-bucket.read:false}")
    private Boolean enableBetBucketRead;
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final BetHistoryRepository betHistoryRepository;
    private final GaServiceWriterDataSourceConfig gaServiceWriterDataSourceConfig;
    private final VendorLineService vendorLineService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;
    private final VendorService vendorService;

    public BetHistoryService(AutowireCapableBeanFactory autowireCapableBeanFactor, BetHistoryRepository betHistoryRepository,
                             GaServiceWriterDataSourceConfig gaServiceWriterDataSourceConfig, VendorLineService vendorLineService,
                             WarehouseBetHistoryService warehouseBetHistoryService, VendorService vendorService) {
        this.autowireCapableBeanFactory = autowireCapableBeanFactor;
        this.betHistoryRepository = betHistoryRepository;
        this.gaServiceWriterDataSourceConfig = gaServiceWriterDataSourceConfig;
        this.vendorLineService = vendorLineService;
        this.warehouseBetHistoryService = warehouseBetHistoryService;
        this.vendorService = vendorService;
    }

    public Long getVendorSettleTime(BetResultData betResultData, UnsettledBet unsettledBet) {
        long settledTime = System.currentTimeMillis();

        if (betResultData.getVendorSettleTime() != null) {
            settledTime = betResultData.getVendorSettleTime();
        } else if (unsettledBet != null && unsettledBet.getVendorSettleTime() != null) {
            settledTime = unsettledBet.getVendorSettleTime();
        }

        return settledTime;
    }

    /**
     * Creates a database record of the given BetHistory entity object.
     * This function will also populate default values of certain fields.
     *
     * @param entity BetHistory entity object containing information of a single bet
     * @return BetHistory entity object after a successful save
     */
    @CachePut(value = "BetHistories", key = "{#entity.roundId, #entity.vendorGameId, #entity.vendorPlayerId}", cacheManager = "cacheManager")
    public BetHistory create(BetHistory entity) throws DuplicateExternalTransactionIdException {
        // Set default values
        entity.setWinAmount(BigDecimal.ZERO);
        entity.setWinLoss(BigDecimal.ZERO);
        entity.setEffectiveTurnover(BigDecimal.ZERO);
        entity.setResultType(ResultType.LOSE.code);
        entity.setStatus(BetStatus.UNSETTLED.code);

        try {
            betHistoryRepository.save(entity);

        } catch (DataIntegrityViolationException dataIntegrityViolationException) {

            throw new DuplicateExternalTransactionIdException("Duplicate bet_history " +
                    ", external_transaction_id:" + entity.getExternalTransactionId() +
                    ", round_id:" + entity.getRoundId() +
                    ", vendor_line_id:" + entity.getVendorLineId());
        }

        return entity;
    }


    /**
     * Retrieve a bet transaction record based on vendor's round Id
     *
     * @param roundId        Vendor's round Id
     * @param gameId         Game Id within Game Aggregator System
     * @param vendorPlayerId Id of the record in VendorPlayer
     * @return BetHistory entity object containing all information of a single Bet
     * @throws BetNotFoundException If no bet record is found
     */
    // TODO: performance tuning, read from cache
    @Cacheable(value = "BetHistories", key = "{#roundId, #gameId, #vendorPlayerId}", cacheManager = "cacheManager")
    public BetHistory getBetTransactionByRoundId(String roundId, Integer gameId, Long vendorPlayerId) throws BetNotFoundException {
        BetHistory betHistory = betHistoryRepository.findByRoundIdAndVendorGameIdAndVendorPlayerId(roundId, gameId, vendorPlayerId);
        if (betHistory == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find round Id: " + roundId);
        }
        return betHistory;
    }

    public IBetDetailUrlInfo getBetHistoryDetail(Integer agentId, String betId) throws BetNotFoundException {

        IBetDetailUrlInfo iBetDetailUrlInfo = null;
        iBetDetailUrlInfo = betHistoryRepository.findByIdAndAgentId(agentId, betId);

        if (iBetDetailUrlInfo == null) { // No matching bet record for the given transaction Id
            throw new BetNotFoundException();
        }

        return iBetDetailUrlInfo;
    }

    public IBetDetailUrlInfo getBetHistoryDetailV2(Integer agentId, String betId, long fromTime, long toTime) throws BetNotFoundException {

        IBetDetailUrlInfo iBetDetailUrlInfo = null;

        iBetDetailUrlInfo = warehouseBetHistoryService.getBetHistoryDetailV2(agentId, betId, fromTime, toTime);

        if (iBetDetailUrlInfo == null) { // No matching bet record for the given transaction Id
            throw new BetNotFoundException();
        }

        return iBetDetailUrlInfo;
    }


    public TransactionDetailData getDetailUrl(IBetDetailUrlInfo iBetDetailUrlInfo, TransactionDetailData transactionDetailData,
                                              VendorLine vendorLine, VendorLanguageCode vendorLanguageCode) throws
            InvalidVendorResponseException, DisabledVendorLineException, InvalidVendorLineException {


        //2. get vendor line credential
        Map<String, String> credentials = vendorLineService.toCredentialMap(vendorLine.getId());


        try {
            String vendorClassName = vendorService.getByVendorId(vendorLine.getVendorId(), null).getClassName();

            String className = "com.nextgen.gameaggregator.vendor." + vendorClassName + ".api.betdetail.BetDetailService";
            BetDetailUrl betDetailUrl = (BetDetailUrl) Class.forName(className).getConstructor().newInstance();
            autowireCapableBeanFactory.autowireBean(betDetailUrl);
            MultiValueMap<String, String> formData = betDetailUrl.formDataBuilder(credentials, iBetDetailUrlInfo, vendorLanguageCode);

            BetDetailUrlVo betDetailUrlVo = betDetailUrl.call(formData, credentials, iBetDetailUrlInfo, vendorLanguageCode);
            if (betDetailUrlVo != null) {
                transactionDetailData.setDetailUrl(betDetailUrlVo.getBetDetailUrl());
            }

            return transactionDetailData;
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | InvalidVendorLineException |
                 InvalidFormatException | RecordNotFoundException | InvalidVendorException
                gameClassException) {
            gameClassException.printStackTrace();
            log.error("GAME CLASS ERROR :" + gameClassException.getStackTrace().toString());
            throw new InvalidVendorResponseException();
        }
    }

    public TransactionDetailData getSportBetDetail(IBetDetailUrlInfo iBetDetailUrlInfo, TransactionDetailData transactionDetailData,
                                                   VendorLine vendorLine, VendorLanguageCode vendorLanguageCode) throws
            InvalidVendorResponseException, DisabledVendorLineException, InvalidVendorLineException {

        //2. get vendor line credential
        Map<String, String> credentials = vendorLineService.toCredentialMap(vendorLine.getId());


        try {
            String vendorClassName = vendorService.getByVendorId(vendorLine.getVendorId(), null).getClassName();

            String className = "com.nextgen.gameaggregator.vendor." + vendorClassName + ".api.betdetail.BetDetailService";
            SportBetDetail<?> sportBetDetail = (SportBetDetail<?>) Class.forName(className).getConstructor().newInstance();
            autowireCapableBeanFactory.autowireBean(sportBetDetail);
            MultiValueMap<String, String> formData = sportBetDetail.formDataBuilder(credentials, iBetDetailUrlInfo, vendorLanguageCode);

            Object vo = sportBetDetail.call(formData, credentials, iBetDetailUrlInfo, vendorLanguageCode);

            if (vo instanceof BetDetailUrlVo betDetailUrlVo) {
                transactionDetailData.setDetailUrl(betDetailUrlVo.getBetDetailUrl());
            } else {
                transactionDetailData.setDetailUrl("");
                transactionDetailData.setSportBetDetail(new SportBetDetailData((SportBetDetailVo) vo));
            }

            return transactionDetailData;
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | InvalidVendorLineException |
                 InvalidFormatException | RecordNotFoundException | InvalidVendorException
                gameClassException) {
            gameClassException.printStackTrace();
            log.error("GAME CLASS ERROR :" + gameClassException.getStackTrace().toString());
            throw new InvalidVendorResponseException();
        }
    }
}
