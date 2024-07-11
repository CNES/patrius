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
 * @history Created 07/11/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations.coefficients;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test class
 * 
 * @see VariablePotentialCoefficientsSet
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: VariablePotentialCoefficientsSetTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class VariablePotentialCoefficientsSetTest {

    /** threshold */
    private final double eps = Precision.EPSILON;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle variable coefficients set
         * 
         * @featureDescription test the variable coefficients
         * 
         * @coveredRequirements DV-MOD_190, DV-MOD_220, DV-MOD_230
         */
        VARIABLE_COEFFICIENTS
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS}
     * 
     * @testedMethod {@link VariablePotentialCoefficientsSet#VariablePotentialCoefficientsSet(int, int, double, double, double[], double[])}
     * 
     * @description tests the constructor with faulty params
     * 
     * @input set parameters
     * 
     * @output exceptions
     * 
     * @testPassCriteria expected exceptions are thrown
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testVariablePotentialCoefficientsSet() {
        new VariablePotentialCoefficientsSet(2, 2, 1, 2, new double[] { 1, 2, 3, 4, 5 }, new double[] { 8, 9, 10, 11,
            12 });
        try {
            new VariablePotentialCoefficientsSet(2, -1, 1, 2, new double[] { 1, 2, 3, 4, 5, 6 }, new double[] { 7, 8,
                9, 10, 11, 12 });
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }
        try {
            new VariablePotentialCoefficientsSet(2, 2, 1, 2, new double[] { 1, 2, 3, 4, 5, 6 }, new double[] { 7, 8, 9,
                10, 11 });
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected!
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_COEFFICIENTS}
     * 
     * @testedMethod {@link VariablePotentialCoefficientsSet#getC()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getS()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getCc()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getSc()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getDegree()}
     * @testedMethod {@link VariablePotentialCoefficientsSet#getOrder()}
     * 
     * @description tests the getters of the coefficients set
     * 
     * @input a VariablePotentialCoefficientsSet
     * 
     * @output the different constants
     * 
     * @testPassCriteria actual values are the same as the expected ones. threshold is 1e-16
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGetters() {
        final VariablePotentialCoefficientsSet set = new VariablePotentialCoefficientsSet(2, 3, 1, 2, new double[] { 1,
            2, 3, 4, 5 }, new double[] { 8, 9, 10, 11, 12 });

        Assert.assertEquals(2, set.getDegree());
        Assert.assertEquals(3, set.getOrder());
        Assert.assertEquals(0, (1 - set.getC()), this.eps);
        Assert.assertEquals(0, (2 - set.getS()), this.eps);
        this.assertEquals(new double[] { 1, 2, 3, 4, 5 }, set.getCc(), this.eps);
        this.assertEquals(new double[] { 8, 9, 10, 11, 12 }, set.getSc(), this.eps);
    }

    /**
     * Assert arrays equal
     * 
     * @param exp
     *        expected array
     * @param act
     *        actual
     * @param thr
     *        threshold
     */
    private void assertEquals(final double[] exp, final double[] act, final double thr) {

        Assert.assertEquals(exp.length, act.length);

        for (int k = 0; k < exp.length; k++) {
            Assert.assertEquals(0, (exp[k] - act[k]) / act[k], thr);
        }

    }

}
