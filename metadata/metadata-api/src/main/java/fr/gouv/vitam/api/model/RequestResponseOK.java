/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.api.model;

import java.util.List;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SingletonUtils;

/**
 * Meta-data RequestResponseOK class contains hits and result objects
 *
 */
public class RequestResponseOK extends RequestResponse {
    private DatabaseCursor hits;
    private List<String> results;

    /**
     * @return the hits of RequestResponseOK object
     */
    public DatabaseCursor getHits() {
        if (hits == null) {
            return new DatabaseCursor(0, 0, 0);
        }
        return hits;
    }

    /**
     * @param hits as DatabaseCursor object
     * @return RequestReponseOK with the hits are setted
     */
    public RequestResponseOK setHits(DatabaseCursor hits) {
        ParametersChecker.checkParameter("DatabaseCursor of result is a mandatory parameter", hits);
        this.hits = hits;
        return this;
    }

    /**
     * @param total of units inserted/modified as integer
     * @param offset of unit in database as integer
     * @param limit of unit per response as integer
     * @return the RequestReponseOK with the hits are setted
     */
    public RequestResponseOK setHits(int total, int offset, int limit) {
        ParametersChecker.checkParameter("Total of result is a mandatory parameter", total);
        ParametersChecker.checkParameter("Offset of result is a mandatory parameter", offset);
        ParametersChecker.checkParameter("Limit of result is a mandatory parameter", limit);
        hits = new DatabaseCursor(total, offset, limit);
        return this;
    }

    /**
     * @return the result of RequestResponse as a list of String
     */
    public List<String> getResults() {
        if (results == null) {
            return SingletonUtils.singletonList();
        }
        return results;
    }

    /**
     * @param results as a list of String
     * @return the RequestReponseOK with the result is setted
     */
    public RequestResponseOK setResults(List<String> results) {
        ParametersChecker.checkParameter("List of result is a mandatory parameter", results);
        this.results = results;
        return this;
    }

}