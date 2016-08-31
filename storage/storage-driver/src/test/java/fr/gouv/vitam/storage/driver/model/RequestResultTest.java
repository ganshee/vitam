/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

package fr.gouv.vitam.storage.driver.model;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TEst for PutObjectRequestTest
 */
public class RequestResultTest {
    private static GetObjectRequest getObjectRequest;
    private static GetObjectResult getObjectResult;
    private static RemoveObjectRequest removeObjectRequest;
    private static RemoveObjectResult removeObjectResult;
    private static StorageCapacityRequest storageCapacityRequest;
    private static StorageCapacityResult storageCapacityResult;    
    

    @BeforeClass
    public static void init() {
        getObjectRequest = new GetObjectRequest();
        getObjectResult = new GetObjectResult();
        removeObjectRequest = new RemoveObjectRequest();
        removeObjectResult = new RemoveObjectResult();
        storageCapacityRequest = new StorageCapacityRequest();
        storageCapacityResult = new StorageCapacityResult();
    }

    @Test
    public void testGetObject() throws Exception {
        assertNotNull(getObjectRequest);
        assertNotNull(getObjectResult);        
    }
    
    @Test
    public void testRemoveObject() throws Exception {
        assertNotNull(removeObjectRequest);
        assertNotNull(removeObjectResult);        
    }
    
    @Test
    public void testStorageCapacity() throws Exception {
        storageCapacityRequest.setTenantId("ff");
        assertEquals("ff", storageCapacityRequest.getTenantId());
        storageCapacityResult.setUsableSpace(1000);
        storageCapacityResult.setUsedSpace(1000);
        assertEquals(1000, storageCapacityResult.getUsableSpace());
        assertEquals(1000, storageCapacityResult.getUsedSpace());
        assertNotNull(storageCapacityRequest);
        assertNotNull(storageCapacityResult);
    }

}