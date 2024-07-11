/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history creation 03/04/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2967:15/11/2021:[PATRIUS] corriger les utilisations de java.util.Date 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.concurrency;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * TestNG for use of the CelestialBodyFactory in a multi-threaded environment.
 * Specifics :
 * <ul>
 * <li>No issues with the "getBody" related methods</li>
 * </ul>
 * 
 * @author cardosop
 * 
 * @version $Id: CelestialBodyFactoryTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class CelestialBodyFactoryTest {

    /** Orekit data path. */
    private static final String KEY = "orekit.data.path";

    /** Invocation count for the multithreaded tests. */
    private static final int INVOC_COUNT = 500;
    /** Number of threads. */
    private static final int NB_THREADS = 10;
    /** Timeout in ms for tests that may hang on a live lock. */
    private static final int TIME_OUT = 1000;

    /**
     * Setup for Orekit.
     */
    @BeforeClass
    public void beforeClass() {
        // Cleanup of the DataProvidersManager
        final DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.clearLoadedDataNames();
        manager.clearProviders();
        // Cleanup of the CelestialBodyFactory
        CelestialBodyFactory.clearCelestialBodyLoaders();

        final URL regDatCNES = this.getClass().getResource("/regular-dataCNES");
        final String regDatCNESpath = regDatCNES.getPath();
        final StringBuffer sbf = new StringBuffer();
        for (int i = 0; i < 5; i++) {
            sbf.append(regDatCNESpath + System.getProperty("path.separator"));
        }
        System.setProperty(KEY, sbf.toString());
        System.out.println("BEFORECLASS : orekit.data.path = " + System.getProperty(KEY));
    }

    /**
     * Creates a thread id custom string.
     */
    @BeforeMethod(alwaysRun = true)
    public void beforeMethod() {
        // set thread name
        Thread.currentThread().setName("Thread #" + UniqueThreadIdGenerator.getCurrentThreadId());
    }

    /**
     * Test run multiple times through TestNG.
     * Note : asserts are disabled so that tests can run fully.
     * This test fails only once for ten runs approximately, on a dead lock for the TimeScalesFactory.
     * 
     * @throws PatriusException
     *         should not happen.
     */
    @Test(priority = 1, threadPoolSize = NB_THREADS, invocationCount = INVOC_COUNT, timeOut = TIME_OUT)
    public void celestialBodyConcurrentTest() throws PatriusException {

        // Pick two random bodies
        final CelestialBody b1 = orderedCelestialBodyPicker();
        final CelestialBody b2 = orderedCelestialBodyPicker();

        // Get the first one's body frame
        final Frame b1Frame = b1.getRotatingFrameTrueModel();
        // Get the PV coordinates of the second body in the first's frame
        final PVCoordinates pvc = b2.getPVCoordinates(new AbsoluteDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC), TimeScalesFactory.getTT()), b1Frame);
        System.out.println("pvc : " + pvc.toString());
    }

    /** Order for the celestial body pick. */
    private static volatile int orderCB = 0;

    /**
     * Ordered celestial body picker. This method is purposedly not thread-protected.
     * 
     * @return a celestial body.
     */
    private static CelestialBody orderedCelestialBodyPicker() throws PatriusException {
        // Get the next integer
        final int nextInt = orderCB++;
        // orderCB reinit
        if (orderCB > 10) {
            orderCB = 0;
        }
        CelestialBody cBody;
        switch (nextInt) {
            case 0:
                cBody = CelestialBodyFactory.getEarth();
                break;
            case 1:
                cBody = CelestialBodyFactory.getJupiter();
                break;
            case 2:
                cBody = CelestialBodyFactory.getMars();
                break;
            case 3:
                cBody = CelestialBodyFactory.getVenus();
                break;
            case 4:
                cBody = CelestialBodyFactory.getSaturn();
                break;
            case 5:
                cBody = CelestialBodyFactory.getUranus();
                break;
            case 6:
                cBody = CelestialBodyFactory.getNeptune();
                break;
            case 7:
                cBody = CelestialBodyFactory.getPluto();
                break;
            case 8:
                cBody = CelestialBodyFactory.getSun();
                break;
            case 9:
                cBody = CelestialBodyFactory.getMercury();
                break;
            case 10:
            default:
                cBody = CelestialBodyFactory.getMoon();
        }
        return cBody;
    }
}
