/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.bsp.spice.SpiceFrame;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class SpiceFrameTest {
    
    /**
     * Test the translation of a frame ID into its name.
     * If the id is not found in the database, an empty string is 
     * returned
     * @throws PatriusException
     */
    @Test
    public void frameId2NameTest() throws PatriusException {
        // Test a valid id
        final int marsBarId = 10004;
        final String marsBarName = "IAU_MARS_BARYCENTER";
        
        Assert.assertEquals(marsBarName, 
                            SpiceFrame.frameId2Name(marsBarId));
        
        // Test a non valid id
        final int inconnu = 17005;
        Assert.assertTrue(SpiceFrame.frameId2Name(inconnu).isEmpty());

    }
    
    /**
     * Test the translation of frame names into IDs.
     * If the frame is not recognized, 0 is returned
     * @throws PatriusException
     */
    @Test
    public void frameName2IdTest() throws PatriusException {
        // Test a valid name
        final int marsBarId = 10004;
        final String marsBarName = "IAU_MARS_BARYCENTER";
        
        Assert.assertEquals(marsBarId, 
                            SpiceFrame.frameName2Id(marsBarName));
        
        // Test an invalid name
        Assert.assertEquals(0, SpiceFrame.frameName2Id("Mars"));
    }
    
    /**
     * Test retrieving info from a frame just from the Id.
     * Test {@link SpiceFrame#frameInfo(int, int[], int[], int[])}
     * Based on the example found in the FRINFO routine in the framex.for file of the SPICE library
     * @throws PatriusException If there is initialization problem.
     */
    @Test
    public void frameInfoTest() throws PatriusException {
        // Test a frame of the list
        final int frame = 13000;
        
        // Create output variables:
        final int[] center = new int[1];
        final int[] frClass = new int[1];
        final int[] classId = new int[1];
        
        // Do the search : 
        final boolean found = SpiceFrame.frameInfo(frame, center, frClass, classId);
        
        // The frame must be found in the list
        Assert.assertTrue(found);
        
        // Check the info retrieved.
        Assert.assertEquals(399, center[0]);
        Assert.assertEquals(2, frClass[0]);
        Assert.assertEquals(3000, classId[0]);
        
        // Test J2000
        final boolean fnd = SpiceFrame.frameInfo(1, center, frClass, classId);
        
        // The frame must be found in the list
        Assert.assertTrue(fnd);
        
        // Check the info retrieved.
        Assert.assertEquals(0, center[0]);
        Assert.assertEquals(1, frClass[0]);
        Assert.assertEquals(1, classId[0]);
    }
    
    /**
     * Test retrieving info from a frame from its class and class id.
     * Test {@link SpiceFrame#getFrameFromClass(int, int, int[], String[], int[])}
     * Test based on the example found in the CCIFRM routine in the framex.for file of the SPICE library
     * @throws PatriusException If there is initialization problem.
     */
    @Test
    public void getFrameFromClassTest() throws PatriusException {
        // Find the frame code associated with ITRF93
        final String frName = "ITRF93";
        final int realCode = 13000;
        final int realCenter = 399;
        
        final int frCod1 = SpiceFrame.frameName2Id(frName);
        
        Assert.assertEquals(realCode, frCod1);
        
        // Get the frame information.
        final int[] center = new int[1];
        final int[] cent = new int[1];
        final int[] frClass = new int[1];
        final int[] classId = new int[1];
        final int[] frCode = new int[1];
        final String[] name = new String[1];
        
        // Do the search : 
        final boolean found1 = SpiceFrame.frameInfo(frCod1, center, frClass, classId);
        
        // The frame must be found in the list
        Assert.assertTrue(found1);        
        // Check the info retrieved.
        Assert.assertEquals(realCenter, center[0]);
        Assert.assertEquals(2, frClass[0]);
        Assert.assertEquals(3000, classId[0]);
        
        // Return the frame name, frame ID, and center associated
        // with the frame CLSS and CLSSID.
        final boolean found2 = SpiceFrame.getFrameFromClass(frClass[0], classId[0], frCode, name, cent);
        
        // The frame must be found in the list
        Assert.assertTrue(found2);
        
        // Check the info retrieved.
        Assert.assertEquals(frName, name[0]);
        Assert.assertEquals(realCode, frCode[0]);
        Assert.assertEquals(realCenter, cent[0]);
    }

}
