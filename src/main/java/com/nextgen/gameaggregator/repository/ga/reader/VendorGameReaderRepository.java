package com.nextgen.gameaggregator.repository.ga.reader;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorGameReaderRepository extends JpaRepository<VendorGame, Integer> {
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



    // ONEAPI-529: the page of games is selected first, and only that page is aggregated.
    // The joins to vendor_game_codes and vendor_game_currencies are at different grains --
    // game x language x platform against game x currency -- so joining them at the same level
    // multiplies rows per game before anything collapses them. Selecting the page first keeps
    // the aggregation proportional to the page rather than to catalogue x currencies.
    // :innerLimit must cover every row the caller's page can reach, so it is offset + pageSize.
    @Query(value = "SELECT " +
            "paged.gameCode, " +
            "IFNULL(langList.langName, paged.gameName) AS name, " +
            "paged.categoryCode, " +
            "IFNULL( concat(:gameUrl, (IFNULL(langList.langImageSquare, paged.defaultImageSquare))), null) AS imageSquare, " +
            "IFNULL( concat( :gameUrl, (IFNULL(langList.langImageLandscape, paged.defaultImageLanscape))), null) AS imageLanscape, " +
            "codeList.languageCode, " +
            "codeList.platformCode, " +
            "currList.currencyCode " +
            "FROM " +
            "(SELECT " +
            "vg.id AS gameID, " +
            "vg.code AS gameCode, vg.name AS gameName, " +
            "vg.image_square AS defaultImageSquare, vg.image_landscape AS defaultImageLanscape, " +
            "gc.code AS categoryCode " +
            "FROM vendor_games vg " +
            "INNER JOIN game_categories gc on gc.id = vg.game_category_id " +
            "WHERE " +
            "vg.game_category_id IN (:categoryIds) " +
            // The dimension joins stay inside these EXISTS clauses on purpose. The statement
            // this replaces reached the dimensions by INNER JOIN, so a code row naming a
            // language or platform that no longer exists dropped its game from the list
            // entirely. Requiring them here keeps that behaviour rather than quietly widening
            // the result.
            "AND EXISTS ( " +
            "   SELECT 1 FROM vendor_game_codes vgc " +
            "   INNER JOIN languages l on l.id = vgc.language_id " +
            "   INNER JOIN platforms p on p.id = vgc.platform_id " +
            "   WHERE vgc.vendor_game_id = vg.id " +
            "   AND vgc.vendor_id = :vendorId " +
            "   AND vgc.status = :status " +
            ") " +
            "AND EXISTS ( " +
            "   SELECT 1 FROM vendor_game_currencies vgcurrency " +
            "   INNER JOIN currencies c on c.id = vgcurrency.currency_id " +
            "   WHERE vgcurrency.vendor_game_id = vg.id " +
            "   AND vgcurrency.status = :status " +
            "   AND vgcurrency.currency_id IN (:currencyIds) " +
            ") " +
            // Same reason as the dimension joins above: the old statement joined vendors and
            // never projected the column, but the join still required the row to exist.
            "AND EXISTS ( SELECT 1 FROM vendors v WHERE v.id = vg.vendor_id ) " +
            "AND NOT EXISTS ( " +
            "   SELECT 1 FROM vendor_game_deactivated as game_deactived " +
            "   WHERE game_deactived.vendor_game_id = vg.id " +
            "   AND vg.vendor_id = :vendorId " +
            "   AND ((game_deactived.sas_entity_hierarchy_id =1) OR " +
            "   (game_deactived.sas_entity_hierarchy_id =2 AND game_deactived.house_id = :houseId) OR " +
            "   (game_deactived.sas_entity_hierarchy_id =3 AND game_deactived.master_agent_id = :masterAgentId ) OR " +
            "   (game_deactived.sas_entity_hierarchy_id =4 AND game_deactived.agent_id = :agentId ) ) " +
            "   AND  game_deactived.is_deleted = 0 " +
            ") " +
            // vg.code is unique, so the page boundary is deterministic and a game cannot appear
            // on two pages. The statement this replaces ordered only inside its derived table.
            "ORDER BY vg.code " +
            "LIMIT :innerLimit) AS paged " +
            "LEFT JOIN LATERAL ( SELECT " +
            "GROUP_CONCAT(DISTINCT l.code SEPARATOR ',') AS languageCode, " +
            "GROUP_CONCAT(DISTINCT p.code SEPARATOR ',') AS platformCode " +
            "FROM vendor_game_codes vgc " +
            "INNER JOIN languages l on l.id = vgc.language_id " +
            "INNER JOIN platforms p on p.id = vgc.platform_id " +
            "WHERE vgc.vendor_game_id = paged.gameID " +
            "AND vgc.vendor_id = :vendorId AND vgc.status = :status " +
            ") AS codeList ON TRUE " +
            "LEFT JOIN LATERAL ( SELECT " +
            "GROUP_CONCAT(DISTINCT c.code SEPARATOR ',') AS currencyCode " +
            "FROM vendor_game_currencies vgcurrency " +
            "INNER JOIN currencies c on c.id = vgcurrency.currency_id " +
            "WHERE vgcurrency.vendor_game_id = paged.gameID " +
            "AND vgcurrency.status = :status AND vgcurrency.currency_id IN (:currencyIds) " +
            ") AS currList ON TRUE " +
            // Grouped by game alone, with deterministic picks. The lookup this replaces grouped
            // by name as well, so a game whose platform rows disagreed on its name in the
            // requested language was returned twice and inflated the total.
            "LEFT JOIN LATERAL ( SELECT " +
            "MIN(vgcl.name) as langName, MIN(vgcl.image_square) as langImageSquare, MIN(vgcl.image_landscape) as langImageLandscape " +
            "FROM vendor_game_codes vgcl  " +
            "WHERE vgcl.vendor_game_id = paged.gameID " +
            "AND vgcl.vendor_id = :vendorId AND vgcl.language_id = :languageId " +
            ") AS langList ON TRUE " +
            "ORDER BY paged.gameCode",
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
                            "WHERE "+
                            " vgc.vendor_game_id not IN ( " +
                            "   SELECT vendor_game_id FROM vendor_game_deactivated as game_deactived " +
                            "   INNER JOIN vendor_games  on game_deactived.vendor_game_id = vendor_games.id " +
                            "   WHERE" +
                            "   vendor_games.vendor_id = :vendorId AND " +
                            "   ((game_deactived.sas_entity_hierarchy_id =1) OR " +
                            "   (game_deactived.sas_entity_hierarchy_id =2 AND game_deactived.house_id = :houseId) OR " +
                            "   (game_deactived.sas_entity_hierarchy_id =3 AND game_deactived.master_agent_id = :masterAgentId ) OR " +
                            "	(game_deactived.sas_entity_hierarchy_id =4 AND game_deactived.agent_id =  :agentId ) ) " +
                            "   AND  game_deactived.is_deleted = 0 " +
                            "   group by game_deactived.vendor_game_id " +
                            ") " +
                            "AND vgc.status = :status " +
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
            @Param("houseId") Integer houseId,
            @Param("masterAgentId") Integer masterAgentId,
            @Param("agentId") Integer agentId,
            @Param("innerLimit") Integer innerLimit,
            Pageable pageable);

}
