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
package fr.gouv.vitam.ingest.external.core;

import fr.gouv.vitam.common.storage.filesystem.FileSystem;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.workspace.common.WorkspaceFileSystem;

import java.io.InputStream;
/**
Asynchrone exchange Model
 */
public class PreUploadResume {
    private LogbookOperationsClientHelper helper;

    private LogbookTypeProcess logbookTypeProcess;
    private LogbookOperationParameters startedParameters;
    private WorkspaceFileSystem workspaceFileSystem;
    private String contextWithExecutionMode;

    public PreUploadResume(
        LogbookOperationsClientHelper helper,
        LogbookTypeProcess logbookTypeProcess,
        LogbookOperationParameters startedParameters,
        WorkspaceFileSystem workspaceFileSystem, String contextWithExecutionMode) {
        this.helper = helper;
        this.logbookTypeProcess = logbookTypeProcess;
        this.startedParameters = startedParameters;
        this.workspaceFileSystem = workspaceFileSystem;
        this.contextWithExecutionMode = contextWithExecutionMode;
    }

    public LogbookTypeProcess getLogbookTypeProcess() {
        return logbookTypeProcess;
    }

    public LogbookOperationParameters getStartedParameters() {
        return startedParameters;
    }

    public WorkspaceFileSystem getWorkspaceFileSystem() {
        return workspaceFileSystem;
    }

    public String getContextWithExecutionMode() {
        return contextWithExecutionMode;
    }

    public LogbookOperationsClientHelper getHelper() {
        return helper;
    }
}
