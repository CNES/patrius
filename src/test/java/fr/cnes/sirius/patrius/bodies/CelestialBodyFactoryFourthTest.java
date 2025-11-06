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
 * VERSION:4.14:OPENFD-256:22/08/2024:[PATRIUS] Ajout d'une methode pour
 * savoir si un corps celeste a un loader defini
 * END-HISTORY
 */  
package fr.cnes.sirius.patrius.bodies;

import static org.junit.Assert.assertFalse;
import fr.cnes.sirius.patrius.Utils;
import static org.junit.Assert.assertTrue;
import fr.cnes.sirius.patrius.Utils;

import org.junit.Before;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Test;
import fr.cnes.sirius.patrius.Utils;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.Utils;

public class CelestialBodyFactoryFourthTest {

    @Test
    public void testHasNoLoader() throws PatriusException {
        CelestialBodyFactory.clearCelestialBodyLoaders();

        // Case with an existing predefined body loader
        CelestialBodyFactory.addDefaultCelestialBodyLoader(CelestialBodyFactory.VENUS,
            JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES);
        assertFalse(CelestialBodyFactory.hasNoLoader(CelestialBodyFactory.VENUS));

        // Case with no existing predefined body loader
        assertTrue(CelestialBodyFactory.hasNoLoader("test"));
    }

    @Before
    public void setUp() {
        Utils.clear();
    }
}
