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
package fr.gouv.vitam.ihmrecette.soapui;

import java.io.IOException;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * SoapUI Client factory
 */
public class SoapUiClientFactory {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SoapUiClientFactory.class);
    private static final SoapUiClientFactory SOAP_UI_CLIENT_FACTORY = new SoapUiClientFactory();
    private static final String CONFIGURATION_FILENAME = "soapui.conf";
    protected SoapUiConfig clientConfiguration;
    private SoapUiClientType clientType = SoapUiClientType.MOCK;

    /**
     * Initialize the factory configuration
     */
    private SoapUiClientFactory() {
        changeConfiguration(CONFIGURATION_FILENAME);
    }

    /**
     * Get the SoapUiClientFactory instance
     *
     * @return the instance
     */
    public static final SoapUiClientFactory getInstance() {
        return SOAP_UI_CLIENT_FACTORY;
    }

    /**
     * Get the default type SOAP UI client
     *
     * @return the default SOAP UI client
     */
    public SoapUiClient getClient() {
        SoapUiClient client;
        switch (clientType) {
            case MOCK:
                client = new SoapUiClientMock();
                break;
            case NORMAL:
                client = new SoapUiClientCommand(clientConfiguration);
                break;
            default:
                throw new IllegalArgumentException("Log type unknown");
        }
        return client;
    }

    /**
     * Get the default SoapUI client type
     *
     * @return the default SoapUI client type
     */
    public SoapUiClientType getClientType() {
        return clientType;
    }

    /**
     * Modify the default SoapUI client type
     *
     * @param type the client type to set
     * @throws IllegalArgumentException if type null
     */
    void changeClientType(SoapUiClientType type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        clientType = type;
    }

    /**
     * Change client configuration from server/host params
     *
     * @param configurationPath
     */
    public final void changeConfiguration(String configurationPath) {
        changeClientType(SoapUiClientType.MOCK);
        SoapUiConfig configuration = null;

        if (configurationPath == null) {
            LOGGER.error("Error when retrieving configuration file {}, using mock",
                configurationPath);
            return;
        }

        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(configurationPath), SoapUiConfig.class);
        } catch (final IOException fnf) {
            LOGGER.debug("Error when retrieving configuration file {}, using mock",
                configurationPath, fnf);
        }

        if (configuration == null) {
            LOGGER.error("Error when retrieving configuration file {}, using mock",
                configurationPath);
        } else {
            changeClientType(SoapUiClientType.NORMAL);
            clientConfiguration = configuration;
        }
    }

    /**
     * enum to define client type
     */
    public enum SoapUiClientType {
        /**
         * To use only in MOCK: READ operations are not supported (default)
         */
        MOCK,
        /**
         * Use real service (need server to be set)
         */
        NORMAL
    }
}
