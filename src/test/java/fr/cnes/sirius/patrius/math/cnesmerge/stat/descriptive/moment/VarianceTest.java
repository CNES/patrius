/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
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
 */
package fr.cnes.sirius.patrius.math.cnesmerge.stat.descriptive.moment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * <p>
 * -this class adds tests in order to raise the cover rate of Variance
 * </p>
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: VarianceTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class VarianceTest {

    /** Epsilon for double comparison. */
    private final double epsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Increase the code coverage
         * 
         * @featureDescription Increase the code coverage.
         * 
         * @coveredRequirements NA
         */
        COVERAGE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link Variance#evaluate(double[])}
     * 
     * @description Test an exception is thrown for a null values array.
     * 
     * @input double[] values : values array from which to get the median
     * 
     * @output NullArgumentException
     * 
     * @testPassCriteria The null array is detected and the exception thrown.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test(expected = NullArgumentException.class)
    public final void testEvaluateDoubleArray() {
        final double[] values = null;
        final Variance variance = new Variance();
        variance.evaluate(values);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link Variance#evaluate(double[], double[], int, int)}
     * 
     * @description Test the value returned for 1 value input.
     * 
     * @input double[] values : values array from which to get the median
     * 
     * @output double
     * 
     * @testPassCriteria The output should be 0 (with an epsilon of 1e-14 because it is a comparison between
     *                   two doubles).
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testEvaluateDoubleArrayDoubleArrayIntInt() {
        final double[] values = { 2.0 };
        final double[] weights = { 12.0 };
        final Variance variance = new Variance();
        final double actualResult = variance.evaluate(values, weights, 0, 1);
        assertEquals(0.0, actualResult, this.epsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link Variance#evaluate(double[], double, int, int)}
     * 
     * @description Test the value returned for 1 value input.
     * 
     * @input double[] values : values array from which to get the median
     * 
     * @output double
     * 
     * @testPassCriteria The output should be 0 (with an epsilon of 1e-14 because it is a comparison between
     *                   two doubles).
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testEvaluateDoubleArrayDoubleIntInt() {
        final double[] values = { 2.0 };
        final Variance variance = new Variance();
        final double actualResult = variance.evaluate(values, 2.0, 0, 1);
        assertEquals(0.0, actualResult, this.epsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link Variance#evaluate(double[], double[], double, int, int)}
     * 
     * @description Test the value returned for 1 value input.
     * 
     * @input double[] values : values array from which to get the median
     * 
     * @output double
     * 
     * @testPassCriteria The output should be 0 (with an epsilon of 1e-14 because it is a comparison between
     *                   two doubles).
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testEvaluateDoubleArrayDoubleArrayDoubleIntInt() {
        final double[] values = { 2.0 };
        final double[] weights = { 12.0 };
        final Variance variance = new Variance();
        final double actualResult = variance.evaluate(values, weights, 2.0, 0, 1);
        assertEquals(0.0, actualResult, this.epsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link Variance#isBiasCorrected()}
     * 
     * @description Test the setting method for the correction of the bias.
     * 
     * @input boolean value = false
     * 
     * @output none
     * 
     * @testPassCriteria The value before calling the setter should be true and after should be false.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testIsBiasCorrected() {
        final Variance variance = new Variance();
        assertTrue(variance.isBiasCorrected());
        variance.setBiasCorrected(false);
        assertFalse(variance.isBiasCorrected());
    }

}
