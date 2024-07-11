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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:211:08/04/2014:Modified analytical 2D propagator
 * VERSION::DM:211:30/04/2014:Added missing methods
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Test class for {@link Analytical2DParameterModel}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: Analytical2DParameterModelTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class Analytical2DParameterModelTest {

    /** Machine epsilon */
    private final double eps = Precision.EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the parameter models
         * 
         * @featureDescription Validate the parameter model
         * 
         * @coveredRequirements DV-PROPAG_10, DV-PROPAG_20
         */
        ANALYTICAL_2D_PARAMETER_MODEL
    }

    private Analytical2DParameterModel model;
    private AbsoluteDate ref;

    @Before
    public void setup() {
        this.ref = AbsoluteDate.J2000_EPOCH;
        final double[] poly = new double[] { 1., .5, .3 };
        final double[][] trig = new double[][] { { 1, 1, .5, 1.2 }, { 3, 2, .4, .8 } };

        this.model = new Analytical2DParameterModel(new DatePolynomialFunction(this.ref, poly), trig);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_PARAMETER_MODEL}
     * 
     * @testedMethod {@link Analytical2DParameterModel#Analytical2DParameterModel(UnivariateDateFunction, double[][])}
     * 
     * @description test constructors
     * 
     * @input models and arrays
     * 
     * @output instance
     * 
     * @testPassCriteria exceptions thrown as expected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testAnalytical2DParameterModel() {
        // Centered part: polynomial coefficients size out of range
        try {
            new Analytical2DParameterModel(new DatePolynomialFunction(new AbsoluteDate(), new double[0]),
                new double[0][0]);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Trigonometric part: trigonometric coefficients size out of range
        try {
            new Analytical2DParameterModel(new DatePolynomialFunction(new AbsoluteDate(), new double[5]),
                new double[1][3]);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Trigonometric part: trigonometric coefficients not sorted by decreasing amplitude
        try {
            final double[][] trigcoefs = { { 1, 1, 2, 1 }, { 1, 1, 1.5, 1 }, { 1, 1, 1.7, 1 } };
            new Analytical2DParameterModel(new DatePolynomialFunction(new AbsoluteDate(), new double[5]), trigcoefs);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_PARAMETER_MODEL}
     * 
     * @testedMethod {@link Analytical2DParameterModel#getValue(AbsoluteDate)}
     * @testedMethod {@link Analytical2DParameterModel#getValue(AbsoluteDate, int)}
     * @testedMethod {@link Analytical2DParameterModel#getValue(AbsoluteDate, double[])}
     * @testedMethod {@link Analytical2DParameterModel#getValue(AbsoluteDate, double[], int)}
     * 
     * @description test getValue() method
     * 
     * @input date, order
     * 
     * @output value
     * 
     * @testPassCriteria equals expected value to machine epsilon, fails when order too high / low
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testGetValue() {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(100);
        final double dt = date.durationFrom(this.ref);

        // Case (AbsoluteDate, double, double)
        final double pso = 2.5 + .7 * 100;
        final double lna = 1.8 + .6 * 100;
        final double actual5 = this.model.getValue(date, pso, lna);
        final double expected5 = 1 + .5 * dt + .3 * dt * dt + .5 * MathLib.cos(1 * pso + 1 * lna + 1.2) + .4
            * MathLib.cos(3 * pso + 2 * lna + .8);
        Assert.assertEquals(expected5, actual5, this.eps);

        // Case (AbsoluteDate, double, double, int)
        final double pso4 = 2.5 + .7 * 100;
        final double lna4 = 1.8 + .6 * 100;
        final double actual6 = this.model.getValue(date, pso4, lna4, 2);
        final double expected6 = 1 + .5 * dt + .3 * dt * dt + .5 * MathLib.cos(1 * pso4 + 1 * lna4 + 1.2) + .4
            * MathLib.cos(3 * pso4 + 2 * lna4 + .8);
        Assert.assertEquals(expected6, actual6, this.eps);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_PARAMETER_MODEL}
     * 
     * @testedMethod {@link Analytical2DParameterModel#getCenteredValue(AbsoluteDate)}
     * 
     * @description test the method
     * 
     * @input date and order
     * 
     * @output centered value
     * 
     * @testPassCriteria equals expected value to machine epsilon, fails when order too high / low
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testGetCenteredValue() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(3600);
        final double dt = date.durationFrom(this.ref);
        final double actual = this.model.getCenteredValue(date);
        final double expected = 1 + .5 * dt + .3 * dt * dt;
        Assert.assertEquals(expected, actual, this.eps);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_PARAMETER_MODEL}
     * 
     * @testedMethod {@link Analytical2DParameterModel#getTrigonometricValue(AbsoluteDate, int)}
     * @testedMethod {@link Analytical2DParameterModel#getTrigonometricValue(AbsoluteDate)}
     * @testedMethod {@link Analytical2DParameterModel#getTrigonometricValue(double[])}
     * @testedMethod {@link Analytical2DParameterModel#getTrigonometricValue(double[], int)}
     * @testedMethod {@link Analytical2DParameterModel#getOneHarmonicValue(double, double, int)}
     * 
     * @description test the method
     * 
     * @input pso, lna and order
     * 
     * @output trigonometric value
     * 
     * @testPassCriteria equals expected value to machine epsilon, fails when order too high / low
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGetTrigonometricValue() {

        // Initialization
        final double pso200 = 2.5 + .7 * 200;
        final double lna200 = 1.8 + .6 * 200;

        // Case (double, double)
        final double actual4 = this.model.getTrigonometricValue(pso200, lna200);
        final double expected4 = .5 * MathLib.cos(1 * pso200 + 1 * lna200 + 1.2) + .4
            * MathLib.cos(3 * pso200 + 2 * lna200 + .8);
        Assert.assertEquals(expected4, actual4, this.eps);

        // Case (double, double, int)
        final double actual5 = this.model.getTrigonometricValue(pso200, lna200, 2);
        final double expected5 = .5 * MathLib.cos(1 * pso200 + 1 * lna200 + 1.2) + .4
            * MathLib.cos(3 * pso200 + 2 * lna200 + .8);
        Assert.assertEquals(expected5, actual5, this.eps);

        // getOneHarmonicValue
        final double pso = 2.5 + .7 * 100;
        final double lna = 1.8 + .6 * 100;
        final double actual6 = this.model.getOneHarmonicValue(pso, lna, 0);
        final double expected6 = .5 * MathLib.cos(1 * pso + 1 * lna + 1.2);
        Assert.assertEquals(expected6, actual6, this.eps);

        // Exception (order out of range)
        try {
            this.model.getOneHarmonicValue(pso, lna, -1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            this.model.getOneHarmonicValue(pso, lna, 4);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_PARAMETER_MODEL}
     * 
     * @testedMethod {@link Analytical2DParameterModel#getCenteredModel()}
     * @testedMethod {@link Analytical2DParameterModel#getPsoModel()}
     * @testedMethod {@link Analytical2DParameterModel#getLnaModel()}
     * @testedMethod {@link Analytical2DParameterModel#getTrigonometricCoefficients()}
     * @testedMethod {@link Analytical2DParameterModel#getMaxTrigonometricOrder()}
     * 
     * @description test getters
     * 
     * @input Analytical2DParameterModel
     * 
     * @output attributes
     * 
     * @testPassCriteria attributes are exactly as expected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testGetters() {
        // Check centered model
        Assert.assertEquals(3, ((DatePolynomialFunction) this.model.getCenteredModel()).getCoefPoly().length);
        Assert.assertEquals(1., ((DatePolynomialFunction) this.model.getCenteredModel()).getCoefPoly()[0]);
        Assert.assertEquals(.5, ((DatePolynomialFunction) this.model.getCenteredModel()).getCoefPoly()[1]);
        Assert.assertEquals(.3, ((DatePolynomialFunction) this.model.getCenteredModel()).getCoefPoly()[2]);

        // Check trigonometric coefficients
        Assert.assertEquals(2, this.model.getTrigonometricCoefficients().length);
        Assert.assertEquals(4, this.model.getTrigonometricCoefficients()[0].length);
        Assert.assertEquals(4, this.model.getTrigonometricCoefficients()[1].length);

        Assert.assertEquals(1, this.model.getTrigonometricCoefficients()[0][0], this.eps);
        Assert.assertEquals(1, this.model.getTrigonometricCoefficients()[0][1], this.eps);
        Assert.assertEquals(.5, this.model.getTrigonometricCoefficients()[0][2], this.eps);
        Assert.assertEquals(1.2, this.model.getTrigonometricCoefficients()[0][3], this.eps);
        Assert.assertEquals(3, this.model.getTrigonometricCoefficients()[1][0], this.eps);
        Assert.assertEquals(2, this.model.getTrigonometricCoefficients()[1][1], this.eps);
        Assert.assertEquals(.4, this.model.getTrigonometricCoefficients()[1][2], this.eps);
        Assert.assertEquals(.8, this.model.getTrigonometricCoefficients()[1][3], this.eps);

        // Check max trigonometric order
        Assert.assertEquals(2, this.model.getMaxTrigonometricOrder());
    }
}
