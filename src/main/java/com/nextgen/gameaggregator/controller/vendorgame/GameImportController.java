package com.nextgen.gameaggregator.controller.vendorgame;

import com.nextgen.gameaggregator.controller.vendorgame.service.GameExcelService;
import com.nextgen.gameaggregator.controller.vendorgame.service.GameExcelValidatorService;
import com.nextgen.gameaggregator.controller.vendorgame.vo.ImportResponse;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.VendorGameRepository;
import com.nextgen.gameaggregator.service.GameCategoryService;
import com.nextgen.gameaggregator.service.VendorService;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

@RestController
@RequestMapping(path = "vendorGame/")
public class GameImportController {

    @Autowired
    VendorGameRepository vendorGameRepository;

    @Autowired
    GameExcelService gameExcelService;

    @Autowired
    GameExcelValidatorService gameExcelValidatorService;
    @Autowired
    VendorService vendorService;
    @Autowired
    GameCategoryService gameCategoryService;

    @PostMapping(value = "upload", consumes = {"multipart/form-data"})
    public ResponseEntity<ImportResponse> uploadFile(@RequestParam(value = "file") MultipartFile file) {

        ImportResponse responseVo = new ImportResponse();
        try {
            String message = "";
            if (!gameExcelService.hasExcelFormat(file)) {
                message = "Please upload an excel file!";
                throw new InvalidRequestException("invalid file format");
            }

            //1. validate excel name format
            String[] vendorInfo = gameExcelValidatorService.checkFileName(file.getOriginalFilename());
            //2. validate vendor code from Excel file name
            Vendor vendor = vendorService.findVendorByCode(vendorInfo[0]);
            //3. validate game category code from Excel file name
            GameCategory gameCategory = gameCategoryService.findGameCategoryByCode(vendorInfo[1].split("\\.", 2)[0]);
            //4. validate vendor supported game category
            gameCategoryService.checkVendorSupportGameCategory(vendor, gameCategory);

            //5. get vendor supported language
            HashMap<String, Language> vendorLanguages = vendorService.findVendorSupportedLanguages(vendor.getId());
            //6. get vendor supported currencies
            HashMap<String, Currency> vendorCurrencies = vendorService.findVendorSupportedCurrencies(vendor.getId());

            HashMap<String, Platform> platforms = gameExcelValidatorService.getSystemSupportedPlatform();

            Iterator<Row> rows = gameExcelService.readContent(file.getInputStream());
            HashMap<Integer, HashMap<String, String>> columnTypes = gameExcelValidatorService.validateHeader(rows, platforms);


            responseVo = gameExcelService.saveRows(rows, columnTypes, vendorLanguages, vendorCurrencies, vendor, gameCategory, platforms);

            message = "Uploaded the file successfully: " + file.getOriginalFilename();
            responseVo.setMessage(message);
        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setMessage(invalidRequestException.getMessage());
        } catch (InvalidFormatException invalidFormatException) {
            responseVo.setMessage(invalidFormatException.getMessage());
        } catch (InvalidVendorException invalidVendorException) {
            responseVo.setMessage("Vendor Code not found");
        } catch (IOException iOException) {
            responseVo.setMessage("Invalid file format");
        } catch (InvalidGameCategoryException invalidGameCategoryException) {
            responseVo.setMessage("Game category Code not found");
        } catch (VendorGameCategoryNotSupportedException vendorGameCategoryNotSupportedException) {
            responseVo.setMessage("Game category Code not supported by vendor");
        } catch (InvalidLanguageException invalidLanguageException) {
            responseVo.setMessage("Not Language supported by vendor");
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setMessage("Not currency supported by vendor");
        }


//        catch (Exception exception) {
//            responseVo.setMessage(exception.getMessage());
//        }

        return new ResponseEntity<>(responseVo, HttpStatus.OK);

    }


}