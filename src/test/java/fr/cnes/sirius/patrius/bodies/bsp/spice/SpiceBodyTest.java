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

import fr.cnes.sirius.patrius.bodies.bsp.spice.SpiceBody;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class SpiceBodyTest {

    /**
     * Test the translation of integer identifiers into body names.
     * If the identifier is not in the database, an empty string is 
     * returned
     * @throws PatriusException if there is a problem in the translation
     */
    @Test
    public void testBodyCode2Name() throws PatriusException {
        // List of ids and their body names attached;
        final int[] ids = {3, 4, 10, 399, 401, 402, 499};
        final String[] expectedBodies = {"EARTH BARYCENTER", "MARS BARYCENTER","SUN", "EARTH", "PHOBOS", "DEIMOS", "MARS"};
        
        // Test them
        for (int i = 0; i<ids.length; i++) {
            final String body = SpiceBody.bodyCode2Name(ids[i]);
            Assert.assertEquals(expectedBodies[i], body);
        }
        
        final int codeNoOk = 286;
        // Test a non existing code in the database
        Assert.assertTrue(SpiceBody.bodyCode2Name(codeNoOk).isEmpty());       
    }
    
    /**
     * Test the translation of integer identifiers into Strings.
     * If the identifier is not in the database, the identifier as a string is 
     * returned. If it is found, the body name is returned.
     * @throws PatriusException if there is a problem in the translation
     */
    @Test
    public void testBodyCode2String() throws PatriusException {
        final int codeOk = 199;
        final int codeNoOk = 187;
        
        // Test an existing code
        Assert.assertEquals("MERCURY", SpiceBody.bodyCode2String(codeOk));
        // Test a non-existing code
        Assert.assertEquals(String.valueOf(codeNoOk), SpiceBody.bodyCode2String(codeNoOk));
    
    }
    
    /**
     * Test the translation of body names to SPICE id code.
     * If the body is not in the databases, the identifier returned is -1
     * @throws PatriusException if there is a problem in the translation
     */
    @Test
    public void testBodyName2Code() throws PatriusException {
        // List of ids and their body names attached;
        final int[] expectedIds = {3, 4, 10, 399, 401, 402, 499};
        final boolean[] found = new boolean[1];
        final String[] bodies = {"EARTH BARYCENTER", "MARS BARYCENTER","SUN", "EARTH", "PHOBOS", "DEIMOS", "MARS"};
        
        // Test them
        for (int i = 0; i<bodies.length; i++) {
            final int id = SpiceBody.bodyName2Code(bodies[i], found);
            Assert.assertTrue(found[0]);
            Assert.assertEquals(expectedIds[i], id);
        }
        
        final String nameNoOk = "286";
        // Test a non existing code in the database
        int code = SpiceBody.bodyName2Code(nameNoOk, found);
        Assert.assertFalse(found[0]);
    }
}
