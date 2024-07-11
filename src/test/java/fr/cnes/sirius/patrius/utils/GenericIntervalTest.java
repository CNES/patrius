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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.interval.GenericInterval;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;

/**
 * Unit tests for <code>GenericInterval</code>.<br>
 * All tests will be made with the "Double" type.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.0
 * 
 */
public class GenericIntervalTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle GENERIC_INTERVAL
         * 
         * @featureDescription implementation of a generic interval
         * 
         * @coveredRequirements DV-DATES_150, DV-DATES_160, DV-DATES_170
         * 
         */
        GENERIC_INTERVAL
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_INTERVAL}
     * 
     * @testedMethod {@link GenericInterval#GenericInterval(IntervalEndpointType, Object, Object, IntervalEndpointType)}
     * @testedMethod {@link GenericInterval#getLowerData()}
     * @testedMethod {@link GenericInterval#getLowerEndpoint()}
     * @testedMethod {@link GenericInterval#getUpperData()}
     * @testedMethod {@link GenericInterval#getUpperEndpoint()}
     * 
     * @description unit test for constructor and getters
     * 
     * @input data for the constructor
     * 
     * @output getters' data
     * 
     * @testPassCriteria Getter values match those given to the constructor
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGenericInterval() {
        // Very simple constructor test
        // Note the order of values is off : the class cannot enforce an order
        final IntervalEndpointType lowType = IntervalEndpointType.OPEN;
        final Double lowValue = new Double(34.56);
        final IntervalEndpointType upType = IntervalEndpointType.CLOSED;
        final Double upValue = new Double(-2.3e34);
        final GenericInterval<Double> tgi = new GenericInterval<Double>(lowType, lowValue, upValue, upType);
        // Should never ever fail (unless JVM awfully buggy)
        assertNotNull(tgi);
        // Test lower type getter
        final IntervalEndpointType gLowType = tgi.getLowerEndpoint();
        assertEquals(lowType, gLowType);
        // Test lower data getter
        final Double gLowValue = tgi.getLowerData();
        assertEquals(lowValue.doubleValue(), gLowValue.doubleValue(), 0.);
        // Test upper data getter
        final Double gUpValue = tgi.getUpperData();
        assertEquals(upValue.doubleValue(), gUpValue.doubleValue(), 0.);
        // Test upper type getter
        final IntervalEndpointType gUpType = tgi.getUpperEndpoint();
        assertEquals(upType, gUpType);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_INTERVAL}
     * 
     * @testedMethod {@link GenericInterval#GenericInterval(IntervalEndpointType, Object, Object, IntervalEndpointType)}
     * 
     * @description unit test for invalid interval creation cases
     * 
     * @input wrong data for the constructors
     * 
     * @output {@link MathIllegalArgumentException}
     * 
     * @testPassCriteria exceptions raised as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testInvalidIntervals() {
        // Note the order of values is off : the class cannot enforce an order
        final IntervalEndpointType lowType = IntervalEndpointType.CLOSED;
        final Double lowValue = new Double(-0.23e11);
        final IntervalEndpointType upType = IntervalEndpointType.OPEN;
        final Double upValue = new Double(-6.5432e34);
        // First argument null
        boolean allRight = false;
        try {
            new GenericInterval<Double>(null, lowValue, upValue, upType);
        } catch (final MathIllegalArgumentException e) {
            allRight = true;
        }
        assertTrue(allRight);
        // Second argument null
        allRight = false;
        try {
            new GenericInterval<Double>(lowType, null, upValue, upType);
        } catch (final MathIllegalArgumentException e) {
            allRight = true;
        }
        assertTrue(allRight);
        // Third argument null;
        allRight = false;
        try {
            new GenericInterval<Double>(lowType, lowValue, null, upType);
        } catch (final MathIllegalArgumentException e) {
            allRight = true;
        }
        assertTrue(allRight);
        // Fourth argument null;
        allRight = false;
        try {
            new GenericInterval<Double>(lowType, lowValue, upValue, null);
        } catch (final MathIllegalArgumentException e) {
            allRight = true;
        }
        assertTrue(allRight);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_INTERVAL}
     * 
     * @testedMethod {@link GenericInterval#toString()}
     * 
     * @description unit test for toString
     * 
     * @input constructor data
     * 
     * @output strings from toString()
     * 
     * @testPassCriteria strings with format and values as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testToString() {
        // a simple interval
        final IntervalEndpointType closedType = IntervalEndpointType.CLOSED;
        final Double lowValue = new Double(1.23);
        final IntervalEndpointType openedType = IntervalEndpointType.OPEN;
        final Double upValue = new Double(4.56);
        final GenericInterval<Double> tgi = new GenericInterval<Double>(closedType, lowValue, upValue, openedType);
        final String expectedString = "[ 1.23 ; 4.56 [";
        // toString result test
        assertEquals(expectedString, tgi.toString());
        // Second test with reversed brackets, for branch coverage
        final GenericInterval<Double> tgi2 = new GenericInterval<Double>(openedType, lowValue, upValue, closedType);
        final String expectedString2 = "] 1.23 ; 4.56 ]";
        // toString result test
        assertEquals(expectedString2, tgi2.toString());
    }

}
