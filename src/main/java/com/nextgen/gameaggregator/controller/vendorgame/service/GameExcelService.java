package com.nextgen.gameaggregator.controller.vendorgame.service;

import com.nextgen.gameaggregator.controller.vendorgame.enums.HeaderName;
import com.nextgen.gameaggregator.controller.vendorgame.vo.GameDataEntity;
import com.nextgen.gameaggregator.controller.vendorgame.vo.ImportResponse;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameCodeRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameCurrencyRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class GameExcelService {

    private static final String USERTYPE = "vendor-api-service";
    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static String SHEET = "games";
    @Autowired
    GameExcelValidatorService gameExcelValidatorService;
    @Autowired
    VendorGameRepository vendorGameRepository;
    @Autowired
    VendorGameCodeRepository vendorGameCodeRepository;
    @Autowired
    VendorGameCurrencyRepository vendorGameCurrencyRepository;

    public static boolean hasExcelFormat(MultipartFile file) {

        if (!TYPE.equals(file.getContentType())) {
            return false;
        }

        return true;
    }

    public Iterator<Row> readContent(InputStream is) throws IOException, InvalidRequestException {
        Workbook workbook = new XSSFWorkbook(is);

        Sheet sheet = workbook.getSheet(SHEET);

        return Objects.requireNonNull(sheet, "Sheet :" + SHEET + " not found!").iterator();
    }

    public ImportResponse saveRows(Iterator<Row> rows, HashMap<Integer, HashMap<String, String>> columnTypes,
                                   HashMap<String, Language> vendorLanguages, HashMap<String, Currency> vendorCurrencies,
                                   Vendor vendor, GameCategory gameCategory, HashMap<String, Platform> platforms) {

        ImportResponse responseVo = new ImportResponse();

        int rowNum = 3;
        //begin loop from row 4
        while (rows.hasNext()) {
            Boolean proceedSave = true;
            Row currentRow = rows.next();

            Iterator<Cell> cellsInRow = currentRow.iterator();
            GameDataEntity gameDataEntity = new GameDataEntity();
            int columnNum = 0;
            while (cellsInRow.hasNext()) {

                Cell currentCell = cellsInRow.next();

                //skip prepare data for first column is empty
                if ((columnNum == 0) && (currentCell.getStringCellValue() == "")) {
                    proceedSave = false;
                    break;
                }

                try {
                    if (columnTypes.get(columnNum).get("dataType").equals(HeaderName.GAME_NAME.name)) {
                        gameDataEntity = gameExcelValidatorService.verifyGameName(gameDataEntity, vendorLanguages, columnTypes,
                                columnNum, rowNum, currentCell.getStringCellValue());
                    } else if (columnTypes.get(columnNum).get("dataType").equals(HeaderName.OPEN_GAME_CODE.name)) {
                        gameDataEntity = gameExcelValidatorService.verifyOpenGameCode(gameDataEntity, vendorLanguages, columnTypes,
                                columnNum, rowNum, currentCell.getStringCellValue());
                    } else if (columnTypes.get(columnNum).get("dataType").equals(HeaderName.BET_GAME_CODE.name)) {
                        gameDataEntity = gameExcelValidatorService.verifyBetGameCode(gameDataEntity, vendorLanguages, columnTypes,
                                columnNum, rowNum, currentCell.getStringCellValue());
                    } else if (columnTypes.get(columnNum).get("dataType").equals(HeaderName.SQUARE_IMAGE_NAME.name)) {
                        gameDataEntity = gameExcelValidatorService.verifySquareImage(gameDataEntity, vendorLanguages, columnTypes,
                                columnNum, rowNum, currentCell.getStringCellValue(), vendor, gameCategory);
                    } else if (columnTypes.get(columnNum).get("dataType").equals(HeaderName.SUPPORT_PLATFORM.name)) {
                        gameDataEntity = gameExcelValidatorService.verifyPlatform(gameDataEntity, platforms, columnTypes,
                                columnNum, rowNum, currentCell.getStringCellValue());
                    } else if (columnTypes.get(columnNum).get("dataType").equals(HeaderName.SUPPORT_LANGUAGE.name)) {
                        gameDataEntity = gameExcelValidatorService.verifyLanguage(gameDataEntity, vendorLanguages, columnTypes,
                                columnNum, rowNum, currentCell.getStringCellValue());
                    } else if (columnTypes.get(columnNum).get("dataType").equals(HeaderName.SUPPORT_CURRENCY.name)) {
                        gameDataEntity = gameExcelValidatorService.verifyCurrency(gameDataEntity, vendorCurrencies, columnTypes,
                                columnNum, rowNum, currentCell.getStringCellValue());
                    }
                } catch (Exception exception) {
                    proceedSave = false;
                    responseVo.getResult().put("Cell " + gameExcelValidatorService.getCellName(rowNum, columnNum), exception.getMessage());
                    responseVo.setTotalFail(responseVo.getTotalFail() + 1);
                    break;
                }

                columnNum++;
            }
            if (proceedSave) {
                if (this.saveGameData(gameDataEntity, vendor, gameCategory)) {
                    responseVo.setTotalSuccess(responseVo.getTotalSuccess() + 1);
                }
            }

            rowNum++;

        }


        return responseVo;

    }

    private Boolean saveGameData(GameDataEntity gameDataEntity, Vendor vendor, GameCategory gameCategory) {

        gameDataEntity.getVendorGame().setCode(vendor.getCode() + "_" + gameDataEntity.getVendorGame().getVendorGameCode());

        VendorGame vendorGameExist = vendorGameRepository.findByCode(gameDataEntity.getVendorGame().getCode());

        if (Objects.nonNull(vendorGameExist)) {
            gameDataEntity.setVendorGame(vendorGameExist);
        } else {
            gameDataEntity.getVendorGame().setVendorId(vendor.getId());
            gameDataEntity.getVendorGame().setGameCategoryId(gameCategory.getId());
            gameDataEntity.getVendorGame().setStatus(1);
            gameDataEntity.getVendorGame().setIsByCurrency(0);
            gameDataEntity.getVendorGame().setBetDataPreprocessing(0);
            gameDataEntity.getVendorGame().prepareSave(0, USERTYPE);
        }

        List<VendorGameCode> vendorGameCodes = this.generateVendorGameCodeListByPlatform(
                gameDataEntity.getVendorGameCodes(), gameDataEntity.getPlatformSupported(), vendor, gameDataEntity.getVendorGame(),
                gameDataEntity.getDefaultOpenGameCode(), gameDataEntity.getDefaultBetGameCode());

        List<VendorGameCurrency> vendorGameCurrencies = this.generateVendorGameCurrencyList(
                gameDataEntity.getVendorGameCurrencies(), gameDataEntity.getVendorGame());

        if (vendorGameCodes.stream().noneMatch(value -> value.getStatus().equals(1)) && vendorGameCurrencies.stream().noneMatch(value -> value.getStatus().equals(1))) {
            gameDataEntity.getVendorGame().setStatus(0);
        }

        vendorGameRepository.save(gameDataEntity.getVendorGame());
        vendorGameCodeRepository.saveAll(vendorGameCodes);
        vendorGameCurrencyRepository.saveAll(vendorGameCurrencies);

        return true;
    }

    private List<VendorGameCode> generateVendorGameCodeListByPlatform(
            HashMap<String, VendorGameCode> vendorGameCodeMap,
            HashMap<String, Platform> platformSupportedMap, Vendor vendor, VendorGame vendorGame,
            String defaultOpenGameCode, String defaultBetGameCode) {

        List<VendorGameCode> vendorGameCodes = new ArrayList<>();

        for (Map.Entry<String, Platform> platformMapValue : platformSupportedMap.entrySet()) {

            Platform platform = platformMapValue.getValue();
            for (Map.Entry<String, VendorGameCode> vendorGameCodeValue : vendorGameCodeMap.entrySet()) {

                VendorGameCode vendorGameCodeLang = vendorGameCodeValue.getValue();

                VendorGameCode vendorGameCodeExist = vendorGameCodeRepository.findByVendorGameIdAndPlatformIdAndLanguageId(
                        vendorGame.getId(), platform.getId(), vendorGameCodeLang.getLanguageId());

                VendorGameCode vendorGameCode = new VendorGameCode();

                if (Objects.nonNull(vendorGameCodeExist)) {
                    vendorGameCode = vendorGameCodeExist;
                } else {
                    vendorGameCode.setVendorGame(vendorGame);
                    vendorGameCode.setVendorId(vendor.getId());
                    vendorGameCode.setName(vendorGameCodeLang.getName());
                    vendorGameCode.setImageSquare(vendorGameCodeLang.getImageSquare());

                    vendorGameCode.setOpenGameCode(
                            (vendorGameCodeLang.getOpenGameCode() == null) ? defaultOpenGameCode : vendorGameCodeLang.getOpenGameCode());
                    vendorGameCode.setBetGameCode(
                            (vendorGameCodeLang.getBetGameCode() == null) ? defaultBetGameCode : vendorGameCodeLang.getBetGameCode());
                    vendorGameCode.setLanguageId(vendorGameCodeLang.getLanguageId());
                    vendorGameCode.setPlatformId(platform.getId());
                    //default status as false if platform is disable
                    vendorGameCode.setStatus(platform.getStatus().equals(0) ? 0 : vendorGameCodeLang.getStatus());

                    vendorGameCode.prepareSave(0, USERTYPE);
                }

                vendorGameCodes.add(vendorGameCode);

            }
        }

        return vendorGameCodes;

    }


    private List<VendorGameCurrency> generateVendorGameCurrencyList(HashMap<String, VendorGameCurrency> vendorGameCurrencyHashMap, VendorGame vendorGame) {
        List<VendorGameCurrency> vendorGameCurrencies = new ArrayList<>();

        for (Map.Entry<String, VendorGameCurrency> vendorGameCurrencyValue : vendorGameCurrencyHashMap.entrySet()) {
            VendorGameCurrency vendorGameCurrency = vendorGameCurrencyValue.getValue();

            vendorGameCurrency.setVendorGame(vendorGame);

            VendorGameCurrency vendorGameCurrencyExist = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyId(
                    vendorGame.getId(), vendorGameCurrency.getCurrency().getId());

            if (Objects.nonNull(vendorGameCurrencyExist)) {
                vendorGameCurrency = vendorGameCurrencyExist;
            } else {
                vendorGameCurrency.prepareSave(0, USERTYPE);
            }

            vendorGameCurrencies.add(vendorGameCurrency);
        }

        return vendorGameCurrencies;

    }
}
