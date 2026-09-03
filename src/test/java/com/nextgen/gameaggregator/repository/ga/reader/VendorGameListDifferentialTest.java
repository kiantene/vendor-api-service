package com.nextgen.gameaggregator.repository.ga.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.jpa.repository.Query;

/**
 * ONEAPI-529 — differential for the POST /game/list main statement.
 *
 * <p>The statement under test is read out of the {@link Query} annotation on
 * {@link VendorGameReaderRepository}, so this exercises what production runs rather than a
 * copy of it. The pre-fix statement is pinned in {@code oneapi-529/main-query-before.sql}.
 *
 * <p>Opt-in: needs a MySQL holding a populated {@code game_aggregator}. Skipped unless a URL
 * is supplied, because CI has no such database.
 *
 * <pre>
 * mvn test -Dtest=VendorGameListDifferentialTest \
 *   -Dga.test.jdbcUrl="jdbc:mysql://127.0.0.1:3306/game_aggregator" \
 *   -Dga.test.user=root -Dga.test.password=...
 * </pre>
 *
 * <p>What this does not reach: it drives the statement over JDBC rather than through Spring
 * Data, so nothing here proves Hibernate binds the parameters or appends the page clause the
 * way it is written. Nor does it go through {@code GameListService}, so how the page and the
 * total become a response is unchecked.
 */
@EnabledIfSystemProperty(named = "ga.test.jdbcUrl", matches = ".+")
public class VendorGameListDifferentialTest {

    /**
     * The page's worth of aggregation the rewrite is allowed. Each game contributes roughly one
     * row per language-platform pair plus one per currency — a low three-figure multiple. The
     * bound is deliberately generous: what it rules out is multiplication by catalogue size,
     * which lands three orders of magnitude above it.
     */
    private static final long MAX_AGGREGATION_ROWS_PER_PAGE_ROW = 200L;

    /** Joins a row's columns into one comparable value; cannot occur in the data. */
    private static final char FIELD_SEPARATOR = '\u0000';

    private static final int PAGE_SIZE = 500;
    private static final int NO_PAGING = 100_000;

    private static final Pattern NAMED_PARAM = Pattern.compile(":([a-zA-Z][a-zA-Z0-9_]*)");
    private static final Pattern PLAN_ROWS =
            Pattern.compile("rows=([0-9.eE+]+) loops=([0-9.eE+]+)");

    private Connection connection;
    private Map<String, Object> params;

    @BeforeEach
    void openConnection() throws SQLException {
        connection = DriverManager.getConnection(
                System.getProperty("ga.test.jdbcUrl"),
                System.getProperty("ga.test.user", "root"),
                System.getProperty("ga.test.password", ""));
        connection.setAutoCommit(false);
        // The pre-fix statement selects columns it does not group by, so it cannot run under
        // ONLY_FULL_GROUP_BY. The replacement can; nothing below relaxes the mode for it.
        try (Statement s = connection.createStatement()) {
            s.execute("SET SESSION sql_mode = REPLACE(@@SESSION.sql_mode, 'ONLY_FULL_GROUP_BY', '')");
        }
        params = defaultParameters();
    }

    @AfterEach
    void closeConnection() throws SQLException {
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
    }

    /**
     * Dataset A — the catalogue as it stands, where a game's platform rows agree on its name.
     * Behaviour preservation: the rewrite must return what the statement it replaces returned.
     *
     * <p>This one passes before the fix as well, by construction — before the fix the two sides
     * are the same statement. It is the guard, not the reproduction.
     */
    @Test
    void rewrittenQueryReturnsTheSameRowsAsTheStatementItReplaces() throws Exception {
        setPaging(NO_PAGING, 0);

        List<String> before = runToRows(pinnedPreFixQuery());
        List<String> after = runToRows(productionQuery());

        assertEquals(before.size(), after.size(),
                "the rewrite returned a different number of games");
        before.sort(String::compareTo);
        after.sort(String::compareTo);
        assertEquals(before, after, "the rewrite returned different game rows");
    }

    /**
     * Dataset A — the reproduction. The statement must aggregate a page, not the catalogue.
     * Measured on the plan MySQL actually executed, because rows-examined understates a
     * join that multiplies before it groups.
     */
    @Test
    void rewrittenQueryAggregatesOnlyThePageItReturns() throws Exception {
        setPaging(PAGE_SIZE, 0);

        long rows = widestPlanNode(productionQuery());
        long allowed = MAX_AGGREGATION_ROWS_PER_PAGE_ROW * PAGE_SIZE;

        assertTrue(rows <= allowed,
                "widest plan node handled " + rows + " rows to return a page of " + PAGE_SIZE
                        + "; the page's worth is " + allowed + " or fewer");
    }

