/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.parser.request.parser.query;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.builder.request.construct.QueryHelper;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERY;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.parser.request.parser.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Query from Parser Helper
 *
 */
public class QueryParserHelper extends QueryHelper {

    protected QueryParserHelper() {
    }

    /**
     *
     * @param array
     *            primary list of path in the future PathQuery
     * @param adapter 
     * @return a PathQuery
     */
    public static final PathQuery path(final JsonNode array, final VarNameAdapter adapter) {
        return new PathQuery(QUERY.path, array, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a CompareQuery using EQ comparator
     * @throws InvalidParseOperationException 
     */
    public static final CompareQuery eq(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new CompareQuery(QUERY.eq, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a CompareQuery using NE comparator
     * @throws InvalidParseOperationException 
     */
    public static final CompareQuery ne(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new CompareQuery(QUERY.ne, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a CompareQuery using LT (less than) comparator
     * @throws InvalidParseOperationException 
     */
    public static final CompareQuery lt(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new CompareQuery(QUERY.lt, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a CompareQuery using LTE (less than or equal) comparator
     * @throws InvalidParseOperationException 
     */
    public static final CompareQuery lte(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new CompareQuery(QUERY.lte, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a CompareQuery using GT (greater than) comparator
     * @throws InvalidParseOperationException 
     */
    public static final CompareQuery gt(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new CompareQuery(QUERY.gt, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a CompareQuery using GTE (greater than or equal) comparator
     * @throws InvalidParseOperationException 
     */
    public static final CompareQuery gte(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new CompareQuery(QUERY.gte, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a CompareQuery using SIZE comparator
     * @throws InvalidParseOperationException 
     */
    public static final CompareQuery size(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new CompareQuery(QUERY.size, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return an ExistsQuery
     * @throws InvalidCreateOperationException
     *             using Exists operator
     * @throws InvalidParseOperationException 
     */
    public static final ExistsQuery exists(final JsonNode command, final VarNameAdapter adapter)
            throws InvalidCreateOperationException, InvalidParseOperationException {
        return new ExistsQuery(QUERY.exists, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return an ExistsQuery using Missing operator
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException 
     */
    public static final ExistsQuery missing(final JsonNode command, final VarNameAdapter adapter)
            throws InvalidCreateOperationException, InvalidParseOperationException {
        return new ExistsQuery(QUERY.missing, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return an ExistsQuery using isNull operator
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException 
     */
    public static final ExistsQuery isNull(final JsonNode command, final VarNameAdapter adapter)
            throws InvalidCreateOperationException, InvalidParseOperationException {
        return new ExistsQuery(QUERY.isNull, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return an InQuery using IN operator
     * @throws InvalidParseOperationException
     */
    public static final InQuery in(final JsonNode command, final VarNameAdapter adapter)
            throws InvalidParseOperationException {
        return new InQuery(QUERY.in, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return an InQuery using NIN (not in) operator
     * @throws InvalidParseOperationException
     */
    public static final InQuery nin(final JsonNode command, final VarNameAdapter adapter)
            throws InvalidParseOperationException {
        return new InQuery(QUERY.nin, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a MatchQuery using MATCH operator
     * @throws InvalidParseOperationException 
     */
    public static final MatchQuery match(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new MatchQuery(QUERY.match, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a MatchQuery using MATCH_PHRASE operator
     * @throws InvalidParseOperationException 
     */
    public static final MatchQuery matchPhrase(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new MatchQuery(QUERY.match_phrase, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a MatchQuery using MATCH_PHRASE_PREFIX operator
     * @throws InvalidParseOperationException 
     */
    public static final MatchQuery matchPhrasePrefix(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new MatchQuery(QUERY.match_phrase_prefix, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a MatchQuery using PREFIX operator
     * @throws InvalidParseOperationException 
     */
    public static final MatchQuery prefix(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new MatchQuery(QUERY.prefix, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a SearchQuery using REGEX operator
     * @throws InvalidParseOperationException 
     */
    public static final SearchQuery regex(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new SearchQuery(QUERY.regex, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a SearchQuery using SEARCH operator
     * @throws InvalidParseOperationException 
     */
    public static final SearchQuery search(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new SearchQuery(QUERY.search, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a TermQuery
     * @throws InvalidParseOperationException 
     */
    public static final TermQuery term(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new TermQuery(QUERY.term, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a WildcardQuery
     * @throws InvalidParseOperationException 
     */
    public static final WildcardQuery wildcard(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new WildcardQuery(QUERY.wildcard, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a MltQuery using a FLT (fuzzy like this) operator
     * @throws InvalidParseOperationException 
     */
    public static final MltQuery flt(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new MltQuery(QUERY.flt, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a MltQuery using a MLT (more like this) operator
     * @throws InvalidParseOperationException 
     */
    public static final MltQuery mlt(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new MltQuery(QUERY.mlt, command, adapter);
    }

    /**
     *
     * @param command
     * @param adapter 
     * @return a RangeQuery
     * @throws InvalidParseOperationException 
     */
    public static final RangeQuery range(final JsonNode command, final VarNameAdapter adapter) throws InvalidParseOperationException {
        return new RangeQuery(QUERY.range, command, adapter);
    }
}