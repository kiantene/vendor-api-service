package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.GameCategory;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.entity.ga.VendorGameCategory;
import com.nextgen.gameaggregator.exception.InvalidGameCategoryException;
import com.nextgen.gameaggregator.exception.VendorGameCategoryNotSupportedException;
import com.nextgen.gameaggregator.repository.ga.writer.GameCategoryRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GameCategoryService {

    @Autowired
    private GameCategoryRepository gameCategoryRepository;

    @Autowired
    private VendorGameCategoryRepository vendorGameCategoryRepository;


    public GameCategory findGameCategoryByCode(String gameCategoryCode) throws InvalidGameCategoryException{
        GameCategory gameCategory = gameCategoryRepository.findByCode(gameCategoryCode);
        Optional.ofNullable(gameCategory).orElseThrow(InvalidGameCategoryException::new);
        return gameCategory;
    }


    public void checkVendorSupportGameCategory(Vendor vendor, GameCategory gameCategory) throws VendorGameCategoryNotSupportedException {
        VendorGameCategory vendorGameCategory = vendorGameCategoryRepository.findByVendorIdAndGameCategoryId(vendor.getId(), gameCategory.getId());
        Optional.ofNullable(vendorGameCategory).orElseThrow(VendorGameCategoryNotSupportedException::new);

    }
}
