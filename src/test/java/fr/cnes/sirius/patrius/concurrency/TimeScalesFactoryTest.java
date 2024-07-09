/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.8:FA:FA-2959:15/11/2021:[PATRIUS] Levee d'exception NullPointerException lors du calcul d'intersection a altitude
* VERSION:4.8:DM:DM-2967:15/11/2021:[PATRIUS] corriger les utilisations de java.util.Date 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history creation 02/04/12
 */
package fr.cnes.sirius.patrius.concurrency;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Random;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * TestNG demonstrating the TimeScalesFactory issues.
 * Must be run 10 times approx. to show the problem (dead lock on instances' initialiazation).
 * 
 * @author cardosop
 * 
 * @version $Id: TimeScalesFactoryTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class TimeScalesFactoryTest {

    /** Orekit data path. */
    private static final String KEY = "orekit.data.path";

    /** Invocation count for the multithreaded tests. */
    private static final int INVOC_COUNT = 100;
    /** Number of threads. */
    private static final int NB_THREADS = 10;
    /** Timeout in ms for tests that may hang on a live lock. */
    private static final int TIME_OUT = 1000;

    /** Wrongs counter. */
    private static volatile int wrongsCounter = 0;

    /**
     * Setup for Orekit.
     */
    @BeforeClass
    public void beforeClass() {
        // Cleanup of the DataProvidersManager
        final DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.clearLoadedDataNames();
        manager.clearProviders();

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
    public void timeScalesFactoryConcurrentTest() throws PatriusException {

        // Pick two random time scales
        final TimeScale ts1 = this.randomTimeScalePicker();
        final TimeScale ts2 = this.randomTimeScalePicker();

        // Create an absolute date in ts1
        final AbsoluteDate ad1 = new AbsoluteDate(2000, 9, 13, 0, 1, 2., ts1);
        // Create a java LocalDateTime from ad1 in the second time scale
        final LocalDateTime d2 = ad1.toLocalDateTime(ts2);
        System.out.println(d2);
    }

    /** Random number generator. */
    final static Random randGen = new Random();

    /**
     * Random time scale picker. This method is purposedly not thread-protected.
     * 
     * @return a random time scale.
     */
    private TimeScale randomTimeScalePicker() throws PatriusException {
        // Get a random integer
        final int randomInt = randGen.nextInt(10);
        TimeScale tScale;
        switch (randomInt) {
            case 0:
                tScale = TimeScalesFactory.getGMST();
                break;
            case 1:
                tScale = TimeScalesFactory.getGPS();
                break;
            case 2:
                tScale = TimeScalesFactory.getGST();
                break;
            case 3:
                tScale = TimeScalesFactory.getTAI();
                break;
            case 4:
                tScale = TimeScalesFactory.getTCB();
                break;
            case 5:
                tScale = TimeScalesFactory.getTCG();
                break;
            case 6:
                tScale = TimeScalesFactory.getTDB();
                break;
            case 7:
                tScale = TimeScalesFactory.getTT();
                break;
            case 8:
                tScale = TimeScalesFactory.getUT1();
                break;
            case 9:
            default:
                tScale = TimeScalesFactory.getUTC();
        }
        return tScale;
    }

    /**
     * Prints the number of failed tests.
     */
    @AfterClass
    public void afterClass() {
        System.out.println();
        System.out.println("Number of times a test went wrong : " + wrongsCounter);
        System.out.println();
    }
}