    /**
     * Dataset B — the localised-name lookup groups by name as well as game, so a game whose
     * platform rows disagree on its name in the requested language is returned twice and the
     * total is overstated. Nothing in the schema prevents the disagreement: the tables carry no
     * foreign key to the name.
     *
     * <p>Arranged inside the transaction this test rolls back, so it needs no seeded fixture.
     */
    @Test
    void aGameWhosePlatformRowsDisagreeOnItsNameIsReturnedOnce() throws Exception {
        setPaging(NO_PAGING, 0);

        String gameCode = makeOneGamesPlatformNamesDisagree();

        long occurrences = runToRows(productionQuery()).stream()
                .filter(row -> row.startsWith(gameCode + FIELD_SEPARATOR))
                .count();

        assertEquals(1L, occurrences,
                "game " + gameCode + " was returned " + occurrences + " times");
    }

    /**
     * The statement takes its own page now, so consecutive pages must not overlap, must not
     * skip, and must come back in the order one unpaged read gives. vg.code is unique, which is
     * what makes the boundary deterministic — the statement this replaced ordered only inside
     * its derived table, where the order is not guaranteed to survive.
     */
    @Test
    void consecutivePagesDoNotOverlapOrSkipGames() throws Exception {
        int pageSize = 10;

        setPaging(NO_PAGING, 0);
        List<String> unpaged = firstColumn(runToRows(productionQuery()));

        List<String> paged = new ArrayList<>();
        for (int pageNo = 1; pageNo <= 3; pageNo++) {
            setPaging(pageSize, (pageNo - 1) * pageSize);
            paged.addAll(firstColumn(runToRows(productionQuery())));
        }

        assertEquals(unpaged.subList(0, paged.size()), paged,
                "paging the statement did not reproduce the unpaged order");
        assertEquals(paged.size(), paged.stream().distinct().count(),
                "a game appeared on more than one page");
    }

    /** The pre-fix statement carries none of these; unused parameters bind harmlessly. */
    private void setPaging(int limit, int offset) {
        params.put("limit", limit);
        params.put("offset", offset);
    }

    private List<String> firstColumn(List<String> rows) {
        return rows.stream().map(row -> row.substring(0, row.indexOf(FIELD_SEPARATOR))).toList();
    }

    /**
     * The total beside the list has to be the number of games the list can actually return.
     * The count statement filters more loosely than the list does — it never joins languages,
     * platforms or vendors — so a game the list drops for an unresolved dimension is still
     * counted. The reader sees a total the pages cannot add up to.
     */
    @Test
    void countIsTheNumberOfGamesTheListCanReturn() throws Exception {
        setPaging(NO_PAGING, 0);

        long listed = runToRows(productionQuery()).size();
        long counted = countFrom(countStatement());

        assertEquals(listed, counted,
                "the list returns " + listed + " games but the total says " + counted);
    }

    /**
     * The reproduction for the count. It answers with a single number, so there is no page to
     * bound it against — what it must not do is build the same per-game product the list used
     * to. Bounded against the games it counts, not against games times currencies.
     */
    @Test
    void countDoesNotMultiplyRowsBeforeCounting() throws Exception {
        setPaging(NO_PAGING, 0);

        long counted = countFrom(countStatement());
        long rows = widestPlanNode(countStatement());
        long allowed = MAX_AGGREGATION_ROWS_PER_PAGE_ROW * Math.max(counted, 1);

        assertTrue(rows <= allowed,
                "widest plan node handled " + rows + " rows to count " + counted
                        + " games; " + allowed + " or fewer is the catalogue's worth");
    }

    // ---------------------------------------------------------------- the statements

    /** Looked up by name, so a change to the method's parameters does not break this. */
    private String productionQuery() {
        return java.util.Arrays.stream(VendorGameReaderRepository.class.getMethods())
                .filter(m -> m.getName()
                        .equals("findByVendorIdAndStatusAndLanguageAndCategoryAndCurrency"))
                .map(m -> m.getAnnotation(Query.class))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no @Query on the game list method"))
                .value();
    }

