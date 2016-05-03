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
package fr.gouv.vitam.builder.request.construct.query;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERY;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERYARGS;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;

/**
 * Match Query
 *
 */
public class MatchQuery extends Query {
    protected MatchQuery() {
        super();
    }

    /**
     * Match Query constructor
     *
     * @param matchQuery
     *            match, match_phrase, match_phrase_prefix
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public MatchQuery(final QUERY matchQuery, final String variableName,
            final String value)
            throws InvalidCreateOperationException {
        super();
        switch (matchQuery) {
            case match:
            case match_phrase:
            case match_phrase_prefix:
            case prefix: {
                createQueryVariableValue(matchQuery, variableName, value);
                currentQUERY = matchQuery;
                setReady(true);
                break;
            }
            default:
                throw new InvalidCreateOperationException(
                        "Query " + matchQuery + " is not a Match Query");
        }
    }

    /**
     *
     * @param max
     *            max expansions for Match type request only (not regex, search)
     * @return this MatchQuery
     * @throws InvalidCreateOperationException
     */
    public final MatchQuery setMatchMaxExpansions(final int max)
            throws InvalidCreateOperationException {
        switch (currentQUERY) {
            case match:
            case match_phrase:
            case match_phrase_prefix:
            case prefix:
                ((ObjectNode) currentObject).put(QUERYARGS.max_expansions.exactToken(),
                        max);
                break;
            default:
                throw new InvalidCreateOperationException(
                        "Query " + currentQUERY + " is not a Match Query");
        }
        return this;
    }
}