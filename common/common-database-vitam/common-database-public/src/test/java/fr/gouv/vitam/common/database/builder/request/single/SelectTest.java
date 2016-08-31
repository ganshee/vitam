package fr.gouv.vitam.common.database.builder.request.single;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.flt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.isNull;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.matchPhrase;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.matchPhrasePrefix;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.missing;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.mlt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.ne;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.nin;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.not;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.path;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.prefix;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.range;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.regex;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.search;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.size;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.term;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.wildcard;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.ExistsQuery;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

public class SelectTest {
    @Test
    public void testAddLimitFilter() {
        final Select select = new Select();
        select.setLimitFilter(0, 0);
        assertFalse(select.getFilter().has(SELECTFILTER.LIMIT.exactToken()));
        assertFalse(select.getFilter().has(SELECTFILTER.OFFSET.exactToken()));
        select.setLimitFilter(1, 0);
        assertFalse(select.getFilter().has(SELECTFILTER.LIMIT.exactToken()));
        assertTrue(select.getFilter().has(SELECTFILTER.OFFSET.exactToken()));
        select.setLimitFilter(0, 1);
        assertTrue(select.getFilter().has(SELECTFILTER.LIMIT.exactToken()));
        assertFalse(select.getFilter().has(SELECTFILTER.OFFSET.exactToken()));
        select.setLimitFilter(1, 1);
        assertTrue(select.getFilter().has(SELECTFILTER.LIMIT.exactToken()));
        assertTrue(select.getFilter().has(SELECTFILTER.OFFSET.exactToken()));
        select.resetLimitFilter();
        assertFalse(select.getFilter().has(SELECTFILTER.LIMIT.exactToken()));
        assertFalse(select.getFilter().has(SELECTFILTER.OFFSET.exactToken()));
    }

