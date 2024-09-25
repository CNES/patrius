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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link UserCelestialBodyLoader} class.
 */
public class UserCelestialBodyLoaderTest {

    /**
     * @objective loads PHOBOS body/Mars barycenter object through UserCelestialBodyLoader
     *
     * @description loads PHOBOS body/Mars barycenter object through UserCelestialBodyLoader
     *
     * @passCriteria body built as expected
     */
    @Test
    public void testFunctional() throws PatriusException {
        // Initialization
        Utils.setDataRoot("bsp");
        
        final BSPEphemerisLoader ephemerisLoader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        // Phobos
        final CelestialBodyEphemeris phobosEphemeris = ephemerisLoader.loadCelestialBodyEphemeris("PHOBOS");
        final UserCelestialBodyLoader loaderPhobos = new UserCelestialBodyLoader(phobosEphemeris, 123, null, FramesFactory.getGCRF(), null);
        CelestialBodyFactory.addCelestialBodyLoader("PHOBOS", loaderPhobos);
        
        // Retrieve body
        final CelestialBody phobos = CelestialBodyFactory.getBody("PHOBOS");
        Assert.assertEquals(123, phobos.getGM(), 0.);
        Assert.assertEquals("Jupiter", loaderPhobos.getName("Jupiter"));
        
        // Mars barycenter
        final CelestialBodyEphemeris marsBarycenterEphemeris = ephemerisLoader.loadCelestialBodyEphemeris("MARS BARYCENTER");
        final UserCelestialBodyLoader loaderMarsBarycenter = new UserCelestialBodyLoader(marsBarycenterEphemeris, 1234, null, FramesFactory.getGCRF(), null);
        CelestialBodyFactory.addCelestialBodyLoader("MARS BARYCENTER", loaderMarsBarycenter);
        
        // Retrieve body
        final CelestialPoint marsBarycenter = CelestialBodyFactory.getPoint("MARS BARYCENTER");
        Assert.assertEquals(1234, marsBarycenter.getGM(), 0.);

        
        final CelestialPoint phobos2 = CelestialBodyFactory.getPoint("PHOBOS");
        Assert.assertEquals(123, phobos2.getGM(), 0.);
    }
}
