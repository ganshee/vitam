/**
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
 */
package fr.gouv.vitam.storage.offers.workspace.driver;

import java.util.Properties;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;

/**
 * Workspace Driver Implementation
 */
public class DriverImpl implements Driver {

    private static final String DRIVER_NAME = "WorkspaceDriver";
    private static final String URL_IS_A_MANDATORY_PARAMETER = "Url is a mandatory parameter";

    @Override
    public ConnectionImpl connect(String url, Properties parameters) throws StorageDriverException {
        try {
            ParametersChecker.checkParameter(URL_IS_A_MANDATORY_PARAMETER, url);
        } catch (IllegalArgumentException exc) {
            throw new StorageDriverException(DRIVER_NAME, StorageDriverException.ErrorCode.PRECONDITION_FAILED,
                URL_IS_A_MANDATORY_PARAMETER);
        }
        try {
            ConnectionImpl connection = new ConnectionImpl(url, DRIVER_NAME);
            connection.getStatus();
            return connection;
        } catch (StorageDriverException exception) {
            throw new StorageDriverException(DRIVER_NAME, StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                exception.getMessage());
        }

    }

    @Override
    public boolean isStorageOfferAvailable(String url, Properties parameters) throws StorageDriverException {
        return true;
    }

    @Override
    public String getName() {
        return DRIVER_NAME;
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

}