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
 * @history creation 16/11/11
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.concurrency;

import java.net.URL;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * TestNG demonstrating the DataProvidersManager issues.
 * 
 * @author cardosop
 * 
 * @version $Id: DataProvidersManagerTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class DataProvidersManagerTest {

    /** Orekit data path. */
    private static final String KEY = "orekit.data.path";

    /** Invocation count for the multithreaded tests. */
    private static final int INVOC_COUNT = 500;
    /** Number of threads. */
    private static final int NB_THREADS = 10;
    /** Timeout in ms for tests that may hang on a live lock. */
    private static final int TIME_OUT = 10000;

    /** Wrongs counter. */
    private static volatile int wrongsCounter = 0;

    /**
     * Setup for the DataProvidersManager.
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
     * Single-thread test that never fails.
     * 
     * @throws PatriusException
     *         should not happen.
     */
    @Test(priority = 1)
    public void addDefaultProviderTest() throws PatriusException {
        final DataProvidersManager manager = DataProvidersManager.getInstance();
        System.out.println("Testing addDefaultProvider");
        System.out.println("orekit.data.path = " + System.getProperty(KEY));
        System.out.println("Size before addDefaultProviders() : " + manager.getProviders().size());
        manager.addDefaultProviders();
        System.out.println("Size after addDefaultProviders(): " + manager.getProviders().size());
        AssertJUnit.assertEquals(0, manager.getProviders().size() % 5);
    }

    /**
     * Test run multiple times through TestNG.
     * Note : asserts are disabled so that tests can run fully.
     * 
     * @throws PatriusException
     *         should not happen.
     */
    @Test(priority = 2, threadPoolSize = NB_THREADS, invocationCount = INVOC_COUNT, timeOut = TIME_OUT)
    public void addDefaultProviderConcurrentTest() throws PatriusException {
        final String thrName = Thread.currentThread().getName();
        final DataProvidersManager manager = DataProvidersManager.getInstance();
        final String thrOutput = "testing addDefaultProvider - " + thrName;
        manager.addDefaultProviders();
        if (0 == manager.getProviders().size() % 5) {
            System.out.println(thrOutput + " : OK");
        } else {
            System.out.println(thrOutput + " : WRONG!");
            wrongsCounter++;
            // Assert disabled for now.
            // AssertJUnit.fail();
        }
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
