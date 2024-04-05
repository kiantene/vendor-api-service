package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorGameRepository extends JpaRepository<VendorGame, Integer> {
    VendorGame findByCode(String code);
    VendorGame findByIdAndStatus(Integer id, Integer status);
    VendorGame findByVendorGameCode(String vendorGameCode);
    VendorGame findByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId);
    @Query(value =" SELECT vg.code as gameCode, vg.name as gameName, gc.code as categoryCode FROM vendor_games as vg " +
            "INNER JOIN game_categories as gc ON gc.id = vg.game_category_id WHERE vg.vendor_id=:vendorId AND vg.status=:status",
            countQuery =
                    "SELECT count(*) FROM vendor_games WHERE vendor_id=:vendorId AND status=:status",
            nativeQuery=true)
    Page<Object> findByVendorIdAndStatus(@Param("vendorId") Integer vendorId, @Param("status") Integer status, Pageable pageable);



    @Query(value = "SELECT " +
            "gamelist.gameCode, " +
            "IFNULL(languageList.langName, gamelist.GameName) AS name, " +
            "gamelist.categoryCode, " +
            "IFNULL( concat(:gameUrl, (IFNULL(languageList.langImageSquare, gamelist.defaultImageSquare))), null) AS imageSquare, " +
            "IFNULL( concat( :gameUrl, (IFNULL(languageList.langImageLandscape, gamelist.defaultImageLanscape))), null) AS imageLanscape, " +
            "gamelist.languageCode, " +
            "gamelist.platformCode, " +
            "gamelist.currencyCode " +
            "FROM " +
            "(SELECT " +
            "vg.id AS gameID, " +
            "vg.code AS gameCode, vg.name as GameName, " +
            "vg.image_square AS defaultImageSquare, vg.image_landscape AS defaultImageLanscape, " +
            "vgc.status, " +
            "gc.code AS categoryCode, v.code AS vendorCode, " +
            "GROUP_CONCAT(DISTINCT l.code SEPARATOR ',') AS languageCode, " +
            "GROUP_CONCAT(DISTINCT p.code SEPARATOR ',') AS platformCode, " +
            "GROUP_CONCAT(DISTINCT c.code SEPARATOR ',') AS currencyCode " +
            "FROM vendor_game_codes AS vgc " +
            "INNER JOIN vendor_games vg ON vgc.vendor_game_id = vg.id " +
            "INNER JOIN languages l on l.id = vgc.language_id  " +
            "INNER JOIN platforms p on p.id = vgc.platform_id " +
            "INNER JOIN game_categories gc on vg.game_category_id = gc.id " +
            "INNER JOIN vendors v on vg.vendor_id = v.id " +
            "INNER JOIN vendor_game_currencies vgcurrency on vg.id = vgcurrency.vendor_game_id " +
            "INNER JOIN currencies c on c.id = vgcurrency.currency_id " +
            "WHERE vgc.status = :status " +
            "AND vgcurrency.status = :status " +
            "AND vgc.vendor_id = :vendorId " +
            "AND vgcurrency.currency_id IN (:currencyIds) " +
            "AND vg.game_category_id IN (:categoryIds) " +
            "GROUP BY  vg.id " +
            "ORDER BY vg.code, l.code,  p.code) AS gamelist " +
            "LEFT JOIN ( SELECT vgcl.name as langName, vgcl.image_square as langImageSquare, vgcl.image_landscape as langImageLandscape, vgcl.vendor_game_id " +
            "FROM vendor_game_codes vgcl  " +
            "WHERE vgcl.vendor_id =:vendorId AND vgcl.language_id =:languageId GROUP BY vgcl.vendor_game_id, vgcl.name " +
            ") AS languageList " +
            "ON languageList.vendor_game_id = gamelist.gameID",
            countQuery =
                    "SELECT " +
                            "COUNT(gamelist.gameID) " +
                            "FROM " +
                            "(SELECT " +
                            "vg.id AS gameID, " +
                            "c.code AS currencyCode, " +
                            ":gameUrl "+
                            "FROM vendor_game_codes AS vgc " +
                            "INNER JOIN vendor_games vg ON vgc.vendor_game_id = vg.id " +
                            "INNER JOIN vendor_game_currencies vgcurrency on vg.id = vgcurrency.vendor_game_id " +
                            "INNER JOIN currencies c on c.id = vgcurrency.currency_id " +
                            "WHERE vgc.status = :status " +
                            "AND vgcurrency.status = :status " +
                            "AND vgc.vendor_id = :vendorId " +
                            "AND vgcurrency.currency_id IN (:currencyIds) " +
                            "AND vg.game_category_id IN (:categoryIds) " +
                            "GROUP BY  vg.id " +
                            ") AS gamelist " +
                            "LEFT JOIN ( SELECT  vgcl.vendor_game_id " +
                            "FROM vendor_game_codes vgcl  " +
                            "WHERE vgcl.vendor_id =:vendorId AND vgcl.language_id =:languageId GROUP BY vgcl.vendor_game_id, vgcl.name " +
                            ") AS languageList " +
                            "ON languageList.vendor_game_id = gamelist.gameID",
            nativeQuery = true)
    Page<Object> findByVendorIdAndStatusAndLanguageAndCategoryAndCurrency(
            @Param("vendorId") Integer vendorId,
            @Param("status") Integer status,
            @Param("categoryIds") List<Integer> categoryIds,
            @Param("currencyIds") List<Integer> currencyIds,
            @Param("languageId") Integer languageId,
            @Param("gameUrl") String gameUrl,
            Pageable pageable);

}
