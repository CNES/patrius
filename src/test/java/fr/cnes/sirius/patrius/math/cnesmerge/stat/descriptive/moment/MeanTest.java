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
package fr.cnes.sirius.patrius.math.cnesmerge.stat.descriptive.moment;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean;

/**
 * <p>
 * This class adds tests in order to raise the cover rate of Mean to 100%
 * </p>
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: MeanTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class MeanTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle evaluate returns NaNwhen there are no values
         * 
         * @featureDescription The evaluate function returns NaN when there are no values, and the begin and length
         *                     parameter are ok.
         * 
         * @coveredRequirements DV-CALCUL_130
         */
        NANWHENWEIGHTED
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#NANWHENWEIGHTED}
     * 
     * @testedMethod {@link Mean#evaluate(double[], double[], int, int)}
     * 
     * @description This test first covers the two copy constructors, then calls the method evaluate with such
     *              parameters that the output should be NaN.
     * 
     * @input double[] values = {} : there are no value to mean
     * @input double[] weights = {} : there are no weights
     * @input double begin = 0 : the beginning index is 0
     * @input double[] length = 0 : the length is 0 because the values array is empty
     * 
     * @output double
     * 
     * @testPassCriteria The output is NaN.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void testEvaluateDoubleArrayDoubleArrayIntInt() {
        final Mean tempMean = new Mean();
        final Mean mean = new Mean(tempMean);
        final double[] values = new double[0];
        final double[] weights = new double[0];
        assertTrue(Double.isNaN(mean.evaluate(values, weights, 0, 0)));
    }

}
