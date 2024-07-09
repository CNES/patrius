/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
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
 */
package fr.cnes.sirius.patrius.tools.parallel;

import org.junit.Test;

/**
 * Unit tests for {@link ParallelException}.
 * 
 * @author cardosop
 * 
 * @version $Id: ParallelExceptionTest.java 17915 2017-09-11 12:35:44Z bignon $
 * 
 */
public class ParallelExceptionTest {
    /** Features description. */
    enum features {
        /**
         * @featureTitle ParallelException test
         * 
         * @featureDescription ParallelException test
         * 
         * @coveredRequirements NA
         */
        PARALLEL_EXCEPTION
    }

    /**
     * Test method for
     * {@link fr.cnes.sirius.patrius.tools.parallel.ParallelException#ParallelException(java.lang.String)}.
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLEL_EXCEPTION}
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test(expected = ParallelException.class)
    public final void testParallelExceptionString() {
        throw new ParallelException("str-message");
    }

    /**
     * Test method for
     * {@link fr.cnes.sirius.patrius.tools.parallel.ParallelException#ParallelException(java.lang.Throwable)} .
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLEL_EXCEPTION}
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test(expected = ParallelException.class)
    public final void testParallelExceptionThrowable() {
        throw new ParallelException(new RuntimeException());
    }

    /**
     * Test method for
     * {@link fr.cnes.sirius.patrius.tools.parallel.ParallelException#ParallelException(java.lang.String, java.lang.Throwable)}
     * .
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLEL_EXCEPTION}
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test(expected = ParallelException.class)
    public final void testParallelExceptionStringThrowable() {
        throw new ParallelException("str-message2", new RuntimeException());
    }

}
