/**
 * 
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
 *
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class test {@link DatePolynomialFunction} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: DatePolynomialFunctionTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 */
public class DatePolynomialFunctionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Polynomial functions
         * 
         * @featureDescription Validate polynomial functions
         * 
         * @coveredRequirements
         */
        POLYNOMIAL_FUNCTION
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#value(AbsoluteDate)}
     * 
     * @description test value function
     * 
     * @input DatePolynomialFunction
     * 
     * @output value
     * 
     * @testPassCriteria values are exactly as expected (mathematical cases)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testValue() {

        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;

        // Normal case (check before, and after reference date)
        final double[] coefs = { 2., 3., 4., 5. };
        final DatePolynomialFunction f = new DatePolynomialFunction(date0, coefs);

        Assert.assertEquals(-4628., f.value(date0.shiftedBy(-10)), 0.);
        Assert.assertEquals(5432., f.value(date0.shiftedBy(10)), 0.);

        // Test cache
        final double[] coefs2 = { 2., 3., 4., 5. };
        final DatePolynomialFunction f2 = new DatePolynomialFunction(date0, coefs2);
        Assert.assertEquals(5432., f2.value(date0.shiftedBy(10)), 0.);

        final double[] coefs3 = { 2., 3., 4., 5., 6. };
        final DatePolynomialFunction f3 = new DatePolynomialFunction(date0, coefs3);
        Assert.assertEquals(65432., f3.value(date0.shiftedBy(10)), 0.);
        Assert.assertEquals(5432., f2.value(date0.shiftedBy(10)), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#getX0()}
     * @testedMethod {@link DatePolynomialFunction#getCoefPoly()}
     * @testedMethod {@link DatePolynomialFunction#getPolyOrder()}
     * @testedMethod {@link DatePolynomialFunction#getT0()}
     * @testedMethod {@link DatePolynomialFunction#getDateIntervals()}
     * 
     * @description test getters and copy
     * 
     * @input DatePolynomialFunction
     * 
     * @output getters
     * 
     * @testPassCriteria values are exactly as expected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testGetters() {

        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final double[] coefs = { 2., 3., 4, 5 };
        final DatePolynomialFunction f = new DatePolynomialFunction(date0, coefs);

        // Test getters
        Assert.assertEquals(3., f.getPolyOrder(), 0.);
        Assert.assertEquals(0., f.getT0().durationFrom(date0), 0.);
        Assert.assertEquals(2., f.getCoefPoly()[0], 0.);
        Assert.assertEquals(3., f.getCoefPoly()[1], 0.);
        Assert.assertEquals(4., f.getCoefPoly()[2], 0.);
        Assert.assertEquals(5., f.getCoefPoly()[3], 0.);

        // Test copy
        final DatePolynomialFunction f2 = new DatePolynomialFunction(f);
        Assert.assertEquals(3., f2.getPolyOrder(), 0.);
        Assert.assertEquals(0., f2.getT0().durationFrom(date0), 0.);
        Assert.assertEquals(2., f2.getCoefPoly()[0], 0.);
        Assert.assertEquals(3., f2.getCoefPoly()[1], 0.);
        Assert.assertEquals(4., f2.getCoefPoly()[2], 0.);
        Assert.assertEquals(5., f2.getCoefPoly()[3], 0.);
        Assert.assertFalse(f.getCoefPoly().hashCode() == f2.getCoefPoly().hashCode());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#DatePolynomialFunction(AbsoluteDate, double[])}
     * 
     * @description test exception at initialization
     * 
     * @input DatePolynomialFunction
     * 
     * @output exception
     * 
     * @testPassCriteria exceptions are raised as expected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testExceptions() {

        // Not enough coefficients
        try {
            final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
            final double[] coefs = {};
            new DatePolynomialFunction(date0, coefs);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }
}
