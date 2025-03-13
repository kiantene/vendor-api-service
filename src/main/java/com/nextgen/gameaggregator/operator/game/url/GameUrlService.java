package com.nextgen.gameaggregator.operator.game.url;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.ga.writer.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.NameUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class GameUrlService {
    private static final String USERTYPE = "operator-api-service";
    private final AgentService agentService;
    private final AgentProductService agentProductService;
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final RawGameSessionRepository rawGameSessionRepository;
    private final AgentPlayerRepository agentPlayerRepository;
    private final VendorPlayerRepository vendorPlayerRepository;
    private final PlatformRepository platformRepository;
    private final VendorService vendorService;
    private final VendorGameCurrencyRepository vendorGameCurrencyRepository;
    private final CurrencyRepository currencyRepository;
    private final RequestService requestService;
    private final ProductGameService productGameService;
    private final VendorLineService vendorLineService;
    private final VendorGameCodeService vendorGameCodeService;
    private final VendorGameDeactivatedService vendorGameDeactivatedService;

    //remove request service
    @Autowired
    public GameUrlService(AgentServiceImpl agentService,
                          AgentProductServiceImpl agentProductService,
                          AutowireCapableBeanFactory autowireCapableBeanFactory,
                          RawGameSessionRepository rawGameSessionRepository,
                          AgentPlayerRepository agentPlayerRepository,
                          VendorPlayerRepository vendorPlayerRepository,
                          PlatformRepository platformRepository,
                          VendorGameCurrencyRepository vendorGameCurrencyRepository,
                          CurrencyRepository currencyRepository,
                          VendorService vendorService,
                          RequestService requestService,
                          ProductGameServiceImpl productGameService,
                          VendorLineService vendorLineService,
                          VendorGameCodeService vendorGameCodeService,
                          VendorGameDeactivatedService vendorGameDeactivatedService) {

        this.agentService = agentService;
        this.agentProductService = agentProductService;
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
        this.rawGameSessionRepository = rawGameSessionRepository;
        this.agentPlayerRepository = agentPlayerRepository;
        this.vendorPlayerRepository = vendorPlayerRepository;
        this.platformRepository = platformRepository;
        this.vendorGameCurrencyRepository = vendorGameCurrencyRepository;
        this.currencyRepository = currencyRepository;
        this.vendorService = vendorService;
        this.productGameService = productGameService;
        this.vendorLineService = vendorLineService;
        this.vendorGameCodeService = vendorGameCodeService;
        this.vendorGameDeactivatedService = vendorGameDeactivatedService;
        this.requestService = requestService;
    }

    public GameUrlData getGameUrl(String gameCode, GameSession gameSession, Map<String, String> credentials,
                                  VendorLine vendorLine, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException {

        GameUrlData gameUrlData = new GameUrlData();
        gameUrlData.setToken(gameSession.getToken());

        try {
            String vendorClassName = vendorService.getById(vendorLine.getVendorId()).getClassName();

            String className = "com.nextgen.gameaggregator.vendor." + vendorClassName + ".api.gameurl.GameUrlService";
            GameUrl gameUrl = (GameUrl) Class.forName(className).getConstructor().newInstance();
            autowireCapableBeanFactory.autowireBean(gameUrl);
            MultiValueMap<String, String> formData = gameUrl.formDataBuilder(gameCode, gameSession, credentials);

            httpRequestLog.setOperatorData(httpRequestLog.getRequestBody());
            httpRequestLog.setRequestBody(new Gson().toJson(formData.toSingleValueMap()));
            long startTime = System.currentTimeMillis();
            httpRequestLog.setBetStart(startTime);

            //GA-9567 Add toggle to skip call to vendor based on player name
            if (requestService.shouldSkipStubCall(gameSession.getAgentPlayerUsername())) {
                gameUrlData.setGameUrl("SkipCallToVendor");
            } else {
                GameUrlVo gameUrlVo = gameUrl.callToVendor(formData, credentials, gameSession, httpRequestLog);

                if (gameUrlVo == null) throw new InvalidVendorResponseException();

                //token will be replaced if vendor's token is needed to verify for action files.
                gameUrlData.setGameUrl(gameUrlVo.getGameUrl());
            }

            gameUrlData.setToken(gameSession.getToken());

            //TODO throw vendor maintenance exception

        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | InvalidVendorLineException | InvalidVendorResponseException |
                 InvalidFormatException | InvalidVendorException | TimeoutException
                gameClassException) {

            throw new InvalidVendorResponseException(gameClassException.getMessage());

        }

        return gameUrlData;
    }

    @Cacheable(value = "Platforms", key = "#platformCode", cacheManager = "cacheManager")
    public Platform checkPlatformCode(String platformCode) throws InvalidPlatformException {
        Platform platform = platformRepository.findByCode(platformCode);
        Optional.ofNullable(platform).orElseThrow(InvalidPlatformException::new);
        return platform;

    }

    public String getVendorPlatformCode(String className, Integer platformId) throws VendorPlatformNotSupportedException {

        //default value
        String vendorPlatformCode = (platformId == 1) ? "H5" : "WEB";

        try {
            String classNamePath = "com.nextgen.gameaggregator.vendor." + className + ".constant.Platforms";
            Class<?> c = Class.forName(classNamePath);
            Field field = c.getField(vendorPlatformCode);
            Object value = field.get(null);
            vendorPlatformCode = value.toString();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new VendorPlatformNotSupportedException();
        }

        return vendorPlatformCode;
    }

    public Currency checkCurrency(String currencyCode) throws InvalidCurrencyException {
        Currency currency = currencyRepository.findByCode(currencyCode);
        Optional.ofNullable(currency).orElseThrow(InvalidCurrencyException::new);
        return currency;
    }

    public void isCurrencySupportedByAgent(Integer agentId, Integer currencyId) throws CurrencyNotSupportedException {
        boolean isSupported = agentService.isCurrencySupportedByAgent(agentId, currencyId);

        if (!isSupported) {
            throw new CurrencyNotSupportedException("AgentId: " + agentId + " CurrencyId: " + currencyId);
        }
    }

    public void checkDuplicateRequest(Integer agentId, String traceId) throws DuplicateRequestException {
        GameSession entity = rawGameSessionRepository.findByAgentIdAndTraceId(agentId, traceId);
        if (entity != null) {
            throw new DuplicateRequestException();
        }
    }

    @Cacheable(value = "AgentPlayers", key = "{#agentId, #username}", cacheManager = "cacheManager")
    public AgentPlayer getOrCreateAgentPlayer(Integer agentId, String username) throws DisabledAgentPlayerException {
        AgentPlayer agentPlayer = agentPlayerRepository.findByAgentIdAndUsername(agentId, username);
        if (agentPlayer == null) {
            agentPlayer = this.createAgentPlayer(agentId, username);
            agentPlayerRepository.save(agentPlayer);
        } else {
            if (agentPlayer.getStatus().equals(Status.INACTIVE.code)) {
                throw new DisabledAgentPlayerException();
            }
        }
        return agentPlayer;
    }

    @Cacheable(value = "VendorPlayers", key = "{#agentPlayer.id, #vendorLine.id, #currencyId}", cacheManager = "cacheManager")
    public VendorPlayer checkVendorPlayer(AgentPlayer agentPlayer, VendorLine vendorLine, Integer currencyId) throws DisabledAgentPlayerException {
        VendorPlayer vendorPlayer = vendorPlayerRepository.findByAgentPlayerIdAndVendorLineIdAndCurrencyId(agentPlayer.getId(), vendorLine.getId(),
                currencyId);

        if (vendorPlayer == null) {
            vendorPlayer = this.createVendorPlayer(agentPlayer.getId(), vendorLine.getId(), vendorLine.getVendorId(), currencyId);
            vendorPlayerRepository.save(vendorPlayer);
        } else {
            if (vendorPlayer.getStatus().equals(Status.INACTIVE.code)) {
                throw new DisabledAgentPlayerException();
            }
        }
        return vendorPlayer;
    }

    public AgentPlayer createAgentPlayer(Integer agentId, String username) {
        AgentPlayer entity = new AgentPlayer();
        entity.setAgentId(agentId);
        entity.setUsername(username);
        entity.setStatus(Status.ACTIVE.code);
        entity.prepareSave(0, USERTYPE);
        return entity;
    }

    public VendorPlayer createVendorPlayer(Long agentPlayerId, Integer vendorLineId, Integer vendorId, Integer currencyId) {

        String vendorPlayerUsername = NameUtils.generateUsername(vendorLineId.longValue(), agentPlayerId)
                + NameUtils.excelColumnNameFormula(currencyId);
        VendorPlayer entity = new VendorPlayer();
        entity.setAgentPlayerId(agentPlayerId);
        entity.setVendorLineId(vendorLineId);
        entity.setVendorId(vendorId);
        entity.setUsername(vendorPlayerUsername);
        entity.setStatus(Status.ACTIVE.code);
        entity.setCurrencyId(currencyId);
        entity.prepareSave(0, USERTYPE);
        return entity;
    }

    public GameLaunchDto launchByProductGame(GameLaunchDto gameLaunchDto, ProductGame productGame) throws
            ProductCombinationNotSupportedException, ProductVendorLineNotFoundException,
            InvalidVendorLineException, DisabledVendorLineException, DisabledGameException, GameNotSupportedException {

        Integer productGameId = productGame.getId();
        Integer productId = productGame.getProductId();
        Integer gameCategoryId = productGame.getGameCategoryId();
        Integer currencyId = gameLaunchDto.getCurrencyId();
        Integer agentId = gameLaunchDto.getAgentId();
        Integer platformId = gameLaunchDto.getPlatformId();

        Integer vendorLineId = agentProductService.getProductVendorLineIdByAgent(productId, gameCategoryId, currencyId, agentId);
        VendorLine vendorLine = vendorLineService.getVendorLine(vendorLineId);
        vendorLineService.checkStatus(vendorLine);
        Integer vendorId = vendorLine.getVendorId();

        gameLaunchDto.setProductId(productId);
        gameLaunchDto.setProductGameId(productGameId);
        gameLaunchDto.setGameCategoryId(gameCategoryId);
        gameLaunchDto.setVendorId(vendorId);
        gameLaunchDto.setVendorLine(vendorLine);
        gameLaunchDto.setVendorLineId(vendorLineId);

        VendorGameCode vendorGameCode = vendorGameCodeService.getByProductGame(productGameId, vendorId, platformId, gameLaunchDto.getLanguageId());
        vendorGameCodeService.checkGameStatus(vendorGameCode);

        boolean gameDeactivated = productGameService.isGameDeactivated(productGameId, agentId, gameLaunchDto.getMasterAgentId(), gameLaunchDto.getHouseId());
        if (gameDeactivated) {
            throw new DisabledGameException();
        }
        gameLaunchDto.setOpenGameCode(vendorGameCode.getOpenGameCode());

        return gameLaunchDto;
    }

    // For backward compatibility, will be removed once all vendors are migrated to use product games
    public GameLaunchDto launchByVendorGame(GameLaunchDto gameLaunchDto, VendorGame vendorGame) throws
            GameNotSupportedException, GameCurrencyNotSupportException, InvalidVendorLineException,
            DisabledVendorLineException, DisabledGameException {

        Integer gameCategoryId = vendorGame.getGameCategoryId();
        Integer currencyId = gameLaunchDto.getCurrencyId();
        Integer agentId = gameLaunchDto.getAgentId();
        Integer vendorId = vendorGame.getVendorId();
        Integer platformId = gameLaunchDto.getPlatformId();
        Integer languageId = gameLaunchDto.getLanguageId();
        Integer vendorGameId = gameLaunchDto.getVendorGameId();

        gameLaunchDto.setVendorId(vendorId);
        gameLaunchDto.setVendorGameId(vendorGame.getId());
        gameLaunchDto.setGameCategoryId(gameCategoryId);

        VendorGameCode vendorGameCode = vendorGameCodeService.getByVendorGame(vendorGameId, platformId, languageId);
        vendorGameCodeService.checkGameStatus(vendorGameCode);

        VendorGameCurrency vendorGameCurrency = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyId(vendorGameId, currencyId);

        if (vendorGameCurrency == null || vendorGameCurrency.getStatus() == 0) {
            throw new GameCurrencyNotSupportException();
        }

        gameLaunchDto.setOpenGameCode(vendorGameCode.getOpenGameCode());

        // Check if is game deactivated (agent, masterAgent, house level)
        vendorGameDeactivatedService.checkGameSupported(gameLaunchDto.getHouseId(), gameLaunchDto.getMasterAgentId(), gameLaunchDto.getAgentId(), gameLaunchDto.getVendorGameId());

        // Retrieve vendor line credentials by category
        AgentVendorLine agentVendorLine = agentService.getActiveVendorLine(agentId, vendorId, currencyId, gameCategoryId);
        if (agentVendorLine == null) throw new DisabledVendorLineException();

        Integer vendorLineId = agentVendorLine.getVendorLineId();
        VendorLine vendorLine = vendorLineService.getVendorLine(vendorLineId);
        vendorLineService.checkStatus(vendorLine);

        gameLaunchDto.setVendorLine(vendorLine);
        gameLaunchDto.setVendorLineId(vendorLine.getId());

        return gameLaunchDto;
    }

    public GameLaunchDto populateVendorParams(GameLaunchDto gameLaunchDto) throws
            VendorLanguageNotSupportedException, VendorCurrencyNotSupportException, InvalidVendorException,
            VendorPlatformNotSupportedException {

        Integer vendorId = gameLaunchDto.getVendorId();
        Integer currencyId = gameLaunchDto.getCurrencyId();
        Integer platformId = gameLaunchDto.getPlatformId();

        VendorLanguageCode vendorLanguageCode = vendorService.findVendorLanguageCode(vendorId, gameLaunchDto.getLanguageId());
        gameLaunchDto.setVendorLanguageCode(vendorLanguageCode.getLanguageCode());

        VendorCurrency vendorCurrency = vendorService.findVendorCurrency(vendorId, currencyId);
        gameLaunchDto.setVendorCurrencyCode(vendorCurrency.getVendorCurrencyCode());

        Vendor vendor = vendorService.getById(vendorId);
        String vendorPlatformCode = this.getVendorPlatformCode(vendor.getClassName(), platformId);
        gameLaunchDto.setVendorPlatformCode(vendorPlatformCode);

        return gameLaunchDto;
    }
}