    @Test
    public void testAddHintFilter() {
        final Select select = new Select();
        try {
            select.addHintFilter(FILTERARGS.CACHE.exactToken());
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertTrue(select.getFilter().has(SELECTFILTER.HINT.exactToken()));
        assertEquals(1, select.getFilter().get(SELECTFILTER.HINT.exactToken()).size());
        try {
            select.addHintFilter(FILTERARGS.NOCACHE.exactToken());
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertTrue(select.getFilter().has(SELECTFILTER.HINT.exactToken()));
        assertEquals(2, select.getFilter().get(SELECTFILTER.HINT.exactToken()).size());
        select.resetHintFilter();
        assertFalse(select.getFilter().has(SELECTFILTER.HINT.exactToken()));
    }

    @Test
    public void testAddOrderByAscFilter() {
        final Select select = new Select();
        try {
            select.addOrderByAscFilter("var1", "var2");
            assertEquals(2, select.getFilter().get(SELECTFILTER.ORDERBY.exactToken()).size());
            select.addOrderByAscFilter("var3").addOrderByAscFilter("var4");
            assertEquals(4, select.getFilter().get(SELECTFILTER.ORDERBY.exactToken()).size());
            select.addOrderByDescFilter("var1", "var2");
            assertEquals(4, select.getFilter().get(SELECTFILTER.ORDERBY.exactToken()).size());
            select.addOrderByDescFilter("var3").addOrderByDescFilter("var4");
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(4, select.getFilter().get(SELECTFILTER.ORDERBY.exactToken()).size());
        select.resetOrderByFilter();
        assertFalse(select.getFilter().has(SELECTFILTER.ORDERBY.exactToken()));
    }

    @Test
    public void testAddUsedProjection() {
        final Select select = new Select();
        assertNull(select.projection);
        try {
            select.addUsedProjection("var1", "var2");
            assertEquals(2, select.projection.get(PROJECTION.FIELDS.exactToken()).size());
            select.addUsedProjection("var3").addUsedProjection("var4");
            assertEquals(4, select.projection.get(PROJECTION.FIELDS.exactToken()).size());
            select.addUnusedProjection("var1", "var2");
            // used/unused identical so don't change the number
            assertEquals(4, select.projection.get(PROJECTION.FIELDS.exactToken()).size());
            select.addUnusedProjection("var3").addUnusedProjection("var4");
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(4, select.projection.get(PROJECTION.FIELDS.exactToken()).size());
        select.resetUsedProjection();
        assertFalse(select.projection.has(PROJECTION.FIELDS.exactToken()));
    }

    @Test
    public void testAddRequests() {
        final Select select = new Select();
        assertTrue(select.query == null);
        try {
            select.setQuery(
                new BooleanQuery(QUERY.AND).add(new ExistsQuery(QUERY.EXISTS, "varA"))
                    .setRelativeDepthLimit(5));
            assertNotNull(select.getQuery());
            select.setLimitFilter(10, 10);
            try {
                select.addHintFilter(FILTERARGS.CACHE.exactToken());
                select.addOrderByAscFilter("var1").addOrderByDescFilter("var2");
                select.addUsedProjection("var3").addUnusedProjection("var4");
                ObjectNode node = select.getFinalSelect();
                assertEquals(3, node.size());
                node = select.getFinalSelect();
                assertEquals(3, node.size());
                select.resetQuery();
                assertEquals(0, select.getQuery().getCurrentQuery().size());
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testAllReset() throws InvalidParseOperationException {
        final Select select = new Select();
        select.addUsedProjection("var1");
        assertEquals(1, select.projection.size());
        select.reset();
        assertEquals(0, select.projection.size());
    }

    @Test
    public void testParser() throws InvalidParseOperationException {
        final Select select = new Select();
        select.parseOrderByFilter("{$orderby : { maclef1 : 1 , maclef2 : -1 }}");
        select.parseLimitFilter("{$limit : 5}");
        assertEquals("{\"maclef1\":1,\"maclef2\":-1}", select.getFilter().get(SELECTFILTER.ORDERBY.exactToken()).toString());
        assertEquals("5", select.getFilter().get(SELECTFILTER.LIMIT.exactToken()).toString());
        select.resetFilter();
        select.parseFilter("{$orderby : { maclef1 : 1 , maclef2 : -1 }, $limit : 5}");
        select.parseProjection("{$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' }");
        assertTrue(select.getAllProjection());
        assertEquals("{\"$fields\":{\"#dua\":1,\"#all\":1}}", select.getProjection().toString());
        final String s = "QUERY: Requests: \n" +
            "\tFilter: {\"$limit\":5,\"$orderby\":{\"maclef1\":1,\"maclef2\":-1}}\n" +
            "\tProjection: {\"$fields\":{\"#dua\":1,\"#all\":1}}";
        assertEquals(s, select.toString());
    }

    @Test
    public void testVariousQueries() throws InvalidCreateOperationException, InvalidParseOperationException {
        final Select select = new Select();
        select.setQuery(path("id1"));
        select.setQuery(
            and().add(exists("mavar1"), missing("mavar2"), isNull("mavar3"),
                or().add(in("mavar4", 1, 2).add("maval1"),
                    nin("mavar5", "maval2").add(true)),
                not().add(size("mavar5", 5), gt("mavar6", 7), lte("mavar7", 8),
                    gte("mavar7", 8), lt("mavar7", 8)),
                not().add(eq("mavar8", 5), ne("mavar9", "ab"),
                    range("mavar10", 12, true, 20, true)),
                matchPhrase("mavar11", "ceci est une phrase"),
                matchPhrasePrefix("mavar11", "ceci est une phrase")
                    .setMatchMaxExpansions(10),
                flt("ceci est une phrase", "mavar12", "mavar13"),
                mlt("ceci est une phrase", "mavar12", "mavar13"),
                and().add(search("mavar13", "ceci est une phrase"),
                    prefix("mavar13", "ceci est une phrase"),
                    wildcard("mavar13", "ceci"),
                    regex("mavar14", "^start?aa.*")),
                and().add(term("mavar14", "motMajuscule").add("mavar15", "simplemot")),
                and().add(term("mavar16", "motMajuscule").add("mavar17", "simplemot"),
                    or().add(eq("mavar19", "abcd"),
                        match("mavar18", "quelques mots"))),
                regex("mavar14", "^start?aa.*")));
        select.setLimitFilter(100, 1000).addHintFilter(FILTERARGS.CACHE.exactToken());
        select.addOrderByAscFilter("maclef1")
            .addOrderByDescFilter("maclef2").addOrderByAscFilter("maclef3");
        select.addUsedProjection("#dua", "#all");
        assertNotNull(select.getFinalSelect());
        assertTrue(select.getAllProjection());
        assertNotNull(select.getFilter());
        assertNotNull(select.getProjection());
        assertNotNull(select.getQuery());
        assertNotNull(select.getFinal());
    }
}