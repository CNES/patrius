/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 
 * @history created 22/08/2024

 *
 * HISTORY
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14:OPENFD-258:22/08/2024:[PATRIUS] Ephemerides des barycentres planetaires
 * dans les fichiers JPL historiques
 * END-HISTORY
 */ 
package fr.cnes.sirius.patrius.bodies;

import static org.junit.Assert.assertEquals;
import fr.cnes.sirius.patrius.Utils;
import static org.junit.Assert.assertFalse;
import fr.cnes.sirius.patrius.Utils;
import static org.junit.Assert.assertTrue;
import fr.cnes.sirius.patrius.Utils;

import org.junit.Before;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Test;
import fr.cnes.sirius.patrius.Utils;

public class PredefinedEphemerisTypeTest {

    /**
     * Test method for new barycenter entries in PredefinedEphemerisType.
     */
    @Test
    public void testGettersBarycenterCases() {
        assertEquals(PredefinedEphemerisType.getEphemerisType("Mars barycenter"),
            PredefinedEphemerisType.MARS_BARY);
        assertEquals(PredefinedEphemerisType.getEphemerisType("Jupiter barycenter"),
            PredefinedEphemerisType.JUPITER_BARY);
        assertEquals(PredefinedEphemerisType.getEphemerisType("Saturn barycenter"),
            PredefinedEphemerisType.SATURN_BARY);
        assertEquals(PredefinedEphemerisType.getEphemerisType("Uranus barycenter"),
            PredefinedEphemerisType.URANUS_BARY);
        assertEquals(PredefinedEphemerisType.getEphemerisType("Neptune barycenter"),
            PredefinedEphemerisType.NEPTUNE_BARY);
        assertEquals(PredefinedEphemerisType.getEphemerisType("Pluto barycenter"),
            PredefinedEphemerisType.PLUTO_BARY);
    }

    /**
     * Test method for isBarycenter() method.
     */
    @Test
    public void testIsBarycenter() {
        // Tests for barycenters
        assertTrue(PredefinedEphemerisType.PLUTO_BARY.isBarycenter());
        // Test for non-barycenters
        assertFalse(PredefinedEphemerisType.PLUTO.isBarycenter());
    }

    @Before
    public void setUp() {
        Utils.clear();
    }
}