    /**
     * The count statement, wherever production currently keeps it. Spring Data carries it as the
     * {@code countQuery} attribute beside the list query; once the page is assembled by hand it
     * becomes a repository method of its own. Looked up in both places so the same check spans
     * that move rather than being rewritten across it.
     */
    private String countStatement() {
        return java.util.Arrays.stream(VendorGameReaderRepository.class.getMethods())
                .filter(m -> m.getName().toLowerCase().startsWith("count"))
                .map(m -> m.getAnnotation(Query.class))
                .filter(java.util.Objects::nonNull)
                .map(Query::value)
                .findFirst()
                .orElseGet(() -> java.util.Arrays.stream(
                                VendorGameReaderRepository.class.getMethods())
                        .filter(m -> m.getName()
                                .equals("findByVendorIdAndStatusAndLanguageAndCategoryAndCurrency"))
                        .map(m -> m.getAnnotation(Query.class))
                        .filter(java.util.Objects::nonNull)
                        .map(Query::countQuery)
                        .filter(q -> !q.isEmpty())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("no count statement found")));
    }

    private long countFrom(String namedSql) throws SQLException {
        try (PreparedStatement ps = prepare(namedSql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private String pinnedPreFixQuery() throws IOException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("oneapi-529/main-query-before.sql")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.startsWith("--"))
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
    }

    // ---------------------------------------------------------------- running them

    /** Rows flattened to separator-joined strings so two result sets compare as values. */
    private List<String> runToRows(String namedSql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (PreparedStatement ps = prepare(namedSql); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.append(rs.getString(i)).append(FIELD_SEPARATOR);
                }
                rows.add(row.toString());
            }
        }
        return rows;
    }

    /** The largest number of rows any node of the executed plan handled, rows times loops. */
    private long widestPlanNode(String namedSql) throws SQLException {
        long widest = 0;
        try (PreparedStatement ps = prepare("EXPLAIN ANALYZE " + namedSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Matcher m = PLAN_ROWS.matcher(rs.getString(1));
                while (m.find()) {
                    long handled = Math.round(
                            Double.parseDouble(m.group(1)) * Double.parseDouble(m.group(2)));
                    widest = Math.max(widest, handled);
                }
            }
        }
        return widest;
    }

    /** Expands {@code :name} placeholders, list parameters included, and binds in order. */
    private PreparedStatement prepare(String namedSql) throws SQLException {
        StringBuilder sql = new StringBuilder();
        List<Object> bindings = new ArrayList<>();
        Matcher m = NAMED_PARAM.matcher(namedSql);
        int cursor = 0;
        while (m.find()) {
            sql.append(namedSql, cursor, m.start());
            Object value = params.get(m.group(1));
            if (value == null) {
                throw new IllegalStateException("no value for :" + m.group(1));
            }
            if (value instanceof List<?> list) {
                sql.append("?,".repeat(list.size() - 1)).append("?");
                bindings.addAll(list);
            } else {
                sql.append("?");
                bindings.add(value);
            }
            cursor = m.end();
        }
        sql.append(namedSql.substring(cursor));

        PreparedStatement ps = connection.prepareStatement(sql.toString());
        for (int i = 0; i < bindings.size(); i++) {
            ps.setObject(i + 1, bindings.get(i));
        }
        return ps;
    }

    // ---------------------------------------------------------------- the datasets

    private Map<String, Object> defaultParameters() throws SQLException {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("vendorId", Integer.getInteger("ga.test.vendorId", 1));
        p.put("status", Integer.getInteger("ga.test.status", 1));
        p.put("languageId", Integer.getInteger("ga.test.languageId", 1));
        p.put("houseId", Integer.getInteger("ga.test.houseId", 1));
        p.put("masterAgentId", Integer.getInteger("ga.test.masterAgentId", 1));
        p.put("agentId", Integer.getInteger("ga.test.agentId", 1));
        p.put("gameUrl", System.getProperty("ga.test.gameUrl", "https://img.example/"));
        // A multi-currency agent is what makes this endpoint slow, so take every currency and
        // every category rather than a narrow slice.
        p.put("currencyIds", idsFrom("SELECT id FROM currencies"));
        p.put("categoryIds", idsFrom("SELECT id FROM game_categories"));
        return p;
    }

    private List<Integer> idsFrom(String sql) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        if (ids.isEmpty()) {
            throw new IllegalStateException("no rows for: " + sql);
        }
        return ids;
    }

    /**
     * Renames one platform's row for a game that has more than one in the requested language,
     * so the two disagree. Returns that game's code.
     */
    private String makeOneGamesPlatformNamesDisagree() throws SQLException {
        String find =
                "SELECT vg.code, MAX(vgc.id) FROM vendor_game_codes vgc"
                        + " INNER JOIN vendor_games vg ON vg.id = vgc.vendor_game_id"
                        + " WHERE vgc.vendor_id = ? AND vgc.language_id = ? AND vgc.status = ?"
                        + " GROUP BY vgc.vendor_game_id, vg.code HAVING COUNT(*) > 1 LIMIT 1";
        String gameCode;
        int codeRowId;
        try (PreparedStatement ps = connection.prepareStatement(find)) {
            ps.setObject(1, params.get("vendorId"));
            ps.setObject(2, params.get("languageId"));
            ps.setObject(3, params.get("status"));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException(
                            "no game has two code rows in the requested language; this dataset "
                                    + "cannot express the defect");
                }
                gameCode = rs.getString(1);
                codeRowId = rs.getInt(2);
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE vendor_game_codes SET name = CONCAT(name, ' (other platform)') WHERE id = ?")) {
            ps.setInt(1, codeRowId);
            ps.executeUpdate();
        }
        return gameCode;
    }
}
