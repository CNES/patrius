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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
 * This class test {@link DateIntervalParabolicFunction} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: DateIntervalParabolicFunctionTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 */
public class DateIntervalParabolicFunctionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Parabolic piecewise functions
         * 
         * @featureDescription Validate parabolic piecewise functions
         * 
         * @coveredRequirements
         */
        PARABOLIC_PIECEWISE_FUNCTION
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARABOLIC_PIECEWISE_FUNCTION}
     * 
     * @testedMethod {@link DateIntervalParabolicFunction#value(AbsoluteDate)}
     * 
     * @description test value function
     * 
     * @input DateIntervalParabolicFunction
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

        // Normal case (check before, during and after intervals definition)
        final double x0 = 10;
        final double x0Dot = 2.;
        final AbsoluteDate[] timeIntervals = { date0, date0.shiftedBy(10.), date0.shiftedBy(20.) };
        final double[] xDotDotIntervals = { 2., 3. };
        final DateIntervalParabolicFunction f = new DateIntervalParabolicFunction(x0, x0Dot, timeIntervals,
            xDotDotIntervals);

        Assert.assertEquals(-10., f.value(date0.shiftedBy(-10)), 0.);
        Assert.assertEquals(45., f.value(date0.shiftedBy(5)), 0.);
        Assert.assertEquals(1020., f.value(date0.shiftedBy(30)), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARABOLIC_PIECEWISE_FUNCTION}
     * 
     * @testedMethod {@link DateIntervalParabolicFunction#getX0()}
     * @testedMethod {@link DateIntervalParabolicFunction#getxDot0()}
     * @testedMethod {@link DateIntervalParabolicFunction#getxDotDotIntervals()}
     * @testedMethod {@link DateIntervalParabolicFunction#getDateIntervals()}
     * 
     * @description test getters and copy
     * 
     * @input DateIntervalParabolicFunction
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
        final double x0 = 10;
        final double x0Dot = 5.;
        final AbsoluteDate[] timeIntervals = { date0, date0.shiftedBy(10.), date0.shiftedBy(20.) };
        final double[] xDotDotIntervals = { 2., 3. };
        final DateIntervalParabolicFunction f = new DateIntervalParabolicFunction(x0, x0Dot, timeIntervals,
            xDotDotIntervals);

        // Test getters
        Assert.assertEquals(10., f.getX0(), 0.);
        Assert.assertEquals(5., f.getxDot0(), 0.);
        Assert.assertEquals(2., f.getxDotDotIntervals()[0], 0.);
        Assert.assertEquals(3., f.getxDotDotIntervals()[1], 0.);
        Assert.assertEquals(0., f.getDateIntervals()[0].durationFrom(timeIntervals[0]), 0.);
        Assert.assertEquals(0., f.getDateIntervals()[1].durationFrom(timeIntervals[1]), 0.);
        Assert.assertEquals(0., f.getDateIntervals()[2].durationFrom(timeIntervals[2]), 0.);

        // Test copy
        final DateIntervalParabolicFunction f2 = new DateIntervalParabolicFunction(f);
        Assert.assertEquals(10., f2.getX0(), 0.);
        Assert.assertEquals(5., f2.getxDot0(), 0.);
        Assert.assertEquals(2., f2.getxDotDotIntervals()[0], 0.);
        Assert.assertEquals(3., f2.getxDotDotIntervals()[1], 0.);
        Assert.assertEquals(0., f2.getDateIntervals()[0].durationFrom(timeIntervals[0]), 0.);
        Assert.assertEquals(0., f2.getDateIntervals()[1].durationFrom(timeIntervals[1]), 0.);
        Assert.assertEquals(0., f2.getDateIntervals()[2].durationFrom(timeIntervals[2]), 0.);
        Assert.assertFalse(f.getxDotDotIntervals().hashCode() == f2.getxDotDotIntervals().hashCode());
        Assert.assertFalse(f.getDateIntervals().hashCode() == f2.getDateIntervals().hashCode());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PARABOLIC_PIECEWISE_FUNCTION}
     * 
     * @testedMethod {@link DateIntervalParabolicFunction#DateIntervalParabolicFunction(double, double, AbsoluteDate[], double[])}
     * 
     * @description test exception at initialization
     * 
     * @input DateIntervalParabolicFunction
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

        // Not ordered time intervals
        try {
            final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
            final double x0 = 10;
            final double x0Dot = 5;
            final AbsoluteDate[] timeIntervals = { date0.shiftedBy(10.), date0, date0.shiftedBy(20.) };
            final double[] xDotIntervals = { 2., 3. };
            new DateIntervalParabolicFunction(x0, x0Dot, timeIntervals, xDotIntervals);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Inconsistent number of time intervals and derivatives intervals
        try {
            final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
            final double x0 = 10;
            final double x0Dot = 5;
            final AbsoluteDate[] timeIntervals = { date0, date0.shiftedBy(10.), date0.shiftedBy(20.) };
            final double[] xDotIntervals = { 2., 3., 4. };
            new DateIntervalParabolicFunction(x0, x0Dot, timeIntervals, xDotIntervals);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Not enough dates
        try {
            final double x0 = 10;
            final double x0Dot = 5;
            final AbsoluteDate[] timeIntervals = {};
            final double[] xDotIntervals = {};
            new DateIntervalParabolicFunction(x0, x0Dot, timeIntervals, xDotIntervals);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }
}
