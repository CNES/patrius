/**
 * 
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
package fr.cnes.sirius.patrius.math.random;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @description <p>
 *              This file tests the class JDKRandomGenerator, that is to say the methods that are added by this class to
 *              the JDK Random class.
 *              </p>
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: JDKRandomGeneratorTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class JDKRandomGeneratorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle choosing the seed for the random generator
         * 
         * @featureDescription The purpose here is to be able to choose the seed for the random generator. It will be
         *                     used for testing, so that the tests can be done again and have the same results.
         * 
         * @coveredRequirements DV-CALCUL_160
         */
        SEED_CHOOSING
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SEED_CHOOSING}
     * 
     * @testedMethod {@link JDKRandomGenerator#setSeed(int)}
     * 
     * @description Ensure that the seed set with an int value corresponds with the same seed set with long type.
     * 
     * @input int marignan = 1515 : value for the int seed
     * @input long longMarignan = 1515 : value for the long seed
     * 
     * @output boolean
     * 
     * @testPassCriteria The test is ok if the numbers generated with both seeds are equal.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testSetSeedInt() {
        final JDKRandomGenerator gene = new JDKRandomGenerator();
        final int marignan = 1515;
        final long longMarignan = 1515L;
        gene.setSeed(marignan);
        final long actualValue = gene.nextLong();
        gene.setSeed(longMarignan);
        final long expectedValue = gene.nextLong();
        assertEquals(expectedValue, actualValue);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SEED_CHOOSING}
     * 
     * @testedMethod {@link JDKRandomGenerator#setSeed(int[])}
     * 
     * @description Ensure that the seed set with an int array corresponds with the same seed set with long type.
     * 
     * @input int[] : value for the int array seed and long : value for the long seed
     * 
     * @output boolean
     * 
     * @testPassCriteria The test is ok if the numbers generated with both seeds are equal.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testSetSeedIntArray() {
        // test for an empty array
        final JDKRandomGenerator gene = new JDKRandomGenerator();
        gene.setSeed(new int[] {});
        long actualValue = gene.nextLong();
        gene.setSeed(0L);
        long expectedValue = gene.nextLong();
        assertEquals(expectedValue, actualValue);

        // test for a single value array
        gene.setSeed(new int[] { 15 });
        actualValue = gene.nextLong();
        gene.setSeed(15L);
        expectedValue = gene.nextLong();
        assertEquals(expectedValue, actualValue);

        // test for a two values array
        gene.setSeed(new int[] { 1, 15 });
        actualValue = gene.nextLong();
        gene.setSeed(4294967306L);
        expectedValue = gene.nextLong();
        assertEquals(expectedValue, actualValue);
    }

}
