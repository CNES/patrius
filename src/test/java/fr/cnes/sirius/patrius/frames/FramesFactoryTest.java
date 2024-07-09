/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
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
 */
/*
 * 
 * HISTORY
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class FramesFactoryTest {

    @Test
    public void testTreeRoot() throws PatriusException {
        Assert.assertNull(FramesFactory.getFrame(Predefined.GCRF).getParent());
    }

    @Test
    public void testTreeICRF() throws PatriusException {
        // Since PATRIUS 4.2, ICRF parent parent frame is GCRF
        final Frame icrf = FramesFactory.getFrame(Predefined.ICRF);
        Assert.assertEquals(CelestialBodyFactory.EARTH_MOON + "/inertial", icrf.getParent().getName());
        Assert.assertEquals(Predefined.GCRF.getName(), icrf.getParent().getParent().getName());
    }

    @Test
    public void testTree() throws PatriusException {
        Assert.assertEquals(17, Predefined.values().length);
        final Predefined[][] reference = new Predefined[][] {
            { Predefined.EME2000, Predefined.GCRF },
            { Predefined.ITRF, Predefined.TIRF },
            { Predefined.ITRF_EQUINOX, Predefined.GTOD_WITH_EOP_CORRECTIONS },
            { Predefined.TIRF, Predefined.CIRF },
            { Predefined.CIRF, Predefined.GCRF },
            { Predefined.VEIS_1950, Predefined.GTOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.GTOD_WITHOUT_EOP_CORRECTIONS, Predefined.TOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.GTOD_WITH_EOP_CORRECTIONS, Predefined.TOD_WITH_EOP_CORRECTIONS },
            { Predefined.TOD_WITHOUT_EOP_CORRECTIONS, Predefined.MOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.TOD_WITH_EOP_CORRECTIONS, Predefined.MOD_WITH_EOP_CORRECTIONS },
            { Predefined.MOD_WITHOUT_EOP_CORRECTIONS, Predefined.EME2000 },
            { Predefined.MOD_WITH_EOP_CORRECTIONS, Predefined.GCRF },
            { Predefined.TEME, Predefined.TOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.EOD_WITH_EOP_CORRECTIONS, Predefined.MOD_WITH_EOP_CORRECTIONS },
            { Predefined.EOD_WITHOUT_EOP_CORRECTIONS, Predefined.MOD_WITHOUT_EOP_CORRECTIONS }
        };
        for (final Predefined[] pair : reference) {
            final Frame child = FramesFactory.getFrame(pair[0]);
            final Frame parent = FramesFactory.getFrame(pair[1]);
            Assert.assertEquals("wrong parent for " + child.getName(),
                parent.getName(), child.getParent().getName());
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
