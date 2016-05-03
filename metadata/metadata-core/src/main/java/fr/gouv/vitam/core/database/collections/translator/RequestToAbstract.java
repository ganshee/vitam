package fr.gouv.vitam.core.database.collections.translator;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.builder.request.construct.Request;
import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.MULTIFILTER;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTION;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.SELECTFILTER;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.parser.request.parser.RequestParser;
import fr.gouv.vitam.parser.request.parser.SelectParser;

/**
 * Request To X Abstract class.
 * All translators should be based on this one.
 *
 */
public class RequestToAbstract {

	protected RequestParser requestParser;

	/**
	 * 
	 * @param requestParser
	 */
	public RequestToAbstract(RequestParser requestParser) {
        this.requestParser = requestParser;
	}

	/**
	 * 
	 * @return the associated RequestParser
	 */
	public RequestParser getRequestParser() {
	    return requestParser;
	}

	/**
	 * @return true if at least one Query is needing Elasticsearch
	 */
	public boolean hasFullTextQuery() {
	    return requestParser.hasFullTextQuery();
	}

	/**
	 * @return True if the hint contains cache
	 */
	public boolean hintCache() {
	    return requestParser.hintCache();
	}

	/**
	 * @return True if the hint contains notimeout
	 */
	public boolean hintNoTimeout() {
	    return requestParser.hintNoTimeout();
	}

	/**
	 * @return the model between Units/ObjectGroups/Objects (in that order)
	 */
	public FILTERARGS model() {
	    return requestParser.model();
	}

	/**
	 * @return The possible maximum depth
	 */
	public int getLastDepth() {
	    return requestParser.getLastDepth();
	}

	/**
	 * @return the Request
	 */
	public Request getRequest() {
	    return requestParser.getRequest();
	}

	/**
	 * @return the number of queries
	 */
	public int getNbQueries() {
	    return requestParser.getRequest().getNbQueries();
	}

	/**
     * @return True if this request is a "multiple" result request
     */
    public boolean isMultiple() {
        return requestParser.getRequest().getFilter().get(MULTIFILTER.mult.exactToken()).asBoolean();
    }

    /**
     * Used by the Data Engine (cache, nocache, notimeout (noCursorTimeout(noCursorTimeout)))
     * 
     * @return the array of hints (if any)
     */
    public ArrayNode getHints() {
        return (ArrayNode) requestParser.getRequest().getFilter()
                .get(SELECTFILTER.hint.exactToken());
    }

    /**
     * FindIterable.limit(limit)
     * 
     * @return the limit
     */
    public int getFinalLimit() {
        JsonNode node = requestParser.getRequest().getFilter()
                .get(SELECTFILTER.limit.exactToken());
        if (node != null) {
            return node.asInt();
        }
        return GlobalDatas.limitLoad;
    }

    /**
     * FindIterable.skip(offset)
     * 
     * @return the offset
     */
    public int getFinalOffset() {
        JsonNode node = requestParser.getRequest().getFilter()
                .get(SELECTFILTER.offset.exactToken());
        if (node != null) {
            return node.asInt();
        }
        return 0;
    }

    /**
     * @return the associated usage if any
     */
    public String getUsage() {
        JsonNode node = ((SelectParser) requestParser).getRequest().getProjection()
                .get(PROJECTION.usage.exactToken());
        if (node != null) {
            return node.asText();
        }
        return null;
    }

	/**
	 * @param nth
	 * @return the nth Query
	 */
	public final Query getNthQuery(int nth) {
	    List<Query> list = requestParser.getRequest().getQueries();
	    if (nth >= list.size()) {
	        throw new IllegalAccessError(
	                "This Query has not enough item to get the position: " + nth);
	    }
	    return list.get(nth);
	}

}