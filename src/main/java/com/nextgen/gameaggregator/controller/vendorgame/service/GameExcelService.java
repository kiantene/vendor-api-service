package com.nextgen.gameaggregator.controller.vendorgame.service;

import com.nextgen.gameaggregator.controller.vendorgame.vo.GameDataEntity;
import com.nextgen.gameaggregator.controller.vendorgame.vo.ImportResponse;
import com.nextgen.gameaggregator.controller.vendorgame.enums.HeaderName;
import com.nextgen.gameaggregator.entity.Currency;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.repository.VendorGameCodeRepository;
import com.nextgen.gameaggregator.repository.VendorGameCurrencyRepository;
import com.nextgen.gameaggregator.repository.VendorGameRepository;
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

    private static final String USERTYPE = "vendor-api-service";

    public static boolean hasExcelFormat(MultipartFile file) {

        if (!TYPE.equals(file.getContentType())) {
            return false;
        }

        return true;
    }

    public Iterator<Row> readContent(InputStream is) throws IOException, InvalidRequestException {
        Workbook workbook = new XSSFWorkbook(is);

        Sheet sheet = workbook.getSheet(SHEET);
        Optional.ofNullable(sheet).orElseThrow(() -> new InvalidRequestException("Sheet :" + SHEET + " not found!"));
        return sheet.iterator();
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
        gameDataEntity.getVendorGame().setVendor(vendor);
        gameDataEntity.getVendorGame().setGameCategory(gameCategory);
        gameDataEntity.getVendorGame().setStatus(1);
        gameDataEntity.getVendorGame().setIsByCurrency(0);
        gameDataEntity.getVendorGame().prepareSave(0, USERTYPE);

        VendorGame vendorGameExist = vendorGameRepository.findByCode(gameDataEntity.getVendorGame().getCode());

        if (vendorGameExist != null) {
            gameDataEntity.getVendorGame().setId(vendorGameExist.getId());
        }
        VendorGame vendorGame = vendorGameRepository.save(gameDataEntity.getVendorGame());

        List<VendorGameCode> VendorGameCodes = this.generateVendorGameCodeListByPlatform(
                gameDataEntity.getVendorGameCodes(), gameDataEntity.getPlatformSupported(), vendor, vendorGame,
                gameDataEntity.getDefaultOpenGameCode(), gameDataEntity.getDefaultBetGameCode());

        vendorGameCodeRepository.saveAll(VendorGameCodes);

        List<VendorGameCurrency> VendorGameCurrencies = this.generateVendorGameCurrencyList(
                gameDataEntity.getVendorGameCurrencies(), vendorGame);

        vendorGameCurrencyRepository.saveAll(VendorGameCurrencies);
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

                VendorGameCode vendorGameCode = new VendorGameCode();
                vendorGameCode.setVendorGame(vendorGame);
                vendorGameCode.setVendor(vendor);
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


                VendorGameCode vendorGameCodeExist = vendorGameCodeRepository.findByVendorGameIdAndPlatformIdAndLanguageId(
                        vendorGame.getId(), platform.getId(), vendorGameCodeLang.getLanguageId());

                if (vendorGameCodeExist != null) {
                    vendorGameCode.setId(vendorGameCodeExist.getId());
                }

                vendorGameCode.prepareSave(0, USERTYPE);
                vendorGameCodes.add(vendorGameCode);

            }
        }

        return vendorGameCodes;

    }


    private List<VendorGameCurrency> generateVendorGameCurrencyList(
            HashMap<String, VendorGameCurrency> VendorGameCurrencyMap, VendorGame vendorGame) {
        List<VendorGameCurrency> VendorGameCurrencies = new ArrayList<>();

        for (Map.Entry<String, VendorGameCurrency> vendorGameCurrencyValue : VendorGameCurrencyMap.entrySet()) {
            VendorGameCurrency vendorGameCurrency = vendorGameCurrencyValue.getValue();

            vendorGameCurrency.setVendorGame(vendorGame);

            VendorGameCurrency vendorGameCurrencyExist = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyId(
                    vendorGame.getId(), vendorGameCurrency.getCurrency().getId());

            if (vendorGameCurrencyExist != null) {
                vendorGameCurrency.setId(vendorGameCurrencyExist.getId());
            }

            vendorGameCurrency.prepareSave(0, USERTYPE);
            VendorGameCurrencies.add(vendorGameCurrency);
        }

        return VendorGameCurrencies;

    }
}
