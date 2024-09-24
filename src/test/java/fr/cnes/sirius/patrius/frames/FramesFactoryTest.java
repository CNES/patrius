/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.11:FA:FA-3321:22/05/2023:[PATRIUS] Thread safety issue a la creation des FactoryManagedFrame
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
* VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
* VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class FramesFactoryTest {

    /**
     * FA 3321: construction of a unique GCRF frame.
     */
    @Test
    public void testThreadSafety() {
        final ConcurrentLinkedQueue<Frame> queueFrame = new ConcurrentLinkedQueue<>();
        IntStream.range(0, 10).parallel().forEach(i -> {
            queueFrame.add(FramesFactory.getGCRF());
        });
        Assert.assertEquals(1, new HashSet<>(queueFrame).size());
    }

    @Test
    public void testTreeRoot() throws PatriusException {
        // ICRF is root frame, it has no parent
        Assert.assertNull(FramesFactory.getFrame(Predefined.ICRF).getParent());
        // Check also JPL bodies frame tree
        Assert.assertNull(CelestialBodyFactory.getSolarSystemBarycenter().getICRF().getParent());
    }

    @Test
    public void testTreeICRF() throws PatriusException {
        // Since PATRIUS 4.9, GCRF parent parent frame is ICRF and EMB is GCRF parent
        final Frame gcrf = FramesFactory.getFrame(Predefined.GCRF);
        Assert.assertEquals(CelestialBodyFactory.EARTH_MOON + " ICRF frame", gcrf.getParent().getName());
        Assert.assertEquals(Predefined.ICRF.getName(), gcrf.getParent().getParent().getName());
        // Check also JPL bodies frame tree is consistent
        Assert.assertEquals(CelestialBodyFactory.getSolarSystemBarycenter().getICRF(), gcrf.getParent().getParent());
        Assert.assertEquals(CelestialBodyFactory.getEarthMoonBarycenter().getICRF(), gcrf.getParent());
        Assert.assertEquals(CelestialBodyFactory.getMars().getICRF().getParent(), FramesFactory.getICRF());
    }

    @Test
    public void testTree() throws PatriusException {
        Assert.assertEquals(18, Predefined.values().length);
        final Predefined[][] reference = new Predefined[][] {
            { Predefined.EME2000, Predefined.GCRF },
            { Predefined.ITRF, Predefined.TIRF },
            { Predefined.ITRF_EQUINOX, Predefined.GTOD_WITH_EOP_CORRECTIONS },
            { Predefined.TIRF, Predefined.CIRF },
            { Predefined.CIRF, Predefined.GCRF },
            { Predefined.GCRF, Predefined.EMB },
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
