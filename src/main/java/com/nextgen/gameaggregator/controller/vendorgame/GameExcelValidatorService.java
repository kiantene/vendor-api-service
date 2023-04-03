package com.nextgen.gameaggregator.controller.vendorgame;

import com.nextgen.gameaggregator.controller.vendorgame.enums.HeaderName;
import com.nextgen.gameaggregator.entity.Currency;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.repository.CurrencyRepository;
import com.nextgen.gameaggregator.repository.LanguageRepository;
import com.nextgen.gameaggregator.repository.PlatformRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameExcelValidatorService {

    private static final String[] platformList = {"WEB", "H5"};
    private static final String[] platformId = {"1", "2"};

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private LanguageRepository languageRepository;

    @Autowired
    private PlatformRepository platformRepository;

    public HashMap<Integer, HashMap<String, String>> validateHeader(Iterator<Row> rows, HashMap<String, Platform> platforms ) throws InvalidFormatException {

        HashMap<String, String> currencies = this.getSystemSupportedCurrency();
        HashMap<String, String> languages = this.getSystemSupportedLanguage();


        HashMap<String, Boolean> compulsoryHeaders = this.compulsoryHeader();

        Row headerRow1 = null;
        ArrayList<String> headers = this.headerlist();

        HashMap<Integer, HashMap<String, String>> columnTypes = new HashMap<>();

        int rowNum = 0;
        while (rows.hasNext()) {

            Row currentRow = rows.next();
            Iterator<Cell> cellsInRow = currentRow.iterator();

            int columnNum = 0;
            while (cellsInRow.hasNext()) {
                Cell currentCell = cellsInRow.next();
                //check is the first row header match with the key format provided
                if (rowNum == 0) {
                    if (!headers.contains(currentCell.getStringCellValue())) {
                        throw new InvalidFormatException("Cell [" + this.getCellName(rowNum, columnNum) + "] is Not Valid Header: " + currentCell.getStringCellValue());
                    }
                }

                if (rowNum == 2) {
                    HashMap<String, String> columnsType =  this.validateHeaderType(
                            headerRow1.getCell(columnNum).getStringCellValue(), currentCell.getStringCellValue(),
                            (rowNum), (columnNum), currencies, languages, platforms);
                    columnTypes.put(columnNum,columnsType);

                    //update compulsory Header exist status
                    if(compulsoryHeaders.containsKey(columnsType.get("dataType")+ "_"+columnsType.get("code"))){
                        compulsoryHeaders.put(columnsType.get("dataType")+ "_"+columnsType.get("code"), true);
                    }

                }
                columnNum++;
            }

            if (rowNum == 0) {
                headerRow1 = currentRow;
            }
            if (rowNum == 2) {
                break;
            }
            rowNum++;
        }

        for (Map.Entry<String, Boolean> compulsoryHeader : compulsoryHeaders.entrySet()) {

            if(!compulsoryHeader.getValue()){
                String[] headerValue = compulsoryHeader.getKey().split("_");
                throw new InvalidFormatException( headerValue[0] +" Header with ALL type is compulsory");
            }

        }

        return columnTypes;
    }


    private HashMap<String, String> validateHeaderType(String header1, String header3, Integer rowNum, Integer columnNum,
                                                       HashMap<String, String> currencies,
                                                       HashMap<String, String> languages,
                                                       HashMap<String, Platform> platforms)
            throws InvalidFormatException {

        ArrayList<String> headers = this.headerlist();
        String[] keyInfo = this.keyValue(header1, header3, rowNum, columnNum);
        //validate game name, open game code, square image name, support language
        if (this.supportLanguageHeader().contains(header1)) {
            String languageId = (String) languages.get(keyInfo[0].toLowerCase());
            if (!keyInfo[0].equals("ALL")) {
                if (languageId == null) {
                    throw new InvalidFormatException("Cell [" + this.getCellName(rowNum, columnNum) + "] is Not Valid Language: " + header1 + "=" + header3);
                } else if (!languageId.equals(keyInfo[1])) {
                    throw new InvalidFormatException("Cell [" + this.getCellName(rowNum, columnNum) + "] is Not Valid Language: " + header1 + "=" + header3);
                }
            }
            // validate support currency
        } else if (this.supportCurrencyHeader().contains(header1)) {
            String currencyId = (String) currencies.get(keyInfo[0].toLowerCase());
            if (currencyId == null) {
                throw new InvalidFormatException("Cell [" + this.getCellName(rowNum, columnNum) + "] is Not Valid Currency: " + header1 + "=" + header3);
            } else if (!currencyId.equals(keyInfo[1])) {
                throw new InvalidFormatException("Cell [" + this.getCellName(rowNum, columnNum) + "] is Not Valid Currency: " + header1 + "=" + header3);
            }
            //validate support platform
        } else if (this.supportPlatformHeader().contains(header1)) {
            Platform platform =  platforms.get(keyInfo[0].toLowerCase());
            if (platform == null) {
                throw new InvalidFormatException("Cell [" + this.getCellName(rowNum, columnNum) + "] is Not Valid Platform: " + header1 + "=" + header3);
            } else if (!keyInfo[1].equals(platform.getId().toString())) {
                throw new InvalidFormatException("Cell [" + this.getCellName(rowNum, columnNum) + "] is Not Valid Platform: " + header1 + "=" + header3);
            }
        }

        HashMap<String, String> columnType = new HashMap<>();
        columnType.put("dataType", header1);
        columnType.put("code", keyInfo[0]);
        columnType.put("id", keyInfo[1]);
        columnType.put("headerCode", header3);

        return columnType;
    }

    private String[] keyValue(String header1, String header3, Integer rowNum, Integer columnNum) throws InvalidFormatException {
        String[] keyInfo;

        //the value is for default type
        if (header3.equals("ALL")) {
            keyInfo = new String[]{"ALL", "ALL"};
        } else {
            keyInfo = header3.trim().split(" ");
            //the format is not match with the header 3 template  e.g WEB [2] , Zh [1]
            if (keyInfo.length != 2) {
                throw new InvalidFormatException("Cell Name\" " + this.getCellName(rowNum, columnNum) + " \" is Not Valid :" + header1 + "=" + header3);
            }
            //remove [] from key
            keyInfo[1] = keyInfo[1].replaceAll("[\\[\\]]", "");

            if (!keyInfo[1].chars().allMatch(Character::isDigit)) {
                throw new InvalidFormatException("Cell Name\" " + this.getCellName(rowNum, columnNum) + " \" is Not Valid :" + header1 + "=" + header3);
            }
        }

        return keyInfo;

    }

    public String[] checkFileName(String fileName) throws InvalidFormatException {
        String[] vendorInfo = fileName.split("_");

        if (vendorInfo.length != 2) {
            throw new InvalidFormatException("file name should be VendorCode + GameCategoryCode, e.g. PP_SLOT");
        }
        return vendorInfo;

    }

    //region fetch system supported value
    public HashMap<String, String> getSystemSupportedCurrency() {
        List<Currency> currencies = currencyRepository.findAll();

        HashMap<String, String> currencyMap = new HashMap<>();
        for (Currency currency : currencies) {
            currencyMap.put(currency.getCode().toLowerCase(), currency.getId().toString());
        }
        return currencyMap;
    }

    public HashMap<String, String> getSystemSupportedLanguage() {
        List<Language> languages = languageRepository.findAll();

        HashMap<String, String> languageMap = new HashMap<>();
        for (Language language : languages) {
            languageMap.put(language.getCode().toLowerCase(), language.getId().toString());
        }
        return languageMap;
    }

    public HashMap<String, Platform> getSystemSupportedPlatform() {
        List<Platform> platforms = platformRepository.findAll();

        HashMap<String, Platform> platformMaps = new HashMap<>();

        for (Platform platform: platforms) {
            platformMaps.put(platform.getCode().toLowerCase(), platform);
        }

//
//        HashMap<String, String> platformMap = new HashMap<>();
//        for (Platform platform : platforms) {
//            platformMap.put(platform.getCode().toLowerCase(), platform.getId().toString());
//        }
        return platformMaps;

    }
    //endregion


    public String getCellName(Integer rowNum, Integer columnNum) {
        return new CellAddress(rowNum, columnNum).toString();
    }

    //region header titlte
    private ArrayList<String> headerlist() {
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(this.betCodeHeader());
        list.addAll(this.supportPlatformHeader());
        list.addAll(this.supportLanguageHeader());
        list.addAll(this.supportCurrencyHeader());

        return list;
    }

    private ArrayList<String> betCodeHeader() {
        ArrayList<String> list = new ArrayList<String>();
        list.add(HeaderName.BET_GAME_CODE.name);
        return list;
    }

    private ArrayList<String> supportPlatformHeader() {
        ArrayList<String> list = new ArrayList<String>();
        list.add(HeaderName.SUPPORT_PLATFORM.name);
        return list;
    }

    private ArrayList<String> supportLanguageHeader() {

        ArrayList<String> list = new ArrayList<String>();
        list.add(HeaderName.GAME_NAME.name);
        list.add(HeaderName.OPEN_GAME_CODE.name);
        list.add(HeaderName.SQUARE_IMAGE_NAME.name);
        list.add(HeaderName.SUPPORT_LANGUAGE.name);
        return list;
    }

    private ArrayList<String> supportCurrencyHeader() {
        ArrayList<String> list = new ArrayList<String>();
        list.add(HeaderName.SUPPORT_CURRENCY.name);
        return list;
    }

    private HashMap<String, Boolean> compulsoryHeader(){
        HashMap<String, Boolean> compulsoryHeaderMap = new HashMap<>();

        compulsoryHeaderMap.put(HeaderName.GAME_NAME.name + "_ALL", false);
        compulsoryHeaderMap.put(HeaderName.OPEN_GAME_CODE.name + "_ALL", false);
        compulsoryHeaderMap.put(HeaderName.BET_GAME_CODE.name + "_ALL", false);
        compulsoryHeaderMap.put(HeaderName.SQUARE_IMAGE_NAME.name + "_ALL", false);

        return compulsoryHeaderMap;

    }
    //endregion


    //region validate row value
    public GameDataEntity verifyGameName(GameDataEntity gameDataEntity, HashMap<String, Language> vendorLanguages,
                                          HashMap<Integer, HashMap<String, String>> columnTypes,
                                          Integer columnNum, Integer rowNum, String name) throws InvalidFormatException {
        if (columnTypes.get(columnNum).get("id").equals("ALL")) {
            //set default game name
            gameDataEntity.getVendorGame().setName(name.trim());
        } else if (vendorLanguages.get(columnTypes.get(columnNum).get("id")) != null) {
            VendorGameCode vendorGameCode =
                    this.getDataEntityVendorGameCode(columnTypes.get(columnNum).get("id"), gameDataEntity);
            //set game name based on language
            vendorGameCode.setName(name.trim());
            vendorGameCode.setLanguageId(Integer.parseInt(columnTypes.get(columnNum).get("id")));
            gameDataEntity.getVendorGameCodes().put(columnTypes.get(columnNum).get("id"), vendorGameCode);

        } else if (vendorLanguages.get(columnTypes.get(columnNum).get("id")) == null) {
            throw new InvalidFormatException(" Vendor Not Supported Game Name with language: " + columnTypes.get(columnNum).get("headerCode"));
        }
        return gameDataEntity;
    }


    public GameDataEntity verifyOpenGameCode(GameDataEntity gameDataEntity, HashMap<String, Language> vendorLanguages,
                                              HashMap<Integer, HashMap<String, String>> columnTypes,
                                              Integer columnNum, Integer rowNum, String openGameCode) throws InvalidFormatException {
        //TODO remove vendor game code from vendor game table
        if (columnTypes.get(columnNum).get("id").equals("ALL")) {
            //set default open game code
            gameDataEntity.getVendorGame().setVendorGameCode(openGameCode.trim());
            gameDataEntity.setDefaultOpenGameCode(openGameCode.trim());

        } else if (vendorLanguages.get(columnTypes.get(columnNum).get("id")) != null) {
            VendorGameCode vendorGameCode =
                    this.getDataEntityVendorGameCode(columnTypes.get(columnNum).get("id"), gameDataEntity);
            //set vendor Game Code base on language
            vendorGameCode.setOpenGameCode(openGameCode.trim());
            vendorGameCode.setLanguageId(Integer.parseInt(columnTypes.get(columnNum).get("id")));
            gameDataEntity.getVendorGameCodes().put(columnTypes.get(columnNum).get("id"), vendorGameCode);

        } else if (vendorLanguages.get(columnTypes.get(columnNum).get("id")) == null) {
            throw new InvalidFormatException("Vendor Not Supported Open Game Code with language: " + columnTypes.get(columnNum).get("headerCode"));
        }
        return gameDataEntity;
    }


    public GameDataEntity verifyBetGameCode(GameDataEntity gameDataEntity, HashMap<String, Language> vendorLanguages,
                                             HashMap<Integer, HashMap<String, String>> columnTypes,
                                             Integer columnNum, Integer rowNum, String betGameCode) throws InvalidFormatException {
        //TODO remove Open Game Code default
        if (columnTypes.get(columnNum).get("id").equals("ALL")) {
            gameDataEntity.setDefaultBetGameCode(betGameCode.trim());
        } else if (vendorLanguages.get(columnTypes.get(columnNum).get("id")) != null) {
            VendorGameCode vendorGameCode =
                    this.getDataEntityVendorGameCode(columnTypes.get(columnNum).get("id"), gameDataEntity);
            //set Bet Game Code based on language
            vendorGameCode.setBetGameCode(betGameCode.trim());
            vendorGameCode.setLanguageId(Integer.parseInt(columnTypes.get(columnNum).get("id")));
            gameDataEntity.getVendorGameCodes().put(columnTypes.get(columnNum).get("id"), vendorGameCode);

        } else if (vendorLanguages.get(columnTypes.get(columnNum).get("id")) == null) {
            throw new InvalidFormatException(" Vendor Not Supported Bet Game Code with language: " + columnTypes.get(columnNum).get("headerCode"));
        }
        return gameDataEntity;
    }

    public GameDataEntity verifySquareImage(GameDataEntity gameDataEntity, HashMap<String, Language> vendorLanguages,
                                             HashMap<Integer, HashMap<String, String>> columnTypes,
                                             Integer columnNum, Integer rowNum, String squareImage) throws InvalidFormatException {
        if (columnTypes.get(columnNum).get("id").equals("ALL")) {
            //set default square image
            gameDataEntity.getVendorGame().setImageSquare(squareImage.trim());
        } else if (vendorLanguages.get(columnTypes.get(columnNum).get("id")) != null) {
            VendorGameCode vendorGameCode =
                    this.getDataEntityVendorGameCode(columnTypes.get(columnNum).get("id"), gameDataEntity);
            //set square image base on language
            vendorGameCode.setImageSquare(squareImage.trim());
            vendorGameCode.setLanguageId(Integer.parseInt(columnTypes.get(columnNum).get("id")));
            gameDataEntity.getVendorGameCodes().put(columnTypes.get(columnNum).get("id"), vendorGameCode);

        } else if (vendorLanguages.get(columnTypes.get(columnNum).get("id")) == null) {
            throw new InvalidFormatException("Vendor Not Supported Square Image with language: " + columnTypes.get(columnNum).get("headerCode"));
        }
        return gameDataEntity;
    }

    public GameDataEntity verifyPlatform(GameDataEntity gameDataEntity, HashMap<String, Platform> platforms,
                                         HashMap<Integer, HashMap<String, String>> columnTypes,
                                          Integer columnNum, Integer rowNum, String supportedPlatform) throws InvalidFormatException {

        if ((!supportedPlatform.equalsIgnoreCase("no")) && (!supportedPlatform.equalsIgnoreCase("yes"))) {
            throw new InvalidFormatException("Vendor Supported Platform " + columnTypes.get(columnNum).get("headerCode") + " value not valid: " + supportedPlatform);
        } else{
            Platform platform =  platforms.get(columnTypes.get(columnNum).get("code").toLowerCase());
            platform.setStatus((supportedPlatform.equalsIgnoreCase("yes")) ? 1: 0);
            gameDataEntity.getPlatformSupported().put(
                    columnTypes.get(columnNum).get("code").toLowerCase(), platform);
        }

        return gameDataEntity;
    }

    public GameDataEntity verifyLanguage(GameDataEntity gameDataEntity, HashMap<String, Language> vendorLanguages,
                                          HashMap<Integer, HashMap<String, String>> columnTypes,
                                          Integer columnNum, Integer rowNum, String supportedLanguage) throws InvalidFormatException {
        //verify the language value with "yes" and "not" only
        if ((!supportedLanguage.equalsIgnoreCase("no")) && (!supportedLanguage.equalsIgnoreCase("yes"))) {
            throw new InvalidFormatException("Vendor Supported Language " + columnTypes.get(columnNum).get("headerCode") + " value not valid: " + supportedLanguage);
            //verify if vendor supported language not found and value still "yes"
        } else if ((vendorLanguages.get(columnTypes.get(columnNum).get("id")) == null) && (supportedLanguage.equalsIgnoreCase("yes"))) {
            throw new InvalidFormatException("Vendor Not Supported with language: " + columnTypes.get(columnNum).get("headerCode"));
        } else if (vendorLanguages.get(columnTypes.get(columnNum).get("id")) != null) {
            VendorGameCode vendorGameCode =
                    this.getDataEntityVendorGameCode(columnTypes.get(columnNum).get("id"), gameDataEntity);
            //set vendor Game Code base on language
            vendorGameCode.setStatus((supportedLanguage.equalsIgnoreCase("yes") ? 1 : 0));
            vendorGameCode.setLanguageId(Integer.parseInt(columnTypes.get(columnNum).get("id")));
            gameDataEntity.getVendorGameCodes().put(columnTypes.get(columnNum).get("id"), vendorGameCode);
        }
        return gameDataEntity;
    }

    public GameDataEntity verifyCurrency(GameDataEntity gameDataEntity, HashMap<String, Currency> vendorCurrencies,
                                          HashMap<Integer, HashMap<String, String>> columnTypes,
                                          Integer columnNum, Integer rowNum, String supportedCurrency) throws InvalidFormatException {
        //verify the language value with "yes" and "not" only
        if ((!supportedCurrency.equalsIgnoreCase("no")) && (!supportedCurrency.equalsIgnoreCase("yes"))) {
            throw new InvalidFormatException("Vendor Supported Currency " + columnTypes.get(columnNum).get("headerCode") + " value not valid: " + supportedCurrency);
            //verify if vendor supported language not found and value still "yes"
        } else if ((vendorCurrencies.get(columnTypes.get(columnNum).get("id")) == null) && (supportedCurrency.equalsIgnoreCase("yes"))) {
            throw new InvalidFormatException("Vendor Not Supported with currency: " + columnTypes.get(columnNum).get("headerCode"));
        } else if (vendorCurrencies.get(columnTypes.get(columnNum).get("id")) != null) {

            VendorGameCurrency vendorGameCurrency =
                    this.getDataEntityVendorGameCurrency(columnTypes.get(columnNum).get("id"), gameDataEntity);
            //set vendor Game Code base on language
            vendorGameCurrency.setStatus((supportedCurrency.equalsIgnoreCase("yes") ? 1 : 0));
            Currency currency = new Currency();
            currency.setId(Integer.parseInt(columnTypes.get(columnNum).get("id")));
            vendorGameCurrency.setCurrency(currency);
            gameDataEntity.getVendorGameCurrencies().put(columnTypes.get(columnNum).get("id"), vendorGameCurrency);
        }
        return gameDataEntity;
    }


    public VendorGameCode getDataEntityVendorGameCode(String languageId, GameDataEntity gameDataEntity) {
        //get from existing vendorGameCode by language id
        VendorGameCode vendorGameCode = gameDataEntity.getVendorGameCodes().get(languageId);
        //check if not exist create a new vendorGameCode
        return (vendorGameCode != null) ? vendorGameCode : new VendorGameCode();
    }

    public VendorGameCurrency getDataEntityVendorGameCurrency(String currencyId, GameDataEntity gameDataEntity) {
        //get from existing vendorGameCode by language id
        VendorGameCurrency vendorGameCurrency = gameDataEntity.VendorGameCurrencies.get(currencyId);
        //check if not exist create a new vendorGameCode
        return (vendorGameCurrency != null) ? vendorGameCurrency : new VendorGameCurrency();
    }

    //endregion
}
